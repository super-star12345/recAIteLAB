package com.recaitelab.app.ui.chat

import com.recaitelab.app.domain.model.UserError
import java.util.UUID

/**
 * 聊天页的完整界面状态。
 *
 * @property messages 当前会话的消息列表，按时间顺序
 * @property inputText 输入框内容
 * @property isLoadingHistory 是否正在从 Room 恢复上次会话的历史消息
 * @property isGenerating 是否正在生成（用于禁用发送 / 切换为停止按钮）
 * @property error 最近一次失败分类；成功发起新请求时清空
 * @property canRetry 失败后是否允许用「重试」重新生成（配置类错误不可重试）
 * @property providers 模型选择器数据，按 Provider 分组，不含 apiKey
 * @property activeProviderId 当前激活 Provider
 * @property activeProviderName 当前激活 Provider 的显示名
 * @property activeModel 当前选中的模型；未选择时为 null
 */
data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val inputText: String = "",
    val isLoadingHistory: Boolean = false,
    val isGenerating: Boolean = false,
    val error: UserError? = null,
    val canRetry: Boolean = false,
    val providers: List<ProviderModels> = emptyList(),
    val activeProviderId: UUID? = null,
    val activeProviderName: String? = null,
    val activeModel: String? = null,
)

/** 模型选择器中的 Provider 分组；刻意不包含 apiKey。 */
data class ProviderModels(
    val id: UUID,
    val name: String,
    val models: List<String>,
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
