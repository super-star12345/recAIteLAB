package com.recaitelab.app.ui.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recaitelab.app.data.repository.KnowledgeRepository
import com.recaitelab.app.domain.model.KnowledgeEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 知识库列表 ViewModel。
 *
 * - 空查询订阅 [KnowledgeRepository.entries]（updatedAt 倒序）；
 * - 非空查询改用 [KnowledgeRepository.search]；
 * - 输入 300ms 防抖，避免每次按键都触发查询。
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class KnowledgeViewModel(
    private val repository: KnowledgeRepository,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow(KnowledgeUiState())

    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            queryFlow
                .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MILLIS }
                .flatMapLatest { query ->
                    if (query.isBlank()) repository.entries else repository.search(query)
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "加载知识库失败：${throwable.message ?: "未知错误"}",
                        )
                    }
                }
                .collect { entries ->
                    _uiState.update {
                        it.copy(
                            entries = entries.map { entry -> entry.toUiModel() },
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        queryFlow.value = value
    }

    fun requestDelete(entry: KnowledgeEntryUiModel) {
        _uiState.update { it.copy(pendingDelete = entry) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val target = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            try {
                repository.delete(target.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "删除知识条目失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun KnowledgeEntry.toUiModel(): KnowledgeEntryUiModel = KnowledgeEntryUiModel(
        id = id,
        title = title,
        summary = summary,
        tags = tags,
        updatedAt = updatedAt,
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS: Long = 300L
    }
}
