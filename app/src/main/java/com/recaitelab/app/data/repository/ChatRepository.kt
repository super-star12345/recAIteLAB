package com.recaitelab.app.data.repository

import com.recaitelab.app.data.remote.ChatProvider
import com.recaitelab.app.data.remote.createChatProvider
import com.recaitelab.app.domain.model.ChatMessage
import com.recaitelab.app.domain.model.ContextWindow
import com.recaitelab.app.domain.model.ProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/**
 * 会话请求仓库：从 Room 读取上下文，使用 [ProviderRepository] 中激活的 Provider / 模型发起请求。
 *
 * 持久化约定（由 [ConversationRepository] 完成）：
 * - 发送前把 user 消息写入数据库；
 * - 流式回复只在内存累积，正常结束后才写入完整 assistant 消息；
 * - 停止或异常时不写 assistant，只保留已持久化的 user 消息；
 * - 重新生成前删除末尾 assistant 消息，再按剩余上下文请求。
 *
 * 切换 Provider / 模型不会影响当前会话上下文：每次 send/regenerate 才重新解析激活配置，
 * 因此新配置只影响之后发出的消息，进行中的流仍使用发起时捕获的快照。
 *
 * @param providerRepository 提供当前激活的 Provider 与模型
 * @param conversationRepository 会话与消息持久化
 * @param temperature 采样温度；null 表示不向服务端发送，使用厂商自己的默认值
 * @param providerFactory 按配置创建 ChatProvider；默认 [createChatProvider]，测试时可替换
 */
class ChatRepository(
    private val providerRepository: ProviderRepository,
    private val conversationRepository: ConversationRepository,
    private val temperature: Float? = null,
    private val providerFactory: (ProviderConfig) -> ChatProvider = ::createChatProvider,
) {

    /** 当前会话 id；切换/新建/删除会话时由 [ConversationRepository] 更新。 */
    val activeConversationId: StateFlow<Long?> = conversationRepository.activeConversationId

    /** 读取上次会话的历史消息；没有会话时返回空列表。 */
    suspend fun loadConversation(): List<ChatMessage> =
        conversationRepository.loadCurrentConversation()?.messages ?: emptyList()

    /** 打开历史记录中的指定会话；会话已不存在时返回 null。 */
    suspend fun openConversation(conversationId: Long): List<ChatMessage>? =
        conversationRepository.openConversation(conversationId)?.messages

    /** 开始新上下文：保留历史会话，下一次发送时创建新会话。 */
    suspend fun startNewConversation() {
        conversationRepository.startNewConversation()
    }

    /**
     * 发送一条用户消息，返回逐段回复的冷流。
     *
     * - 发送时解析当前激活的 Provider 与模型；
     * - 首次发送时由 [ConversationRepository] 创建会话并写入 user 消息；
     * - 回复片段原样下发，同时在内存中累积；
     * - 只有正常读到流的结尾（`[DONE]` / `finish_reason`）才把完整回复写入 Room；
     * - 返回的 Flow 是冷的，只应收集一次；生成期间 UI 应禁用再次发送。
     */
    suspend fun send(prompt: String): Flow<String> {
        require(prompt.isNotBlank()) { "prompt 不能为空" }

        val target = resolveTarget()
        val snapshot = conversationRepository.appendUserMessage(
            content = prompt,
            providerId = target.config.id.toString(),
            model = target.model,
        )
        return streamReply(snapshot, target)
    }

    /**
     * 重新生成最后一条回复。
     *
     * 删除数据库中末尾的 assistant 消息（若有），以剩余上下文再次请求；
     * 末尾只有 user 消息时（例如上次被停止或失败），直接基于当前上下文重试。
     */
    suspend fun regenerate(): Flow<String> {
        val target = resolveTarget()
        val snapshot = conversationRepository.prepareRegeneration(
            providerId = target.config.id.toString(),
            model = target.model,
        ) ?: throw IllegalStateException("没有可重新生成的上下文")
        return streamReply(snapshot, target)
    }

    private fun streamReply(
        snapshot: ConversationRepository.ConversationSnapshot,
        target: ChatTarget,
    ): Flow<String> = flow {
        val reply = StringBuilder()
        providerFactory(target.config).streamChat(
            // 只裁剪本次请求的上下文；Room 中的完整历史保持不变。
            messages = ContextWindow.truncate(snapshot.messages),
            model = target.model,
            temperature = temperature,
        ).collect { delta ->
            reply.append(delta)
            emit(delta)
        }
        if (reply.isNotEmpty()) {
            conversationRepository.appendAssistantMessage(
                conversationId = snapshot.conversationId,
                content = reply.toString(),
                providerId = target.config.id.toString(),
                model = target.model,
            )
        }
    }

    private fun resolveTarget(): ChatTarget {
        val config = providerRepository.activeProvider.value
            ?: throw ChatConfigurationException.NoActiveProvider()
        val model = config.selectedModel
            ?: throw ChatConfigurationException.NoSelectedModel(config.name)
        return ChatTarget(config = config, model = model)
    }

    /** 发起一次请求时锁定的 Provider 与模型快照。 */
    private data class ChatTarget(
        val config: ProviderConfig,
        val model: String,
    )

    /** 清空当前会话及其消息；下次发送会重新创建会话。 */
    suspend fun clearConversation() {
        conversationRepository.clearCurrentConversation()
    }
}
