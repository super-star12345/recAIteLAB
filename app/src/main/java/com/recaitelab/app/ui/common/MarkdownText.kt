package com.recaitelab.app.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.recaitelab.app.R
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode

/**
 * Markdown 文本的展示场景。
 *
 * - [ARTICLE]：知识详情等长文阅读场景，沿用 Markdown 库默认的文章级标题字号；
 * - [CHAT]：对话页助手气泡，使用更紧凑的标题层级，避免 H1/H2 过大。
 */
enum class MarkdownTextStyle {
    ARTICLE,
    CHAT,
}

/**
 * 可复用的 Compose Markdown 渲染组件。
 *
 * 使用 Mikepenz multiplatform-markdown-renderer 的 Material 3 版本；
 * 颜色/排版跟随 MaterialTheme。代码块替换为自定义组件：显示语言标识、
 * 等宽字体、横向滚动，右上角「复制」按钮把代码写入系统剪贴板。
 * 对话页与知识详情共用本组件，不要各自引入渲染库。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: MarkdownTextStyle = MarkdownTextStyle.ARTICLE,
) {
    // 只覆盖代码围栏渲染，其余组件沿用库默认实现；不额外引入 Markdown 库。
    val components = remember {
        markdownComponents(
            codeFence = { model -> MarkdownCodeFence(model) },
        )
    }
    val typography = when (style) {
        MarkdownTextStyle.ARTICLE -> markdownTypography()
        // 对话紧凑样式：只收紧标题字号/行高，不改全局 Typography 和正文块间距。
        MarkdownTextStyle.CHAT -> markdownTypography(
            h1 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            h2 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            h3 = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            h4 = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            h5 = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            h6 = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
    Markdown(
        content = markdown,
        colors = markdownColor(),
        typography = typography,
        components = components,
        modifier = modifier,
    )
}

/** 代码围栏：顶部语言标识 + 复制按钮，正文等宽字体并支持横向滚动。 */
@Composable
private fun MarkdownCodeFence(
    model: MarkdownComponentModel,
    modifier: Modifier = Modifier,
) {
    val code = remember(model.node, model.content) { model.fenceCode() }
    if (code.isBlank()) return
    val language = remember(model.node, model.content) { model.fenceLanguage() }
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language.ifBlank {
                        stringResource(R.string.markdown_code_language_unknown)
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = { context.copyToClipboard(code) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.markdown_code_copy),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                )
            }
        }
    }
}

/** 从代码围栏 AST 中取语言标识（如 ```kotlin 的 kotlin）；无标识时返回空串。 */
private fun MarkdownComponentModel.fenceLanguage(): String {
    val languageNode = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG) ?: return ""
    return languageNode.getTextInNode(content).toString().trim()
}

/** 从代码围栏 AST 中取代码正文，逻辑与库默认实现保持一致。 */
private fun MarkdownComponentModel.fenceCode(): String {
    val children = node.children
    if (children.size < 3) return ""
    val hasLanguage = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG) != null
    val startIndex = if (hasLanguage && children.size > 3) 3 else 2
    val endIndex = maxOf(children.size - 2, startIndex)
    val startOffset = children[2].startOffset
    val endOffset = children[endIndex].endOffset
    if (startOffset >= endOffset) return ""
    return content.subSequence(startOffset, endOffset).toString().replaceIndent()
}

private fun Context.copyToClipboard(code: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, code))
}

private const val CLIP_LABEL = "recAIteLAB code"

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
