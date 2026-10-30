package com.recaitelab.app.domain.model

import kotlinx.serialization.json.Json

/**
 * OpenAI 兼容接口共用的 JSON 配置。
 *
 * - `ignoreUnknownKeys = true`：厂商返回的额外字段（id / usage / system_fingerprint 等）不参与映射；
 * - `explicitNulls = false`：可选字段为空时不输出 `"field": null`，避免部分厂商拒绝未知/空字段；
 * - `encodeDefaults = true`：`stream = true` 这类默认值必须写进请求体，否则服务端会按非流式返回。
 *
 * 网络层必须复用同一实例，保证请求与响应解析行为一致。
 */
val openAiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
