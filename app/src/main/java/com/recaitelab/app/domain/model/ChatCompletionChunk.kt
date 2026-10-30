package com.recaitelab.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SSE 流式响应中单个 `data:` 行的最小结构。
 *
 * 仅保留增量解析所必需的部分，其余字段（id / object / model / usage 等）
 * 由 [openAiJson] 的 `ignoreUnknownKeys` 忽略。
 *
 * @property choices 候选列表，通常只有一项
 */
@Serializable
data class ChatCompletionChunk(
    val choices: List<ChatCompletionChunkChoice> = emptyList(),
)

/**
 * 单个候选的流式增量。
 *
 * @property delta 本片段的增量内容；首包可能只有 role，故允许为空
 * @property finishReason 结束原因，正常结束时为 `stop`；生成中为 null
 */
@Serializable
data class ChatCompletionChunkChoice(
    val delta: ChatCompletionDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

/**
 * 增量内容，正文在 `delta.content` 中逐段下发。
 *
 * @property content 本片段新增的文本；可能为 null（如角色包、结束包）
 */
@Serializable
data class ChatCompletionDelta(
    val content: String? = null,
)
