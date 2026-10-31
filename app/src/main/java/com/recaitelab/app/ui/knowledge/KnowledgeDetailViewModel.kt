package com.recaitelab.app.ui.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recaitelab.app.data.repository.KnowledgeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 知识详情页 ViewModel：按 id 读取一次条目。 */
class KnowledgeDetailViewModel(
    private val repository: KnowledgeRepository,
    private val entryId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeDetailUiState())

    val uiState: StateFlow<KnowledgeDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val entry = repository.getById(entryId)
                _uiState.update {
                    it.copy(
                        entry = entry,
                        isLoading = false,
                        isNotFound = entry == null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载知识详情失败：${e.message ?: "未知错误"}",
                    )
                }
            }
        }
    }
}
