package com.recaitelab.app.data.remote

import com.recaitelab.app.domain.model.ChatCompletionChunk
import com.recaitelab.app.domain.model.openAiJson
import kotlinx.serialization.decodeFromString
import okio.BufferedSource

/**
 * 把 SSE 事件负载解析成增量文本。
 *
 * 纯解析逻辑，不持有网络资源：
 * - `[DONE]` 或 `finish_reason` 终止；
 * - `delta.content` 缺失/为空时跳过该事件；
 * - 事件 JSON 非法时抛出 `SerializationException`，由调用方收敛为错误。
 */
internal object SseChatStreamParser {

    fun read(source: BufferedSource, emit: (String) -> Boolean) {
        val reader = SseEventReader(source)
        while (true) {
            val data = reader.nextEventData() ?: return
            val payload = data.trim()
            if (payload == DONE_MARKER) return
            val choice = openAiJson.decodeFromString<ChatCompletionChunk>(payload)
                .choices
                .firstOrNull()
            val delta = choice?.delta?.content
            if (!delta.isNullOrEmpty() && !emit(delta)) return
            if (choice?.finishReason != null) return
        }
    }

    private const val DONE_MARKER = "[DONE]"
}
