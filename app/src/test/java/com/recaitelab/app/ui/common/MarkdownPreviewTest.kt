package com.recaitelab.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownPreviewTest {

    @Test
    fun `strips heading bold list and quote markers`() {
        val source = """
            ## 标题
            - **加粗**条目
            > 引用
        """.trimIndent()

        assertEquals("标题 加粗条目 引用", markdownToPreview(source))
    }

    @Test
    fun `removes fenced code blocks`() {
        val source = "说明\n```kotlin\nval a = 1\n```\n结束"

        assertEquals("说明 结束", markdownToPreview(source))
    }

    @Test
    fun `keeps link and image labels`() {
        val source = "[文档](https://example.com) ![图](a.png)"

        assertEquals("文档 图", markdownToPreview(source))
    }

    @Test
    fun `collapses whitespace and newlines`() {
        assertEquals("第一行 第二行", markdownToPreview("第一行\n\n   第二行   "))
    }

    @Test
    fun `truncates to max length`() {
        assertEquals("12345", markdownToPreview("1234567890", maxLength = 5))
    }

    @Test
    fun `blank input yields empty preview`() {
        assertEquals("", markdownToPreview("   \n "))
    }
}
