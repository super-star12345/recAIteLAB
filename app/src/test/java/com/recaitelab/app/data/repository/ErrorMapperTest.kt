package com.recaitelab.app.data.repository

import com.recaitelab.app.data.remote.ApiException
import com.recaitelab.app.domain.model.UserError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 统一错误映射回归测试：对话与知识整理共用 [ErrorMapper.classify]。
 *
 * 这里只测试纯分类逻辑，不触发 `android.util.Log`。
 */
class ErrorMapperTest {

    private fun Throwable.classifyForTest(): UserError = with(ErrorMapper) { classify() }

    @Test
    fun unauthorizedMapsToInvalidApiKey() {
        assertEquals(UserError.INVALID_API_KEY, ApiException(401, "unauthorized").classifyForTest())
        assertEquals(UserError.INVALID_API_KEY, ApiException(403, "forbidden").classifyForTest())
    }

    @Test
    fun tooManyRequestsMapsToRateLimited() {
        assertEquals(UserError.RATE_LIMITED, ApiException(429, "rate limited").classifyForTest())
    }

    @Test
    fun otherHttpErrorsMapToUnknown() {
        assertEquals(UserError.UNKNOWN, ApiException(400, "bad request").classifyForTest())
        assertEquals(UserError.UNKNOWN, ApiException(404, "not found").classifyForTest())
        assertEquals(UserError.UNKNOWN, ApiException(500, "server error").classifyForTest())
    }

    @Test
    fun ioExceptionsMapToNetwork() {
        assertEquals(UserError.NETWORK, UnknownHostException("no host").classifyForTest())
        assertEquals(UserError.NETWORK, SocketTimeoutException("timeout").classifyForTest())
        assertEquals(UserError.NETWORK, IOException("offline").classifyForTest())
    }

    @Test
    fun missingConfigurationMapsToConfiguration() {
        val error = ChatConfigurationException.NoSelectedModel("DeepSeek")
        assertEquals(UserError.CONFIGURATION, error.classifyForTest())
    }

    @Test
    fun unexpectedExceptionMapsToUnknown() {
        assertEquals(UserError.UNKNOWN, IllegalStateException("boom").classifyForTest())
    }
}
