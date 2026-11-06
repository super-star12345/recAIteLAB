package com.recaitelab.app.data.repository

import com.recaitelab.app.domain.model.KnowledgeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 知识整理 JSON 解析与兜底回归测试：
 * 纯 JSON、```json 围栏、前后多余文字、缺字段、完全非 JSON 时保存原文。
 */
class KnowledgePayloadParserTest {

    private val now = 1_700_000_000_000L

    private fun entry(raw: String): KnowledgeEntry =
        KnowledgePayloadParser.buildEntry(
            raw = raw,
            payload = KnowledgePayloadParser.parse(raw),
            conversationId = 42L,
            providerId = "provider-x",
            model = "model-x",
            now = now,
        )

    @Test
    fun parsesPlainJson() {
        val raw = """
            {"title":"  标题  ","summary":"  摘要  ","content":"  正文  ","tags":["a","b"]}
        """.trimIndent()

        val entry = entry(raw)

        assertEquals("标题", entry.title)
        assertEquals("摘要", entry.summary)
        assertEquals("正文", entry.content)
        assertEquals(listOf("a", "b"), entry.tags)
        assertEquals(42L, entry.sourceConversationId)
        assertEquals("provider-x", entry.providerId)
        assertEquals("model-x", entry.model)
        assertEquals(now, entry.createdAt)
        assertEquals(now, entry.updatedAt)
    }

    @Test
    fun parsesJsonCodeFence() {
        val raw = """
            ```json
            {"title":"围栏标题","summary":"围栏摘要","content":"围栏正文","tags":["tag"]}
            ```
        """.trimIndent()

        val entry = entry(raw)

        assertEquals("围栏标题", entry.title)
        assertEquals("围栏摘要", entry.summary)
        assertEquals("围栏正文", entry.content)
        assertEquals(listOf("tag"), entry.tags)
    }

    @Test
    fun parsesJsonWithSurroundingText() {
        val raw = """
            好的，以下是整理结果：
            ```json
            {"title":"前后文字","summary":"s","content":"c","tags":[]}
            ```
            希望对你有帮助。
        """.trimIndent()

        val entry = entry(raw)

        assertEquals("前后文字", entry.title)
        assertEquals("s", entry.summary)
        assertEquals("c", entry.content)
    }

    @Test
    fun missingOptionalFieldsUseFallbacks() {
        val raw = """{"title":"只有标题"}"""

        val entry = entry(raw)

        assertEquals("只有标题", entry.title)
        assertEquals(raw.take(100), entry.summary)
        assertEquals(raw, entry.content)
        assertTrue(entry.tags.isEmpty())
    }

    @Test
    fun emptyObjectFallsBackToRawText() {
        val raw = "{}"

        val entry = entry(raw)

        assertEquals("未命名整理", entry.title)
        assertEquals(raw, entry.summary)
        assertEquals(raw, entry.content)
        assertTrue(entry.tags.isEmpty())
    }

    @Test
    fun blankFieldsUseRawFallbacks() {
        val raw = """{"title":"","summary":"","content":"","tags":[]}"""

        val entry = entry(raw)

        assertEquals("未命名整理", entry.title)
        assertEquals(raw.take(100), entry.summary)
        assertEquals(raw, entry.content)
        assertTrue(entry.tags.isEmpty())
    }

    @Test
    fun nonJsonFallsBackToRawOutput() {
        val raw = "这不是 JSON，只是一段普通回答。"

        val entry = entry(raw)

        assertEquals("未命名整理", entry.title)
        assertEquals(raw, entry.summary)
        assertEquals(raw, entry.content)
        assertTrue(entry.tags.isEmpty())
    }

    @Test
    fun malformedJsonFallsBackToRawOutput() {
        val raw = "前缀 {broken json} 后缀"

        val entry = entry(raw)

        assertEquals("未命名整理", entry.title)
        assertEquals(raw, entry.summary)
        assertEquals(raw, entry.content)
    }

    @Test
    fun longRawOutputSummaryIsTruncatedTo100Chars() {
        val raw = "x".repeat(150)

        val entry = entry(raw)

        assertEquals(100, entry.summary.length)
        assertEquals(raw, entry.content)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val raw = """
            {"title":"T","summary":"S","content":"C","tags":["a"],"extra":{"nested":true}}
        """.trimIndent()

        val entry = entry(raw)

        assertEquals("T", entry.title)
        assertEquals("S", entry.summary)
        assertEquals("C", entry.content)
        assertEquals(listOf("a"), entry.tags)
    }
}
