package com.recaitelab.app.data.repository

import com.recaitelab.app.data.remote.ChatProvider
import com.recaitelab.app.data.remote.createChatProvider
import com.recaitelab.app.domain.model.ChatMessage
import com.recaitelab.app.domain.model.ProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 会话仓库：维护当前上下文，并使用 [ProviderRepository] 中激活的 Provider / 模型发起请求。
 *
 * 当前是单会话内存实现；阶段 3 接 Room 时只替换历史的读写（构造注入 DAO），
 * 对外的 `send` / `regenerate` / `messages` / `clearConversation` 保持不变。
 *
 * 切换 Provider / 模型不会清空 [messages]：每次 send/regenerate 才重新解析激活配置，
 * 因此新配置只影响之后发出的消息，进行中的流仍使用发起时捕获的快照。
 *
 * 历史由 [lock] 保护，可从任意线程读写。
 *
 * @param providerRepository 提供当前激活的 Provider 与模型
 * @param temperature 采样温度，默认 0.7
 * @param providerFactory 按配置创建 ChatProvider；默认 [createChatProvider]，测试时可替换
 */
class ChatRepository(
    private val providerRepository: ProviderRepository,
    private val temperature: Float = DEFAULT_TEMPERATURE,
    private val providerFactory: (ProviderConfig) -> ChatProvider = ::createChatProvider,
) {

    private val lock = Any()
    private val history = mutableListOf<ChatMessage>()

    /** 当前上下文的只读快照；换 Room 后改为 DAO 的 `Flow<List<ChatMessage>>`。 */
    val messages: List<ChatMessage>
        get() = synchronized(lock) { history.toList() }

    /**
     * 发送一条用户消息，返回逐段回复的冷流。
     *
     * - 发送时解析当前激活的 Provider 与模型，因此切换 Provider 后下一条消息立即生效；
     * - 调用本方法时立即把 [prompt] 追加为 user 消息；
     * - 回复片段原样下发，同时在内部累积为一条 assistant 消息；
     * - 只有正常读到流的结尾（`[DONE]` / `finish_reason`）才把完整回复写回历史；
     *   取消或异常时已下发的内容仍留给 UI，但不进入上下文；
     * - 返回的 Flow 是冷的，只应收集一次；生成期间 UI 应禁用再次发送。
     */
    suspend fun send(prompt: String): Flow<String> {
        require(prompt.isNotBlank()) { "prompt 不能为空" }

        // 先把本条 user 消息写入上下文，再解析配置；即使随后因未选模型失败，UI 与仓库也保持一致。
        val context = synchronized(lock) {
            history += ChatMessage(role = ROLE_USER, content = prompt)
            history.toList()
        }
        val target = resolveTarget()
        return streamReply(context, target)
    }

    /**
     * 重新生成最后一条回复。
     *
     * 丢弃历史末尾的 assistant 消息（若有），以剩余上下文再次请求，对应 UI 的「重新生成」。
     * 末尾只有 user 消息时（例如上次被停止或失败），直接基于当前上下文重试。
     */
    suspend fun regenerate(): Flow<String> {
        val target = resolveTarget()
        val context = synchronized(lock) {
            if (history.lastOrNull()?.role == ROLE_ASSISTANT) {
                history.removeAt(history.lastIndex)
            }
            require(history.isNotEmpty()) { "没有可重新生成的上下文" }
            history.toList()
        }
        return streamReply(context, target)
    }

    private fun streamReply(
        context: List<ChatMessage>,
        target: ChatTarget,
    ): Flow<String> = flow {
        val reply = StringBuilder()
        providerFactory(target.config).streamChat(
            messages = context,
            model = target.model,
            temperature = temperature,
        ).collect { delta ->
            reply.append(delta)
            emit(delta)
        }
        if (reply.isNotEmpty()) {
            synchronized(lock) {
                history += ChatMessage(role = ROLE_ASSISTANT, content = reply.toString())
            }
        }
    }

    private fun resolveTarget(): ChatTarget {
        val config = providerRepository.activeProvider.value
            ?: throw ChatConfigurationException.NoActiveProvider()
        val model = config.selectedModel
            ?: throw ChatConfigurationException.NoSelectedModel(config.name)
        return ChatTarget(config = config, model = model)
    }

    /** 发起一次请求时锁定的 Provider 与模型快照。 */
    private data class ChatTarget(
        val config: ProviderConfig,
        val model: String,
    )

    /** 清空当前会话上下文（仅内存）。 */
    fun clearConversation() {
        synchronized(lock) {
            history.clear()
        }
    }

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val DEFAULT_TEMPERATURE = 0.7f
    }
}
