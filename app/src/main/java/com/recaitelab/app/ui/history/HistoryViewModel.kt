package com.recaitelab.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recaitelab.app.data.local.model.ConversationSummary
import com.recaitelab.app.data.repository.ConversationRepository
import com.recaitelab.app.data.repository.ErrorMapper
import com.recaitelab.app.data.repository.KnowledgeOrganizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 历史记录 Tab 的 ViewModel。
 *
 * - 列表来自 Room 的会话摘要 Flow（updatedAt 倒序、含最后一条消息）；
 * - 删除/重命名通过 [ConversationRepository] 落库；
 * - 删除当前会话后聊天页的清理由 MainActivity 协调（调用 ChatViewModel）。
 */
class HistoryViewModel(
    private val conversationRepository: ConversationRepository,
    private val knowledgeOrganizer: KnowledgeOrganizer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())

    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            conversationRepository.observeSummaries()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = ErrorMapper.map(TAG, throwable),
                        )
                    }
                }
                .collect { summaries ->
                    _uiState.update { state ->
                        state.copy(
                            conversations = summaries.map { it.toUiModel() },
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun requestDelete(conversation: HistoryConversationUiModel) {
        _uiState.update {
            it.copy(
                pendingDelete = conversation,
                renameTarget = null,
                renameError = null,
            )
        }
    }

    /**
     * 整理指定会话为知识。
     *
     * 运行在 viewModelScope 中，不阻塞主线程；整理期间通过 [HistoryUiState.isOrganizing]
     * 显示 Loading 并拒绝重复触发。
     */
    fun organize(conversation: HistoryConversationUiModel) {
        if (_uiState.value.isOrganizing) return
        _uiState.update {
            it.copy(
                isOrganizing = true,
                organizeSuccessEntryId = null,
                error = null,
            )
        }
        viewModelScope.launch {
            try {
                knowledgeOrganizer.organize(conversation.id).fold(
                    onSuccess = { entry ->
                        _uiState.update {
                            it.copy(
                                isOrganizing = false,
                                organizeSuccessEntryId = entry.id,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isOrganizing = false,
                                error = ErrorMapper.map(TAG, error),
                            )
                        }
                    },
                )
            } finally {
                _uiState.update { it.copy(isOrganizing = false) }
            }
        }
    }

    fun onOrganizeSuccessShown() {
        _uiState.update { it.copy(organizeSuccessEntryId = null) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val target = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            try {
                conversationRepository.deleteConversation(target.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = ErrorMapper.map(TAG, e))
                }
            }
        }
    }

    fun startRename(conversation: HistoryConversationUiModel) {
        _uiState.update {
            it.copy(
                renameTarget = conversation,
                renameInput = conversation.title,
                renameError = null,
                pendingDelete = null,
            )
        }
    }

    fun onRenameInputChange(value: String) {
        _uiState.update { it.copy(renameInput = value, renameError = null) }
    }

    fun confirmRename() {
        val state = _uiState.value
        val target = state.renameTarget ?: return
        val title = state.renameInput.trim()
        val length = title.codePointCount(0, title.length)
        val error = when {
            title.isEmpty() -> HistoryRenameError.EMPTY
            length > MAX_TITLE_LENGTH -> HistoryRenameError.TOO_LONG
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(renameError = error) }
            return
        }

        _uiState.update {
            it.copy(
                renameTarget = null,
                renameInput = "",
                renameError = null,
            )
        }
        viewModelScope.launch {
            try {
                conversationRepository.renameConversation(target.id, title)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = ErrorMapper.map(TAG, e))
                }
            }
        }
    }

    fun cancelRename() {
        _uiState.update {
            it.copy(
                renameTarget = null,
                renameInput = "",
                renameError = null,
            )
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    private fun ConversationSummary.toUiModel(): HistoryConversationUiModel =
        HistoryConversationUiModel(
            id = id,
            title = title,
            preview = lastMessage
                ?.trim()
                ?.replace(WHITESPACE_REGEX, " ")
                ?.take(PREVIEW_MAX_LENGTH)
                .orEmpty(),
            updatedAt = updatedAt,
        )

    private companion object {
        const val TAG: String = "HistoryViewModel"
        const val MAX_TITLE_LENGTH: Int = 40
        const val PREVIEW_MAX_LENGTH: Int = 80
        val WHITESPACE_REGEX: Regex = Regex("\\s+")
    }
}
