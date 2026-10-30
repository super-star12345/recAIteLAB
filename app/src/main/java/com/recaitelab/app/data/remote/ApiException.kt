package com.recaitelab.app.data.remote

import java.io.IOException

/**
 * 服务端返回非 2xx 状态码时抛出。
 *
 * @property statusCode HTTP 状态码，供上层映射为 Key 无效 / 频率超限 / 服务端错误等提示
 * @property serverMessage 服务端返回的错误信息（已截断，可能为空）
 */
class ApiException(
    val statusCode: Int,
    val serverMessage: String,
) : IOException(
    if (serverMessage.isBlank()) "HTTP $statusCode" else "HTTP $statusCode: $serverMessage",
)
