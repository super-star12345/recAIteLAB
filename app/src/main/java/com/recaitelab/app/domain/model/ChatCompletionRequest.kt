package com.recaitelab.app.domain.model

import kotlinx.serialization.Serializable

/**
 * `POST {baseUrl}/chat/completions` 的请求体。
 *
 * 字段名与 OpenAI API JSON 完全一致；[temperature] 为空时不会出现在请求体中。
 *
 * @property model 模型 id，例如 `gpt-4o-mini`
 * @property messages 多轮上下文，按时间顺序排列
 * @property stream 是否使用 SSE 流式返回，默认开启
 * @property temperature 采样温度，取值 0.0–2.0；为空表示由服务端使用默认值
 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Float? = null,
)
