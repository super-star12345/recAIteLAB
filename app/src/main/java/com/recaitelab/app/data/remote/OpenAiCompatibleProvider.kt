package com.recaitelab.app.data.remote

import com.recaitelab.app.domain.model.ChatCompletionChunk
import com.recaitelab.app.domain.model.ChatCompletionRequest
import com.recaitelab.app.domain.model.ChatMessage
import com.recaitelab.app.domain.model.ModelListResponse
import com.recaitelab.app.domain.model.openAiJson
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSource

/**
 * OpenAI 兼容实现：`POST {baseUrl}/chat/completions`，`stream = true`，逐行解析 SSE。
 *
 * 只使用 OkHttp 原生 [Call]（`enqueue`），不引入 Retrofit。
 *
 * @param baseUrl 接口根地址，例如 `https://api.openai.com/v1`；末尾斜杠可有可无
 * @param apiKey 用户自带的 Key，仅用于请求头，不落日志
 * @param client 建议由 `di/AppContainer` 注入单例；默认复用模块内的共享实例
 */
class OpenAiCompatibleProvider(
    baseUrl: String,
    private val apiKey: String,
    private val client: OkHttpClient = sharedHttpClient,
) : ChatProvider {

    private val normalizedBaseUrl: String = baseUrl.trim().trimEnd('/')
    private val endpoint: String = normalizedBaseUrl + CHAT_COMPLETIONS_PATH
    private val modelsEndpoint: String = normalizedBaseUrl + MODELS_PATH

    override suspend fun streamChat(
        messages: List<ChatMessage>,
        model: String,
        temperature: Float?,
    ): Flow<String> = callbackFlow {
        val payload = openAiJson.encodeToString(
            ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature,
            ),
        )
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            close(ApiException(resp.code, resp.errorMessage()))
                            return
                        }
                        val body = resp.body ?: throw IOException("HTTP ${resp.code} 响应体为空")
                        readSse(body.source()) { delta -> trySend(delta).isSuccess }
                        close()
                    }
                } catch (e: Throwable) {
                    close(e)
                }
            }
        })

        awaitClose { call.cancel() }
    }.buffer(Channel.UNLIMITED)

    override suspend fun fetchModels(): List<String> {
        val request = Request.Builder()
            .url(modelsEndpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .get()
            .build()

        val call = client.newCall(request)
        // 模型列表请求使用整体超时，避免共享 client 的 readTimeout=0 导致一直挂起。
        call.timeout().timeout(MODELS_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    ApiException(resp.code, resp.errorMessage()),
                                )
                            }
                            return
                        }
                        try {
                            val raw = resp.body?.string()
                                ?: throw IOException("HTTP ${resp.code} 响应体为空")
                            val models = openAiJson.decodeFromString<ModelListResponse>(raw)
                                .data
                                .map { it.id.trim() }
                                .filter { it.isNotEmpty() }
                                .distinct()
                            if (continuation.isActive) {
                                continuation.resume(models)
                            }
                        } catch (e: Throwable) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(e)
                            }
                        }
                    }
                }
            })
        }
    }

    /**
     * 阻塞读取 SSE 事件，直到 `[DONE]`、`finish_reason` 或流结束。
     *
     * 运行在 OkHttp 的 callback 线程；[emit] 返回 false 表示下游已取消。
     */
    private inline fun readSse(source: BufferedSource, emit: (String) -> Boolean) {
        val reader = SseEventReader(source)
        while (true) {
            val data = reader.nextEventData() ?: return
            val payload = data.trim()
            if (payload == DONE_MARKER) return

            val choice = openAiJson.decodeFromString<ChatCompletionChunk>(payload)
                .choices.firstOrNull()
            val delta = choice?.delta?.content
            if (!delta.isNullOrEmpty() && !emit(delta)) return
            if (choice?.finishReason != null) return
        }
    }

    private fun Response.errorMessage(): String {
        val raw = try {
            body?.string().orEmpty()
        } catch (_: IOException) {
            ""
        }
        if (raw.isBlank()) return ""

        val message = try {
            openAiJson.parseToJsonElement(raw)
                .jsonObject["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        return (message ?: raw).trim().take(MAX_ERROR_MESSAGE_LENGTH)
    }

    private companion object {
        const val CHAT_COMPLETIONS_PATH = "/chat/completions"
        const val MODELS_PATH = "/models"
        const val MODELS_TIMEOUT_SECONDS = 30L
        const val DONE_MARKER = "[DONE]"
        const val MAX_ERROR_MESSAGE_LENGTH = 300
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/**
 * 进程内共享的 OkHttpClient。
 *
 * - `readTimeout = 0`：SSE 两次数据之间可能间隔很久，中断交给 `Call.cancel()`，而不是读超时；
 * - 连接与写入保留有限超时，避免握手阶段无限等待。
 */
private val sharedHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()
}
