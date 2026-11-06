package com.recaitelab.app.data.remote

import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SSE 事件读取回归测试：
 * 空行分隔、多行 data 合并、注释/非 data 字段忽略、流末尾兜底。
 */
class SseEventReaderTest {

    private fun reader(text: String): SseEventReader =
        SseEventReader(Buffer().writeUtf8(text))

    @Test
    fun joinsMultipleDataLinesWithNewline() {
        val reader = reader("data: first\ndata: second\n\n")

        assertEquals("first\nsecond", reader.nextEventData())
        assertNull(reader.nextEventData())
    }

    @Test
    fun blankLineSeparatesEvents() {
        val reader = reader("data: first\n\ndata: second\n\n")

        assertEquals("first", reader.nextEventData())
        assertEquals("second", reader.nextEventData())
        assertNull(reader.nextEventData())
    }

    @Test
    fun ignoresCommentsAndNonDataFields() {
        val reader = reader(
            ": heartbeat\n" +
                "event: message\n" +
                "id: 1\n" +
                "retry: 1000\n" +
                "data: payload\n\n",
        )

        assertEquals("payload", reader.nextEventData())
        assertNull(reader.nextEventData())
    }

    @Test
    fun returnsLastEventWithoutTrailingBlankLine() {
        val reader = reader("data: tail")

        assertEquals("tail", reader.nextEventData())
        assertNull(reader.nextEventData())
    }

    @Test
    fun skipsEmptyDataLines() {
        val reader = reader("data:\n\ndata: next\n\n")

        assertEquals("next", reader.nextEventData())
        assertNull(reader.nextEventData())
    }

    @Test
    fun trimsLeadingSpacesAfterDataField() {
        val reader = reader("data:   spaced\n\n")

        assertEquals("spaced", reader.nextEventData())
    }

    @Test
    fun handlesCrLfLineEndings() {
        val reader = reader("data: one\r\n\r\ndata: two\r\n\r\n")

        assertEquals("one", reader.nextEventData())
        assertEquals("two", reader.nextEventData())
        assertNull(reader.nextEventData())
    }
}
