package com.recaitelab.app.ui.common

/**
 * 把 Markdown 文本压成适合列表预览的单行纯文本。
 *
 * 历史记录等列表只需要「内容概要」，直接展示原始 Markdown 会出现 `**加粗**`、
 * `###`、``` 等标记。这里做最小清洗：去掉结构/强调标记、保留正文与链接文字，
 * 最后合并空白并截断。纯函数，便于单测。
 *
 * @param markdown 原始 Markdown 文本
 * @param maxLength 预览最大字符数（默认 80）
 */
fun markdownToPreview(markdown: String, maxLength: Int = 80): String {
    if (markdown.isBlank() || maxLength <= 0) return ""

    var text = markdown
    text = FENCED_CODE.replace(text, " ")
    text = FENCE_MARKER.replace(text, " ")
    text = LINK_OR_IMAGE.replace(text, "$1")
    text = HEADING.replace(text, "")
    text = HORIZONTAL_RULE.replace(text, " ")
    text = BLOCKQUOTE.replace(text, "")
    text = LIST_MARKER.replace(text, "")
    text = INLINE_CODE.replace(text, "$1")
    text = EMPHASIS.replace(text, "")
    text = WHITESPACE.replace(text, " ").trim()

    return if (text.length <= maxLength) text else text.take(maxLength).trimEnd()
}

/** 整段代码围栏（含内容）直接丢弃，避免预览里出现代码。 */
private val FENCED_CODE = Regex("```[\\s\\S]*?```")

/** 未闭合的围栏只去掉标记本身。 */
private val FENCE_MARKER = Regex("```")

/** `[文字](链接)` / `![描述](图片)` 只保留文字部分。 */
private val LINK_OR_IMAGE = Regex("!?\\[([^\\]]*)]\\([^)]*\\)")

private val HEADING = Regex("(?m)^[ \\t]*#{1,6}[ \\t]*")

private val HORIZONTAL_RULE = Regex("(?m)^[ \\t]*(?:[-*_][ \\t]*){3,}$")

private val BLOCKQUOTE = Regex("(?m)^[ \\t]*>[ \\t]?")

private val LIST_MARKER = Regex("(?m)^[ \\t]*(?:[-*+]|\\d+[.)])[ \\t]+")

private val INLINE_CODE = Regex("`([^`]*)`")

private val EMPHASIS = Regex("[*_~]")

private val WHITESPACE = Regex("\\s+")
