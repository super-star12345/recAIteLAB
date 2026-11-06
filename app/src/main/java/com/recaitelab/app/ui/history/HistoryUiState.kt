package com.recaitelab.app.ui.history

import com.recaitelab.app.domain.model.UserError

/**
 * 历史记录页状态。
 *
 * @property conversations 按 updatedAt 倒序的会话摘要
 * @property isLoading 首次从 Room 读取列表期间为 true
 * @property pendingDelete 待确认删除的会话；非空时显示删除对话框
 * @property renameTarget 正在重命名的会话；非空时显示重命名对话框
 * @property renameInput 重命名输入框内容
 * @property renameError 重命名校验错误
 * @property isOrganizing 是否正在整理为知识（显示 Loading 对话框并禁止重复触发）
 * @property organizeSuccessEntryId 整理成功后的知识条目 id；非空时显示成功 Snackbar
 * @property error 删除/重命名/整理等操作失败分类，由 UI 映射为统一文案
 */
data class HistoryUiState(
    val conversations: List<HistoryConversationUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDelete: HistoryConversationUiModel? = null,
    val renameTarget: HistoryConversationUiModel? = null,
    val renameInput: String = "",
    val renameError: HistoryRenameError? = null,
    val isOrganizing: Boolean = false,
    val organizeSuccessEntryId: Long? = null,
    val error: UserError? = null,
)

/** 历史列表行数据；不含消息正文以外的隐私字段。 */
data class HistoryConversationUiModel(
    val id: Long,
    val title: String,
    val preview: String,
    val updatedAt: Long,
)

/** 重命名校验错误，由 UI 映射到 strings.xml。 */
enum class HistoryRenameError {
    EMPTY,
    TOO_LONG,
}
