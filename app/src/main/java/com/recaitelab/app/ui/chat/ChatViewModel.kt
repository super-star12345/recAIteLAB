package com.recaitelab.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recaitelab.app.data.remote.ApiException
import com.recaitelab.app.data.repository.ChatConfigurationException
import com.recaitelab.app.data.repository.ChatRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 聊天页 ViewModel：把 Repository 的增量流转换成 [ChatUiState]。
 *
 * 只依赖 AndroidX ViewModel 与协程，不引用任何 Compose 类型。
 *
 * @param repository 会话仓库；阶段 3 换成 Room 实现后构造方式不变
 */
class ChatViewModel(
    private val repository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())

    /** 界面唯一数据源；Composable 只读，不暴露可变类型。 */
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    /** 每次 send / retry 递增；用于丢弃停止、清空后仍在回调链上的迟到片段。 */
    private var requestId: Long = 0L

    /** 输入框内容变化。 */
    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * 发送用户消息并流式接收回复。
     *
     * 先同步追加 user 消息与 assistant 占位（`isStreaming = true`），
     * 再逐片段更新占位消息内容，形成打字机效果。
     */
    fun send(prompt: String) {
        val text = prompt.trim()
        if (text.isEmpty() || _uiState.value.isGenerating) return

        val id = ++requestId
        _uiState.update { state ->
            state.copy(
                messages = state.messages +
                    UiMessage(role = UiMessage.ROLE_USER, content = text) +
                    UiMessage(role = UiMessage.ROLE_ASSISTANT, content = "", isStreaming = true),
                inputText = "",
                isGenerating = true,
                errorMessage = null,
            )
        }
        launchGeneration(id) { repository.send(text) }
    }

    /** 停止生成：取消当前任务，保留已输出内容并结束流式标记。 */
    fun stop() {
        if (!_uiState.value.isGenerating) return
        requestId++
        generateJob?.cancel()
        generateJob = null
        finishGeneration()
    }

    /** 重新生成最后一条回复：替换末尾 assistant 消息后再次请求。 */
    fun retry() {
        val state = _uiState.value
        if (state.isGenerating || state.messages.none { it.role == UiMessage.ROLE_USER }) return

        val id = ++requestId
        _uiState.update { current ->
            val kept = current.messages.dropLastWhile { it.role == UiMessage.ROLE_ASSISTANT }
            current.copy(
                messages = kept + UiMessage(role = UiMessage.ROLE_ASSISTANT, content = "", isStreaming = true),
                isGenerating = true,
                errorMessage = null,
            )
        }
        launchGeneration(id) { repository.regenerate() }
    }

    /** 清空会话：取消进行中的生成，并同时清空 UI 与仓库上下文。 */
    fun clear() {
        requestId++
        generateJob?.cancel()
        generateJob = null
        repository.clearConversation()
        _uiState.value = ChatUiState()
    }

    private fun launchGeneration(id: Long, source: suspend () -> Flow<String>) {
        generateJob = viewModelScope.launch {
            try {
                source().collect { delta -> appendDelta(id, delta) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (id == requestId) {
                    _uiState.update { it.copy(errorMessage = e.toUserMessage()) }
                }
            } finally {
                if (id == requestId) {
                    finishGeneration()
                }
            }
        }
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

    private fun Throwable.toUserMessage(): String = when (this) {
        is ChatConfigurationException -> message ?: "对话配置不完整"
        is ApiException -> when (statusCode) {
            401, 403 -> "API Key 无效或无权限"
            404 -> "接口地址或模型不存在"
            429 -> "请求过于频繁，请稍后重试"
            in 500..599 -> "服务端错误（$statusCode）"
            else -> serverMessage.ifBlank { "请求失败（$statusCode）" }
        }
        is IOException -> "网络异常，请检查连接"
        else -> "生成失败，请重试"
    }
}
