package com.recaitelab.app.domain.model

/**
 * 知识库条目（纯 Kotlin 领域模型）。
 *
 * @property id Room 主键；0 表示尚未落库
 * @property title 整理标题
 * @property summary 一句话摘要
 * @property content Markdown 正文
 * @property tags 标签列表，落库时由 `Converters` 以 JSON 数组存储
 * @property sourceConversationId 来源会话 id；会话删除后置空
 * @property providerId 整理所用 Provider 的 UUID 字符串；Provider 删除后置空
 * @property model 整理所用模型快照
 * @property createdAt / updatedAt epoch millis
 */
data class KnowledgeEntry(
    val id: Long = 0L,
    val title: String,
    val summary: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val sourceConversationId: Long? = null,
    val providerId: String? = null,
    val model: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
