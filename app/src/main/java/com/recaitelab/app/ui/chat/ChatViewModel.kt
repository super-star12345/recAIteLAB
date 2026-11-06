package com.recaitelab.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recaitelab.app.data.repository.ChatRepository
import com.recaitelab.app.data.repository.ErrorMapper
import com.recaitelab.app.data.repository.ProviderRepository
import com.recaitelab.app.domain.model.UserError
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 聊天页 ViewModel：把 Repository 的增量流转换成 [ChatUiState]。
 *
 * 只依赖 AndroidX ViewModel 与协程，不引用任何 Compose 类型。
 *
 * @param repository 会话仓库；历史消息从 Room 恢复，流式回复仍在内存增量渲染
 * @param providerRepository 提供当前激活 Provider / 模型，以及模型选择器的数据
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val providerRepository: ProviderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(isLoadingHistory = true))

    /** 界面唯一数据源；Composable 只读，不暴露可变类型。 */
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    /** 首次进入时加载历史的任务；clear() 会取消它，避免旧快照覆盖刚清空的 UI。 */
    private var historyLoadJob: Job? = null

    /** 当前展示的会话 id；用于历史记录删除当前会话时重置 UI。 */
    private var currentConversationId: Long? = null

    /** 每次 send / retry 递增；用于丢弃停止、清空后仍在回调链上的迟到片段。 */
    private var requestId: Long = 0L

    init {
        historyLoadJob = viewModelScope.launch {
            try {
                val history = repository.loadConversation()
                currentConversationId = repository.activeConversationId.value
                _uiState.update { state ->
                    state.copy(
                        messages = history.map { message ->
                            UiMessage(role = message.role, content = message.content)
                        },
                        isLoadingHistory = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        error = ErrorMapper.map(TAG, e),
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.activeConversationId.collect { conversationId ->
                // 删除会话时 active 会变 null；此时保留 currentConversationId，
                // 等 onConversationDeleted(id) 做精确重置。
                if (conversationId != null) {
                    currentConversationId = conversationId
                }
            }
        }

        viewModelScope.launch {
            combine(
                providerRepository.providers,
                providerRepository.activeProvider,
            ) { providers, active ->
                ChatProviderSnapshot(
                    providers = providers.map {
                        ProviderModels(id = it.id, name = it.name, models = it.models)
                    },
                    activeProviderId = active?.id,
                    activeProviderName = active?.name,
                    activeModel = active?.selectedModel,
                )
            }.collect { snapshot ->
                _uiState.update {
                    it.copy(
                        providers = snapshot.providers,
                        activeProviderId = snapshot.activeProviderId,
                        activeProviderName = snapshot.activeProviderName,
                        activeModel = snapshot.activeModel,
                    )
                }
            }
        }
    }

    /** 输入框内容变化。 */
    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /** 切换 Provider 与模型；只影响之后发送的消息，进行中的流不受影响。 */
    fun selectModel(providerId: UUID, model: String) {
        viewModelScope.launch {
            try {
                providerRepository.setSelectedModel(providerId, model)
                providerRepository.setActiveProvider(providerId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = ErrorMapper.map(TAG, e), canRetry = false)
                }
            }
        }
    }

    /**
     * 打开历史记录中的指定会话：取消当前生成，加载该会话消息并替换 UI。
     * 导航到 AI 对话 Tab 由调用方负责。
     */
    fun openConversation(conversationId: Long) {
        requestId++
        historyLoadJob?.cancel()
        historyLoadJob = null
        generateJob?.cancel()
        generateJob = null
        currentConversationId = conversationId
        _uiState.update { state ->
            state.copy(
                messages = emptyList(),
                inputText = "",
                isLoadingHistory = true,
                isGenerating = false,
                error = null,
                canRetry = false,
            )
        }
        historyLoadJob = viewModelScope.launch {
            try {
                val messages = repository.openConversation(conversationId)
                if (messages == null) {
                    currentConversationId = null
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            error = UserError.NOT_FOUND,
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            messages = messages.map { message ->
                                UiMessage(role = message.role, content = message.content)
                            },
                            isLoadingHistory = false,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        error = ErrorMapper.map(TAG, e),
                    )
                }
            }
        }
    }

    /**
     * 开始新上下文：不删除任何历史会话，只清空当前指针；
     * 下一次发送时由 ConversationRepository 创建新会话。
     */
    fun startNewConversation() {
        requestId++
        historyLoadJob?.cancel()
        historyLoadJob = null
        generateJob?.cancel()
        generateJob = null
        currentConversationId = null
        _uiState.update { state ->
            ChatUiState(
                providers = state.providers,
                activeProviderId = state.activeProviderId,
                activeProviderName = state.activeProviderName,
                activeModel = state.activeModel,
                isLoadingHistory = true,
            )
        }
        viewModelScope.launch {
            try {
                repository.startNewConversation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = ErrorMapper.map(TAG, e), canRetry = false)
                }
            } finally {
                _uiState.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    /** 历史记录删除了当前会话时，停止生成并清空聊天页，避免继续向已删除会话写入。 */
    fun onConversationDeleted(conversationId: Long) {
        if (currentConversationId != conversationId) return
        requestId++
        historyLoadJob?.cancel()
        historyLoadJob = null
        generateJob?.cancel()
        generateJob = null
        currentConversationId = null
        _uiState.update { state ->
            ChatUiState(
                providers = state.providers,
                activeProviderId = state.activeProviderId,
                activeProviderName = state.activeProviderName,
                activeModel = state.activeModel,
            )
        }
    }

    /**
     * 发送用户消息并流式接收回复。
     *
     * 先同步追加 user 消息与 assistant 占位（`isStreaming = true`），
     * 再逐片段更新占位消息内容，形成打字机效果。
     */
    fun send(prompt: String) {
        val text = prompt.trim()
        val state = _uiState.value
        if (text.isEmpty() || state.isGenerating || state.isLoadingHistory) return

        val id = ++requestId
        _uiState.update { current ->
            current.copy(
                messages = current.messages +
                    UiMessage(role = UiMessage.ROLE_USER, content = text) +
                    UiMessage(role = UiMessage.ROLE_ASSISTANT, content = "", isStreaming = true),
                inputText = "",
                isGenerating = true,
                error = null,
                canRetry = false,
            )
        }
        launchGeneration(id, restorePrompt = text) { repository.send(text) }
    }

    /** 停止生成：取消当前任务，保留已输出内容并结束流式标记；不视为错误。 */
    fun stop() {
        if (!_uiState.value.isGenerating) return
        requestId++
        generateJob?.cancel()
        generateJob = null
        finishGeneration()
    }

    /** 清空本页的 Snackbar 错误提示。 */
    fun onErrorShown() {
        _uiState.update { it.copy(error = null, canRetry = false) }
    }

    /** 重新生成最后一条回复：替换末尾 assistant 消息后再次请求。 */
    fun retry() {
        val state = _uiState.value
        if (state.isGenerating || state.isLoadingHistory) return
        if (state.messages.none { it.role == UiMessage.ROLE_USER }) return

        val id = ++requestId
        _uiState.update { current ->
            val kept = current.messages.dropLastWhile { it.role == UiMessage.ROLE_ASSISTANT }
            current.copy(
                messages = kept + UiMessage(role = UiMessage.ROLE_ASSISTANT, content = "", isStreaming = true),
                isGenerating = true,
                error = null,
                canRetry = false,
            )
        }
        launchGeneration(id, restorePrompt = null) { repository.regenerate() }
    }

    /** 清空会话：取消进行中的生成，清除 Room 中的当前会话，并重置 UI。 */
    fun clear() {
        requestId++
        historyLoadJob?.cancel()
        historyLoadJob = null
        generateJob?.cancel()
        generateJob = null
        currentConversationId = null
        _uiState.update { state ->
            ChatUiState(
                providers = state.providers,
                activeProviderId = state.activeProviderId,
                activeProviderName = state.activeProviderName,
                activeModel = state.activeModel,
                isLoadingHistory = true,
            )
        }
        viewModelScope.launch {
            try {
                repository.clearConversation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = ErrorMapper.map(TAG, e), canRetry = false)
                }
            } finally {
                _uiState.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    /**
     * 收集流式回复。
     *
     * @param restorePrompt 发送路径传入用户输入；配置错误导致消息还没落库时，用它回滚 UI
     *   并把输入内容还原到输入框，避免用户重输。重试路径传 null。
     */
    private fun launchGeneration(
        id: Long,
        restorePrompt: String?,
        source: suspend () -> Flow<String>,
    ) {
        generateJob = viewModelScope.launch {
            try {
                source().collect { delta -> appendDelta(id, delta) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (id == requestId) {
                    val error = ErrorMapper.map(TAG, e)
                    // 配置错误发生在写 user 消息之前，聊天页里只是占位，回滚并还原输入。
                    val rollbackPrompt = restorePrompt?.takeIf { error == UserError.CONFIGURATION }
                    _uiState.update { state ->
                        state.copy(
                            messages = if (rollbackPrompt != null) {
                                state.messages.rollbackFailedSend(rollbackPrompt)
                            } else {
                                state.messages
                            },
                            inputText = if (rollbackPrompt != null) {
                                state.inputText.ifBlank { rollbackPrompt }
                            } else {
                                state.inputText
                            },
                            error = error,
                            // 配置类错误重试也不会成功，先去设置页补齐配置。
                            canRetry = error != UserError.CONFIGURATION,
                        )
                    }
                }
            } finally {
                if (id == requestId) {
                    finishGeneration()
                }
            }
        }
    }

    /** 回滚一次未落库的发送：空 assistant 占位 + 本次 user 消息。 */
    private fun List<UiMessage>.rollbackFailedSend(prompt: String): List<UiMessage> {
        var result = this
        val last = result.lastOrNull()
        if (last != null && last.role == UiMessage.ROLE_ASSISTANT && last.content.isEmpty()) {
            result = result.dropLast(1)
        }
        val lastUser = result.lastOrNull()
        if (lastUser != null && lastUser.role == UiMessage.ROLE_USER && lastUser.content == prompt) {
            result = result.dropLast(1)
        }
        return result
    }

    private fun appendDelta(id: Long, delta: String) {
        if (id != requestId) return
        _uiState.update { state ->
            val last = state.messages.lastOrNull()
            if (last == null || last.role != UiMessage.ROLE_ASSISTANT) return@update state
            state.copy(
                messages = state.messages.dropLast(1) +
                    last.copy(content = last.content + delta, isStreaming = true),
            )
        }
    }

    /** 结束流式：去掉没有产出任何内容的空占位，避免留下空气泡。 */
    private fun finishGeneration() {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.finishLastAssistant(),
                isGenerating = false,
            )
        }
    }

    private fun List<UiMessage>.finishLastAssistant(): List<UiMessage> {
        val last = lastOrNull() ?: return this
        if (last.role != UiMessage.ROLE_ASSISTANT) return this
        return if (last.content.isEmpty()) {
            dropLast(1)
        } else {
            dropLast(1) + last.copy(isStreaming = false)
        }
    }

    private data class ChatProviderSnapshot(
        val providers: List<ProviderModels>,
        val activeProviderId: UUID?,
        val activeProviderName: String?,
        val activeModel: String?,
    )

    private companion object {
        const val TAG: String = "ChatViewModel"
    }
}
