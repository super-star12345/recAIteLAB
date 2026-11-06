package com.recaitelab.app.data.remote

import kotlinx.serialization.SerializationException
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * SSE 增量解析回归测试：
 * 多事件顺序、空行分隔、[DONE] / finish_reason 终止、delta.content 缺失、非法 JSON。
 */
class SseChatStreamParserTest {

    private fun read(vararg events: String): List<String> {
        val buffer = Buffer()
        events.forEach { event ->
            buffer.writeUtf8("data: ")
            buffer.writeUtf8(event)
            buffer.writeUtf8("\n\n")
        }
        val deltas = mutableListOf<String>()
        SseChatStreamParser.read(buffer) { delta ->
            deltas += delta
            true
        }
        return deltas
    }

    private fun content(text: String): String =
        """{"choices":[{"delta":{"content":"$text"}}]}"""

    @Test
    fun emitsContentEventsInOrder() {
        val deltas = read(content("Hel"), content("lo"))

        assertEquals(listOf("Hel", "lo"), deltas)
    }

    @Test
    fun stopsAtDoneMarker() {
        val deltas = read(content("A"), "[DONE]", content("B"))

        assertEquals(listOf("A"), deltas)
    }

    @Test
    fun ignoresMissingDeltaContent() {
        val roleOnly =
            """{"choices":[{"delta":{"role":"assistant"}}]}"""
        val emptyContent =
            """{"choices":[{"delta":{"content":""}}]}"""

        val deltas = read(roleOnly, emptyContent, content("Hi"))

        assertEquals(listOf("Hi"), deltas)
    }

    @Test
    fun stopsAtFinishReason() {
        val finish =
            """{"choices":[{"delta":{},"finish_reason":"stop"}]}"""

        val deltas = read(content("A"), finish, content("B"))

        assertEquals(listOf("A"), deltas)
    }

    @Test
    fun malformedJsonThrowsSerializationException() {
        assertThrows(SerializationException::class.java) {
            read("{not-json}")
        }
    }

    @Test
    fun emptyStreamEmitsNothing() {
        assertEquals(emptyList<String>(), read())
    }

    @Test
    fun emitsNothingWhenChoicesAreEmpty() {
        val deltas = read(
            """{"choices":[]}""",
            content("ok"),
        )

        assertEquals(listOf("ok"), deltas)
    }
}
