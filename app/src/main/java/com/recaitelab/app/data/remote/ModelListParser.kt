package com.recaitelab.app.data.remote

import com.recaitelab.app.domain.model.ModelListResponse
import com.recaitelab.app.domain.model.openAiJson
import kotlinx.serialization.decodeFromString

/**
 * `GET /models` 响应解析：只取 `data[].id`，去空白、去空、去重。
 *
 * 返回 [Result] 而不是直接抛异常，便于调用方区分空列表与结构异常；
 * 解析失败时由 `fetchModels` 继续以异常上抛，UI 层已有错误提示。
 */
internal object ModelListParser {

    fun parse(raw: String): Result<List<String>> = runCatching {
        openAiJson.decodeFromString<ModelListResponse>(raw)
            .data
            .map { it.id.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
}
