package com.recaitelab.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.recaitelab.app.ui.theme.RecAIteLABTheme

/**
 * 可复用的 Compose Markdown 渲染组件。
 *
 * 使用 Mikepenz multiplatform-markdown-renderer 的 Material 3 版本；
 * 颜色/排版跟随 MaterialTheme，代码块默认 Monospace。
 * 后续对话页直接复用本组件，不要各自引入渲染库。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    Markdown(
        content = markdown,
        colors = markdownColor(),
        typography = markdownTypography(),
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun MarkdownTextPreview() {
    RecAIteLABTheme {
        MarkdownText(
            markdown = """
                ## 示例标题

                正文支持 **粗体**、`行内代码` 和列表：

                - 第一项
                - 第二项

                ```kotlin
                fun main() = println("hello")
                ```
            """.trimIndent(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
