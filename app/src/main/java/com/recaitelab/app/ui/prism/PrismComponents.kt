package com.recaitelab.app.ui.prism

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Prism 正式页面可复用的静态组件。
 *
 * 全部读取 [PrismTheme] token，带统一边缘/阴影/按压反馈；只用于 PRISM_GLASS 模式，
 * 不改变 Material 3 组件，也不承载任何业务逻辑。
 */

@Composable
fun PrismCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = PrismTheme.colors
    val clickModifier = if (onClick != null) {
        Modifier.prismPressFeedback(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier
    }
    Surface(
        modifier = modifier.then(clickModifier),
        shape = PrismTheme.shapes.card,
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(content = content)
    }
}

@Composable
fun PrismListRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .prismPressFeedback(onClick = onClick),
        shape = PrismTheme.shapes.listRow,
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.primaryContainer, PrismTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

enum class PrismButtonStyle { FILLED, TONAL, OUTLINED, DANGER }

@Composable
fun PrismButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PrismButtonStyle = PrismButtonStyle.FILLED,
    leadingIcon: ImageVector? = null,
) {
    val colors = PrismTheme.colors
    val container = when (style) {
        PrismButtonStyle.FILLED -> colors.primary
        PrismButtonStyle.TONAL -> colors.secondaryContainer
        PrismButtonStyle.DANGER -> colors.error
        PrismButtonStyle.OUTLINED -> Color.Transparent
    }
    val content = when (style) {
        PrismButtonStyle.FILLED -> colors.onPrimary
        PrismButtonStyle.TONAL -> colors.onSecondaryContainer
        PrismButtonStyle.DANGER -> colors.onError
        PrismButtonStyle.OUTLINED -> colors.primary
    }
    Surface(
        modifier = modifier.prismPressFeedback(onClick = onClick),
        shape = PrismTheme.shapes.pill,
        color = container,
        contentColor = content,
        border = if (style == PrismButtonStyle.OUTLINED) {
            BorderStroke(1.dp, colors.outline)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun PrismChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    Surface(
        modifier = modifier.prismPressFeedback(onClick = onClick),
        shape = PrismTheme.shapes.pill,
        color = if (selected) colors.primaryContainer else colors.surface,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Composable
fun PrismTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = PrismTheme.colors
    Surface(modifier = modifier.fillMaxWidth(), color = colors.background) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                navigationIcon()
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            actions()
        }
    }
}

@Composable
fun PrismFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    Surface(
        modifier = modifier
            .size(56.dp)
            .prismPressFeedback(onClick = onClick),
        shape = PrismTheme.shapes.pill,
        color = colors.primary,
        contentColor = colors.onPrimary,
        shadowElevation = 6.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun PrismEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    val colors = PrismTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(colors.primaryContainer, PrismTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = colors.onPrimaryContainer,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 标签胶囊；知识列表/详情共用。 */
@Composable
fun PrismTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    Surface(
        modifier = modifier,
        shape = PrismTheme.shapes.pill,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

/** 无底色的图标按钮，带 Prism 按压反馈；用于顶栏返回、卡片删除等。 */
@Composable
fun PrismIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PrismTheme.colors.onSurfaceVariant,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .prismPressFeedback(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = tint,
        )
    }
}

/** 胶囊搜索框，替换 M3 OutlinedTextField 的 Prism 样式；行为与原来一致。 */
@Composable
fun PrismSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    clearDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PrismTheme.shapes.pill,
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colors.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                )
            }
            if (value.isNotEmpty()) {
                PrismIconButton(
                    icon = Icons.Filled.Clear,
                    contentDescription = clearDescription,
                    onClick = { onValueChange("") },
                    tint = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** Prism 确认弹窗：删除知识条目等；默认使用错误色确认按钮。 */
@Composable
fun PrismConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    val colors = PrismTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        PrismCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PrismButton(
                        text = dismissText,
                        onClick = onDismiss,
                        style = PrismButtonStyle.OUTLINED,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PrismButton(
                        text = confirmText,
                        onClick = onConfirm,
                        style = if (destructive) {
                            PrismButtonStyle.DANGER
                        } else {
                            PrismButtonStyle.FILLED
                        },
                    )
                }
            }
        }
    }
}

/** 供页面内联小标题使用（如设置分组）。 */
@Composable
fun PrismSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PrismTheme.colors.onSurfaceVariant,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}
