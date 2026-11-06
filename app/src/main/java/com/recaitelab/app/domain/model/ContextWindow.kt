package com.recaitelab.app.domain.model

/**
 * 发送给模型的上下文窗口策略（纯 Kotlin，便于单测与后续做成设置项）。
 *
 * 规则：
 * - 所有 `system` 消息原样保留；
 * - 保留最近 [MAX_CONTEXT_MESSAGES] 条非 system 历史；
 * - 最后一条非 system 消息视为「当前消息」始终保留，且不计入历史条数。
 *
 * 只返回裁剪后的新列表，不修改入参，也不影响 Room 中的完整历史。
 */
object ContextWindow {

    /** 最多保留的历史消息条数（不含 system 与当前消息）。 */
    const val MAX_CONTEXT_MESSAGES: Int = 20

    private const val ROLE_SYSTEM: String = "system"

    /**
     * 构造实际发送的上下文。
     *
     * @param messages 按时间正序的完整会话消息（可包含 system、当前用户消息）
     * @param maxHistoryMessages 历史消息上限；后续做成设置项时直接传入配置值
     */
    fun truncate(
        messages: List<ChatMessage>,
        maxHistoryMessages: Int = MAX_CONTEXT_MESSAGES,
    ): List<ChatMessage> {
        require(maxHistoryMessages >= 0) { "maxHistoryMessages 不能为负" }
        if (messages.isEmpty()) return emptyList()

        val systemMessages = messages.filter { it.role == ROLE_SYSTEM }
        val conversation = messages.filter { it.role != ROLE_SYSTEM }
        if (conversation.isEmpty()) return systemMessages

        val current = conversation.last()
        val history = conversation.dropLast(1).takeLast(maxHistoryMessages)
        return systemMessages + history + current
    }
}
