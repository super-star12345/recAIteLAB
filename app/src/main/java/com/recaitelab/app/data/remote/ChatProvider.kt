package com.recaitelab.app.data.remote

import com.recaitelab.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * 聊天能力抽象，屏蔽具体厂商的协议差异。
 *
 * MVP 只实现 [OpenAiCompatibleProvider]；Gemini / Anthropic 后续实现同一接口。
 */
interface ChatProvider {

    /**
     * 发起流式对话，逐段返回 `delta.content` 增量文本。
     *
     * 返回的 [Flow] 是冷的：每次收集都会发起一次新请求；
     * 收集被取消时，底层 HTTP 调用随之取消，不会留下悬挂连接。
     *
     * @param messages 多轮上下文（含 system），按时间顺序排列
     * @param model 模型 id，例如 `gpt-4o-mini`
     * @param temperature 采样温度，取值 0.0–2.0
     */
    suspend fun streamChat(
        messages: List<ChatMessage>,
        model: String,
        temperature: Float,
    ): Flow<String>

    /**
     * 拉取服务端当前可用的模型 id 列表（`GET {baseUrl}/models`）。
     *
     * 调用方负责把结果写入 Provider 配置；实现应使用 Bearer 鉴权。
     */
    suspend fun fetchModels(): List<String>
}
