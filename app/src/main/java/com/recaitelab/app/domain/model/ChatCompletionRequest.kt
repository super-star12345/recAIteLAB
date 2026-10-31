package com.recaitelab.app.domain.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * `POST {baseUrl}/chat/completions` 的请求体。
 *
 * 字段名与 OpenAI API JSON 完全一致；[temperature] 为 null 时通过
 * `@EncodeDefault(NEVER)` 与 `openAiJson.explicitNulls = false` 完全不出现在请求体中，
 * 由各厂商服务端使用自己的默认值。
 *
 * @property model 模型 id，例如 `gpt-4o-mini`
 * @property messages 多轮上下文，按时间顺序排列
 * @property stream 是否使用 SSE 流式返回，默认开启
 * @property temperature 采样温度；null 表示不发送该字段，取值由用户显式配置时才传入
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val temperature: Float? = null,
)
