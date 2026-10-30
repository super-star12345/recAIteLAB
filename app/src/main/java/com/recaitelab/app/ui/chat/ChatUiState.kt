package com.recaitelab.app.ui.chat

/**
 * 聊天页的完整界面状态。
 *
 * @property messages 当前会话的消息列表，按时间顺序
 * @property inputText 输入框内容
 * @property isGenerating 是否正在生成（用于禁用发送 / 切换为停止按钮）
 * @property errorMessage 最近一次失败提示，成功发起新请求时清空
 */
data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * 界面层消息，与 domain 的 `ChatMessage` 解耦，便于后续加状态（如 STOPPED）。
 *
 * @property role `user` / `assistant`
 * @property content 正文；流式期间持续增长
 * @property isStreaming 是否仍在流式输出（用于光标与“停止”状态）
 */
data class UiMessage(
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
) {
    companion object {
        const val ROLE_USER: String = "user"
        const val ROLE_ASSISTANT: String = "assistant"
    }
}
