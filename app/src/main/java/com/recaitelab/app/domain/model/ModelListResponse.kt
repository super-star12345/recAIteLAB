package com.recaitelab.app.domain.model

import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容 `GET {baseUrl}/models` 的响应。
 *
 * 只关心 `data[].id`，其余字段由 `openAiJson` 的 `ignoreUnknownKeys` 忽略。
 */
@Serializable
data class ModelListResponse(
    val data: List<ModelEntry> = emptyList(),
)

@Serializable
data class ModelEntry(
    val id: String = "",
)
