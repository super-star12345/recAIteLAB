package com.recaitelab.app.data.repository

import com.recaitelab.app.domain.model.KnowledgeEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** 模型返回的知识 JSON 结构；缺字段时使用空串/空数组兜底。 */
@Serializable
internal data class KnowledgePayload(
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
)

/**
 * 知识整理输出的纯解析逻辑（不依赖网络 / Room / Android）。
 *
 * - 先取首个 `{` 到末个 `}`，兼容 ```json 围栏与前后多余文字；
 * - 完全非 JSON 或结构非法时返回 null，由 [buildEntry] 兜底保存原文；
 * - 可选字段缺失时使用默认值，字段为空时逐项兜底。
 */
internal object KnowledgePayloadParser {

    private val knowledgeJson: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun parse(raw: String): KnowledgePayload? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val candidate = raw.substring(start, end + 1)
        return runCatching {
            knowledgeJson.decodeFromString<KnowledgePayload>(candidate)
        }.getOrNull()
    }

    fun buildEntry(
        raw: String,
        payload: KnowledgePayload?,
        conversationId: Long,
        providerId: String,
        model: String,
        now: Long = System.currentTimeMillis(),
    ): KnowledgeEntry {
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

    private const val FALLBACK_TITLE: String = "未命名整理"
    private const val SUMMARY_FALLBACK_LENGTH: Int = 100
}
