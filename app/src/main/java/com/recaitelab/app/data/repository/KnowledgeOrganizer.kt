package com.recaitelab.app.data.repository

import android.util.Log
import com.recaitelab.app.data.remote.ChatProvider
import com.recaitelab.app.data.remote.createChatProvider
import com.recaitelab.app.domain.model.ChatMessage
import com.recaitelab.app.domain.model.KnowledgeEntry
import com.recaitelab.app.domain.model.ProviderConfig
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * 会话知识整理服务。
 *
 * 流程：读取指定会话全部消息 → 拼接「role: content」→ 用当前激活 Provider/模型
 * 复用 [ChatProvider.streamChat] 收集完整文本 → 解析 JSON → 写入知识库。
 *
 * 只复用现有网络能力，不新增网络代码，也不传 temperature。
 * 解析失败时用原始输出兜底并照常落库，避免丢失内容。
 */
class KnowledgeOrganizer(
    private val conversationRepository: ConversationRepository,
    private val providerRepository: ProviderRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val providerFactory: (ProviderConfig) -> ChatProvider = ::createChatProvider,
) {

    private val knowledgeJson: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    /**
     * 整理指定会话。
     *
     * @return 成功（含解析失败兜底）返回落库后的条目；配置缺失、会话为空或网络失败返回 failure
     */
    suspend fun organize(conversationId: Long): Result<KnowledgeEntry> {
        return try {
            val messages = conversationRepository.messagesFor(conversationId)
            if (messages.isEmpty()) {
                throw IllegalStateException("会话没有可整理的消息")
            }

            val config = providerRepository.activeProvider.value
                ?: throw ChatConfigurationException.NoActiveProvider()
            val model = config.selectedModel
                ?: throw ChatConfigurationException.NoSelectedModel(config.name)

            val transcript = messages
                .joinToString(separator = "\n") { message -> "${message.role}: ${message.content}" }
                .truncateTranscript()
            val prompt = PROMPT_PREFIX + "\n" + transcript

            val output = StringBuilder()
            providerFactory(config).streamChat(
                messages = listOf(ChatMessage(role = ROLE_USER, content = prompt)),
                model = model,
            ).collect { chunk -> output.append(chunk) }

            val raw = output.toString()
            val payload = parsePayload(raw)
            val entry = buildEntry(
                raw = raw,
                payload = payload,
                conversationId = conversationId,
                providerId = config.id.toString(),
                model = model,
            )
            val id = knowledgeRepository.insert(entry)
            // 只记录元数据，不输出对话内容或知识正文。
            Log.i(
                TAG,
                "Knowledge entry stored: id=$id, conversationId=$conversationId, " +
                    "fallback=${payload == null}",
            )
            Result.success(entry.copy(id = id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 先取首个 `{` 到末个 `}`，兼容 ```json 围栏和额外说明文字。 */
    private fun parsePayload(raw: String): KnowledgePayload? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val candidate = raw.substring(start, end + 1)
        return runCatching {
            knowledgeJson.decodeFromString<KnowledgePayload>(candidate)
        }.getOrNull()
    }

    private fun buildEntry(
        raw: String,
        payload: KnowledgePayload?,
        conversationId: Long,
        providerId: String,
        model: String,
    ): KnowledgeEntry {
        val now = System.currentTimeMillis()
        return if (payload != null) {
            KnowledgeEntry(
                title = payload.title.trim().ifBlank { FALLBACK_TITLE },
                summary = payload.summary.trim().ifBlank { raw.take(SUMMARY_FALLBACK_LENGTH) },
                content = payload.content.trim().ifBlank { raw },
                tags = payload.tags,
                sourceConversationId = conversationId,
                providerId = providerId,
                model = model,
                createdAt = now,
                updatedAt = now,
            )
        } else {
            KnowledgeEntry(
                title = FALLBACK_TITLE,
                summary = raw.take(SUMMARY_FALLBACK_LENGTH),
                content = raw,
                tags = emptyList(),
                sourceConversationId = conversationId,
                providerId = providerId,
                model = model,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    /** 超长对话保留首尾，后续阶段再做模型级截断策略。 */
    private fun String.truncateTranscript(): String {
        if (length <= MAX_TRANSCRIPT_CHARS) return this
        val headLength = MAX_TRANSCRIPT_CHARS / 2
        val tailLength = MAX_TRANSCRIPT_CHARS - headLength
        return take(headLength) +
            "\n...（对话过长，已截断）...\n" +
            takeLast(tailLength)
    }

    /** 模型返回的 JSON 结构；缺字段时用空串/空数组兜底。 */
    @Serializable
    private data class KnowledgePayload(
        val title: String = "",
        val summary: String = "",
        val content: String = "",
        val tags: List<String> = emptyList(),
    )

    private companion object {
        const val TAG: String = "KnowledgeOrganizer"
        const val ROLE_USER: String = "user"
        const val FALLBACK_TITLE: String = "未命名整理"
        const val SUMMARY_FALLBACK_LENGTH: Int = 100
        const val MAX_TRANSCRIPT_CHARS: Int = 20_000

        val PROMPT_PREFIX: String = """
            你是知识整理助手。请阅读以下对话记录，提炼有长期价值的知识，只输出 JSON：
            {"title":"知识标题","summary":"一句话摘要","content":"Markdown 正文","tags":["标签"]}
            要求：去除寒暄与重复；保留结论、方法、代码片段、踩坑经验；tags 不超过 5 个。
            对话记录如下：
        """.trimIndent()
    }
}
