package com.recaitelab.app.data.repository

import android.util.Log
import com.recaitelab.app.data.remote.ApiException
import com.recaitelab.app.domain.model.UserError
import java.io.IOException

/**
 * 统一错误映射：对话与知识整理共用同一套分类和文案。
 *
 * 日志只保留异常类型与脱敏后的堆栈；请求头、Authorization、API Key 永远不会进入日志。
 */
object ErrorMapper {

    /**
     * 把底层异常收敛为 [UserError]，并保留原始堆栈到日志。
     *
     * @param tag 调用方模块标签，便于定位是对话还是知识整理失败
     */
    fun map(tag: String, throwable: Throwable): UserError {
        val error = throwable.classify()
        Log.w(
            tag,
            "Request failed: ${throwable.javaClass.simpleName} -> $error\n${throwable.safeDetail()}",
        )
        return error
    }

    /** 纯分类逻辑，不写日志，便于单测覆盖。 */
    internal fun Throwable.classify(): UserError = when (this) {
        // ApiException 继承 IOException，必须先于 IOException 判断。
        is ApiException -> when (statusCode) {
            401, 403 -> UserError.INVALID_API_KEY
            429 -> UserError.RATE_LIMITED
            else -> UserError.UNKNOWN
        }

        is ChatConfigurationException -> UserError.CONFIGURATION

        // UnknownHostException / SocketTimeoutException / ConnectException / 断网等。
        is IOException -> UserError.NETWORK

        else -> UserError.UNKNOWN
    }

    /**
     * 即使异常消息意外携带密钥形态的文本也先脱敏；
     * 正常情况下 OkHttp 异常只包含 URL，不包含请求头。
     */
    private fun Throwable.safeDetail(): String =
        stackTraceToString().replace(SENSITIVE_TOKEN_REGEX, REDACTED)

    private val SENSITIVE_TOKEN_REGEX =
        Regex("""(?i)bearer\s+\S+|\bsk-[A-Za-z0-9_-]+""")

    private const val REDACTED = "[REDACTED]"
}
