package com.recaitelab.app.data.repository

import com.recaitelab.app.data.local.dao.ConversationDao
import com.recaitelab.app.data.local.dao.MessageDao
import com.recaitelab.app.data.local.entity.ConversationEntity
import com.recaitelab.app.data.local.entity.MessageEntity
import com.recaitelab.app.data.local.model.ConversationSummary
import com.recaitelab.app.data.local.preferences.ConversationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/**
 * 会话隔离回归测试。
 *
 * 复现路径：A 会话完成后 startNewConversation() 再发送 B；
 * 旧实现会回退到 getLatest()，把 B 的消息写入 A，并把 A 的历史带入 B 的请求上下文。
 */
class ConversationRepositoryTest {

    private lateinit var conversationDao: FakeConversationDao
    private lateinit var messageDao: FakeMessageDao
    private lateinit var repository: ConversationRepository

    @Before
    fun setUp() {
        conversationDao = FakeConversationDao()
        messageDao = FakeMessageDao()
        repository = ConversationRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            preferences = FakeConversationPreferences(),
        )
    }

    @Test
    fun newConversationStartsWithEmptyContext() = runBlocking {
        val conversationA = repository.appendUserMessage(
            content = "A question",
            providerId = "provider-a",
            model = "model-a",
        )
        repository.appendAssistantMessage(
            conversationId = conversationA.conversationId,
            content = "A answer",
            providerId = "provider-a",
            model = "model-a",
        )

        repository.startNewConversation()
        val conversationB = repository.appendUserMessage(
            content = "B question",
            providerId = "provider-b",
            model = "model-b",
        )

        assertNotEquals(conversationA.conversationId, conversationB.conversationId)
        assertEquals(listOf("B question"), conversationB.messages.map { it.content })

        val reloadedA = repository.openConversation(conversationA.conversationId)
        assertEquals(
            listOf("A question", "A answer"),
            reloadedA?.messages?.map { it.content },
        )
    }

    @Test
    fun alternatingConversationsNeverContainEachOtherMessages() = runBlocking {
        val conversationA = repository.appendUserMessage("A1", null, null)
        repository.appendAssistantMessage(conversationA.conversationId, "A1 answer", null, null)

        repository.startNewConversation()
        val conversationB = repository.appendUserMessage("B1", null, null)
        repository.appendAssistantMessage(conversationB.conversationId, "B1 answer", null, null)

        // A 的上下文只能包含 A 的消息
        val contextA = repository.openConversation(conversationA.conversationId)
        assertFalse(contextA?.messages.orEmpty().any { it.content.contains("B1") })
        val nextA = repository.appendUserMessage("A2", null, null)
        assertEquals(
            listOf("A1", "A1 answer", "A2"),
            nextA.messages.map { it.content },
        )
        assertFalse(nextA.messages.any { it.content.contains("B1") })

        // B 的上下文只能包含 B 的消息
        val contextB = repository.openConversation(conversationB.conversationId)
        assertFalse(contextB?.messages.orEmpty().any { it.content.contains("A1") })
        val nextB = repository.appendUserMessage("B2", null, null)
        assertEquals(
            listOf("B1", "B1 answer", "B2"),
            nextB.messages.map { it.content },
        )
        assertFalse(nextB.messages.any { it.content.contains("A1") })
    }

    @Test
    fun deletingCurrentConversationStartsAnotherNewContext() = runBlocking {
        val conversationA = repository.appendUserMessage("A1", null, null)
        repository.startNewConversation()
        val conversationB = repository.appendUserMessage("B1", null, null)

        repository.deleteConversation(conversationB.conversationId)
        val conversationC = repository.appendUserMessage("C1", null, null)

        assertNotEquals(conversationA.conversationId, conversationC.conversationId)
        assertNotEquals(conversationB.conversationId, conversationC.conversationId)
        assertEquals(listOf("C1"), conversationC.messages.map { it.content })
    }
}

private class FakeConversationPreferences : ConversationPreferences {
    private val lastId = MutableStateFlow<Long?>(null)
    override val lastConversationId: Flow<Long?> = lastId
    override suspend fun setLastConversationId(conversationId: Long?) {
        lastId.value = conversationId
    }
}

private class FakeConversationDao : ConversationDao {

    private val conversations = linkedMapOf<Long, ConversationEntity>()
    private var nextId = 1L

    override fun observeAll(): Flow<List<ConversationEntity>> =
        MutableStateFlow(conversations.values.toList())

    override fun observeSummaries(): Flow<List<ConversationSummary>> =
        MutableStateFlow(emptyList())

    override fun observeById(id: Long): Flow<ConversationEntity?> =
        MutableStateFlow(conversations[id])

    override suspend fun findById(id: Long): ConversationEntity? = conversations[id]

    override suspend fun getLatest(): ConversationEntity? =
        conversations.values.maxByOrNull { it.updatedAt }

    override suspend fun insert(entity: ConversationEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        conversations[id] = entity.copy(id = id)
        return id
    }

    override suspend fun update(entity: ConversationEntity) {
        conversations[entity.id] = entity
    }

    override suspend fun updateTitle(id: Long, title: String, updatedAt: Long) {
        conversations[id]?.let { conversations[id] = it.copy(title = title, updatedAt = updatedAt) }
    }

    override suspend fun touch(id: Long, updatedAt: Long) {
        conversations[id]?.let { conversations[id] = it.copy(updatedAt = updatedAt) }
    }

    override suspend fun updateTarget(
        id: Long,
        providerId: String?,
        model: String?,
        updatedAt: Long,
    ) {
        conversations[id]?.let {
            conversations[id] = it.copy(
                providerId = providerId,
                model = model,
                updatedAt = updatedAt,
            )
        }
    }

    override suspend fun deleteById(id: Long) {
        conversations.remove(id)
    }
}

private class FakeMessageDao : MessageDao {

    private val messages = linkedMapOf<Long, MessageEntity>()
    private var nextId = 1L

    override fun observeByConversation(conversationId: Long): Flow<List<MessageEntity>> =
        MutableStateFlow(messagesOf(conversationId))

    override suspend fun getByConversation(conversationId: Long): List<MessageEntity> =
        messagesOf(conversationId)

    override suspend fun insert(entity: MessageEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        messages[id] = entity.copy(id = id)
        return id
    }

    override suspend fun updateContent(id: Long, content: String) {
        messages[id]?.let { messages[id] = it.copy(content = content) }
    }

    override suspend fun deleteById(id: Long) {
        messages.remove(id)
    }

    override suspend fun deleteByConversation(conversationId: Long) {
        messages.entries.removeAll { it.value.conversationId == conversationId }
    }

    private fun messagesOf(conversationId: Long): List<MessageEntity> =
        messages.values
            .filter { it.conversationId == conversationId }
            .sortedWith(compareBy({ it.timestamp }, { it.id }))
}
