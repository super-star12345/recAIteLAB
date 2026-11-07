package com.recaitelab.app.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.GlassQuality
import com.recaitelab.app.ui.common.MarkdownText
import com.recaitelab.app.ui.common.MarkdownTextStyle
import com.recaitelab.app.ui.prism.PrismTheme
import com.recaitelab.app.ui.prism.prismGlassSurface
import com.recaitelab.app.ui.prism.prismPressFeedback
import dev.chrisbanes.haze.HazeState

/**
 * 对话页的 Prism 视觉组件（仅 PRISM_GLASS 使用）。
 *
 * 只做布局与样式，状态与回调仍由 ChatScreen 传入；气泡不使用背景模糊，
 * 输入区不在内部消费 IME insets（由 ChatScreen 的调用点统一 imePadding）。
 */

@Composable
fun PrismChatTopBar(
    modelLabel: String,
    onOpenPicker: () -> Unit,
    quality: GlassQuality,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .prismGlassSurface(
                quality = quality,
                hazeState = hazeState,
                shape = shapes.large,
            )
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(shapes.listRow)
                .prismPressFeedback(onClick = onOpenPicker)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = modelLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PrismChatBubble(
    message: UiMessage,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes
    val isUser = message.role == UiMessage.ROLE_USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) colors.primaryContainer else colors.surface,
            contentColor = if (isUser) colors.onPrimaryContainer else colors.onSurface,
            shape = shapes.large,
            border = if (isUser) null else BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            when {
                message.content.isEmpty() && message.isStreaming -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary,
                    )
                }

                // 用户消息保持纯文本；助手流式期间也用纯文本，避免每个增量重复解析 Markdown。
                isUser || message.isStreaming -> {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }

                // 助手完成后切 Markdown，沿用对话紧凑标题样式。
                else -> {
                    MarkdownText(
                        markdown = message.content,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MarkdownTextStyle.CHAT,
                    )
                }
            }
        }
    }
}

@Composable
fun PrismChatInputBar(
    inputText: String,
    isGenerating: Boolean,
    isLoadingHistory: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    quality: GlassQuality,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes
    val inputEnabled = !isGenerating && !isLoadingHistory
    val actionEnabled = !isLoadingHistory && (isGenerating || inputText.isNotBlank())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .prismGlassSurface(
                quality = quality,
                hazeState = hazeState,
                shape = shapes.large,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(colors.surface.copy(alpha = 0.82f), shapes.listRow)
                .border(BorderStroke(1.dp, colors.outlineVariant), shapes.listRow)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (inputText.isEmpty()) {
                Text(
                    text = stringResource(R.string.chat_input_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                enabled = inputEnabled,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
            )
        }

        Surface(
            modifier = Modifier
                .size(48.dp)
                .prismPressFeedback(
                    enabled = actionEnabled,
                    onClick = { if (isGenerating) onStop() else onSend() },
                ),
            shape = shapes.pill,
            color = if (actionEnabled) colors.primary else colors.surfaceHigh,
            contentColor = if (actionEnabled) colors.onPrimary else colors.onSurfaceVariant,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isGenerating) {
                        Icons.Filled.Stop
                    } else {
                        Icons.AutoMirrored.Filled.Send
                    },
                    contentDescription = stringResource(
                        if (isGenerating) R.string.chat_stop else R.string.chat_send,
                    ),
                )
            }
        }
    }
}

@Composable
fun PrismModelRow(
    model: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PrismTheme.shapes.listRow)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .prismPressFeedback(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = model,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.chat_model_selected),
                tint = colors.onPrimaryContainer,
            )
        }
    }
}
