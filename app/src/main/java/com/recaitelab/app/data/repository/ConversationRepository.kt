package com.recaitelab.app.data.repository

import com.recaitelab.app.data.local.dao.ConversationDao
import com.recaitelab.app.data.local.dao.MessageDao
import com.recaitelab.app.data.local.entity.ConversationEntity
import com.recaitelab.app.data.local.entity.MessageEntity
import com.recaitelab.app.data.local.model.ConversationSummary
import com.recaitelab.app.data.local.preferences.ConversationPreferences
import com.recaitelab.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 会话与消息持久化仓库。
 *
 * 职责：
 * - 首次发送消息时创建 [ConversationEntity]，标题取首条消息前 20 个字；
 * - user 消息发送前落库，assistant 消息在流式完成后整体落库（生成过程只在内存累积）；
 * - 用 DataStore 的 `last_conversation_id` 记住当前会话，杀进程后可恢复；
 * - 支持历史记录需要的列表摘要、打开指定会话、重命名、删除、开始新上下文；
 * - 当前会话 id 通过 [activeConversationId] 暴露，供 ChatViewModel 跟踪删除等状态。
 *
 * 所有写操作由 [mutex] 串行化，避免“加载历史”和“发送消息”同时改动 activeId。
 */
class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val preferences: ConversationPreferences,
) {

    private val mutex = Mutex()

    /** 内存中的当前会话 id；null 表示新上下文或尚未解析。 */
    private var activeId: Long? = null

    private val _activeConversationId = MutableStateFlow<Long?>(null)

    /** 当前会话 id 的只读观察流；切换/新建/删除会话时更新。 */
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    /** 一次请求所需的会话 id 与完整上下文（按时间正序）。 */
    data class ConversationSnapshot(
        val conversationId: Long,
        val messages: List<ChatMessage>,
    )

    /** 历史记录：按 updatedAt 倒序的会话摘要，带最后一条消息正文。 */
    fun observeSummaries(): Flow<List<ConversationSummary>> = conversationDao.observeSummaries()

    /**
     * 读取上次打开的会话及其消息。
     *
     * 优先使用 DataStore 的 `last_conversation_id`；id 失效时回退到最近更新的会话；
     * 没有任何会话时返回 null，并清空无效偏好。
     */
    suspend fun loadCurrentConversation(): ConversationSnapshot? = mutex.withLock {
        val storedId = preferences.lastConversationId.first()
        val conversation = storedId?.let { conversationDao.findById(it) }
            ?: conversationDao.getLatest()

        if (conversation == null) {
            setActiveId(null)
            if (storedId != null) preferences.setLastConversationId(null)
            return@withLock null
        }

        setActiveId(conversation.id)
        if (conversation.id != storedId) {
            preferences.setLastConversationId(conversation.id)
        }
        conversation.toSnapshot()
    }

    /** 打开历史记录中的指定会话，并把 `last_conversation_id` 指向它。 */
    suspend fun openConversation(conversationId: Long): ConversationSnapshot? = mutex.withLock {
        val conversation = conversationDao.findById(conversationId) ?: return@withLock null
        setActiveId(conversation.id)
        preferences.setLastConversationId(conversation.id)
        conversation.toSnapshot()
    }

    /**
     * 只读获取指定会话的全部消息，不改变当前激活会话。
     * 供知识整理等后台任务使用。
     */
    suspend fun messagesFor(conversationId: Long): List<ChatMessage> = mutex.withLock {
        messageDao.getByConversation(conversationId).map { it.toChatMessage() }
    }

    /**
     * 开始新上下文：保留全部历史会话，只清空当前指针。
     * 下一次发送消息时才真正创建新的 `ConversationEntity`。
     */
    suspend fun startNewConversation() {
        mutex.withLock {
            setActiveId(null)
            preferences.setLastConversationId(null)
        }
    }

    /** 重命名会话；同时刷新 updatedAt，使其回到列表顶部。 */
    suspend fun renameConversation(conversationId: Long, title: String) {
        val normalized = title.trim()
        require(normalized.isNotEmpty()) { "标题不能为空" }
        mutex.withLock {
            if (conversationDao.findById(conversationId) == null) return@withLock
            conversationDao.updateTitle(
                id = conversationId,
                title = normalized,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    /** 删除会话及其消息；若删除的是当前会话，清空当前指针。 */
    suspend fun deleteConversation(conversationId: Long) {
        mutex.withLock {
            conversationDao.deleteById(conversationId)
            if (activeId == conversationId) {
                setActiveId(null)
                preferences.setLastConversationId(null)
            }
        }
    }

    /**
     * 发送前写入 user 消息。
     *
     * 没有当前会话时创建会话（标题取 [content] 前 20 个字），然后把 user 消息落库，
     * 返回写入后的完整上下文供本次请求使用。
     */
    suspend fun appendUserMessage(
        content: String,
        providerId: String?,
        model: String?,
    ): ConversationSnapshot = mutex.withLock {
        val now = System.currentTimeMillis()
        val conversation = resolveConversationForWrite(
            titleSeed = content,
            providerId = providerId,
            model = model,
            now = now,
        )
        messageDao.insert(
            MessageEntity(
                conversationId = conversation.id,
                role = ROLE_USER,
                content = content,
                timestamp = now,
            ),
        )
        conversationDao.updateTarget(
            id = conversation.id,
            providerId = providerId,
            model = model,
            updatedAt = now,
        )
        conversation.toSnapshot()
    }

    /**
     * 重新生成前准备上下文：删除末尾所有 assistant 消息（对应 UI 的替换最后一条回复），
     * 末尾是 user 消息时（上次停止/失败）直接复用上下文。
     *
     * @return null 表示没有会话或没有可重新生成的上下文
     */
    suspend fun prepareRegeneration(
        providerId: String?,
        model: String?,
    ): ConversationSnapshot? = mutex.withLock {
        val conversation = resolveActiveConversation() ?: return@withLock null
        var messages = messageDao.getByConversation(conversation.id)
        while (messages.lastOrNull()?.role == ROLE_ASSISTANT) {
            messageDao.deleteById(messages.last().id)
            messages = messages.dropLast(1)
        }
        if (messages.isEmpty()) return@withLock null

        conversationDao.updateTarget(
            id = conversation.id,
            providerId = providerId,
            model = model,
            updatedAt = System.currentTimeMillis(),
        )
        ConversationSnapshot(
            conversationId = conversation.id,
            messages = messages.map { it.toChatMessage() },
        )
    }

    /**
     * 流式回复完成后写入完整 assistant 消息。
     * 会话在生成期间被删除时直接忽略，避免外键失败。
     */
    suspend fun appendAssistantMessage(
        conversationId: Long,
        content: String,
        providerId: String?,
        model: String?,
    ) {
        mutex.withLock {
            if (conversationDao.findById(conversationId) == null) return@withLock
            val now = System.currentTimeMillis()
            messageDao.insert(
                MessageEntity(
                    conversationId = conversationId,
                    role = ROLE_ASSISTANT,
                    content = content,
                    timestamp = now,
                ),
            )
            conversationDao.updateTarget(
                id = conversationId,
                providerId = providerId,
                model = model,
                updatedAt = now,
            )
        }
    }

    /** 清空当前会话：删除会话及其消息，下次发送时重新创建。 */
    suspend fun clearCurrentConversation() {
        mutex.withLock {
            val conversationId = resolveActiveConversation()?.id
            if (conversationId != null) {
                conversationDao.deleteById(conversationId)
            }
            setActiveId(null)
            preferences.setLastConversationId(null)
        }
    }

    /**
     * 写消息时解析会话：
     * 内存中的 active id → DataStore 的 last id → 新建。
     *
     * 关键：**绝不**回退到“最近更新的会话”。`startNewConversation()` / 删除当前会话后
     * active 与偏好都为空，此时必须新建会话，否则新消息会写回旧会话，造成上下文串联。
     */
    private suspend fun resolveConversationForWrite(
        titleSeed: String,
        providerId: String?,
        model: String?,
        now: Long,
    ): ConversationEntity {
        val inMemory = activeId?.let { conversationDao.findById(it) }
        if (inMemory != null) return inMemory

        val storedId = preferences.lastConversationId.first()
        val stored = storedId?.let { conversationDao.findById(it) }
        if (stored != null) {
            setActiveId(stored.id)
            return stored
        }

        val conversation = ConversationEntity(
            title = titleSeed.toConversationTitle(),
            providerId = providerId,
            model = model,
            createdAt = now,
            updatedAt = now,
        )
        val id = conversationDao.insert(conversation)
        setActiveId(id)
        preferences.setLastConversationId(id)
        return conversation.copy(id = id)
    }

    private suspend fun resolveActiveConversation(): ConversationEntity? {
        val inMemory = activeId?.let { conversationDao.findById(it) }
        if (inMemory != null) return inMemory

        val storedId = preferences.lastConversationId.first()
        val stored = storedId?.let { conversationDao.findById(it) }
        if (stored == null) {
            setActiveId(null)
            if (storedId != null) preferences.setLastConversationId(null)
            return null
        }
        setActiveId(stored.id)
        return stored
    }

    private fun setActiveId(conversationId: Long?) {
        activeId = conversationId
        _activeConversationId.value = conversationId
    }

    private suspend fun ConversationEntity.toSnapshot(): ConversationSnapshot = ConversationSnapshot(
        conversationId = id,
        messages = messageDao.getByConversation(id).map { it.toChatMessage() },
    )

    /** 标题取首条消息前 20 个字（按 Unicode 码点截断，避免切开 emoji）。 */
    private fun String.toConversationTitle(): String {
        val normalized = trim().replace(WHITESPACE_REGEX, " ")
        if (normalized.isBlank()) return FALLBACK_TITLE
        val endIndex = if (normalized.codePointCount(0, normalized.length) <= MAX_TITLE_LENGTH) {
            normalized.length
        } else {
            normalized.offsetByCodePoints(0, MAX_TITLE_LENGTH)
        }
        return normalized.substring(0, endIndex)
    }

    private fun MessageEntity.toChatMessage(): ChatMessage =
        ChatMessage(role = role, content = content)

    private companion object {
        const val ROLE_USER: String = "user"
        const val ROLE_ASSISTANT: String = "assistant"
        const val MAX_TITLE_LENGTH: Int = 20
        const val FALLBACK_TITLE: String = "新对话"
        val WHITESPACE_REGEX: Regex = Regex("\\s+")
    }
}
