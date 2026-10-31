package com.recaitelab.app.data.local.model

/**
 * 历史记录列表用的会话摘要（Room 查询结果，不是实体）。
 *
 * @property lastMessage 该会话最后一条消息正文；没有消息时为 null
 */
data class ConversationSummary(
    val id: Long,
    val title: String,
    val updatedAt: Long,
    val lastMessage: String?,
)
