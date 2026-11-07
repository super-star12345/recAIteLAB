package com.recaitelab.app.ui.prism

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.DEFAULT_PRISM_SEED_ARGB
import com.recaitelab.app.domain.model.GlassQuality
import com.recaitelab.app.domain.model.PrismColorStyle
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import kotlinx.coroutines.launch

/**
 * Prism Glass 静态预览 + 转场原型（T1/T2）。
 *
 * - 静态区：四种配色、明暗、连续圆角卡片、列表行、按钮、顶栏与底栏；
 * - 转场区：「转场预览」推入演示详情页，新页右侧进入、前页联动后退 30% 并暗化 10%，
 *   返回对称反向；弹簧参数集中在 [PrismMotion]；
 * - 全部假数据，不接 ViewModel / Repository；正式页面与 Material 导航不变。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrismPreviewScreen(
    initialStyle: PrismColorStyle,
    seedColorArgb: Long,
    initialDark: Boolean,
    glassQuality: GlassQuality,
    onGlassQualityChange: (GlassQuality) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var style by rememberSaveable { mutableStateOf(initialStyle) }
    var dark by rememberSaveable { mutableStateOf(initialDark) }
    var demoVisible by rememberSaveable { mutableStateOf(false) }

    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberAnimationsEnabled()

    PrismTheme(
        style = style,
        seedColor = Color(seedColorArgb.toInt()),
        darkTheme = dark,
    ) {
        val colors = PrismTheme.colors
        val motion = PrismTheme.motion

        fun openDemo() {
            if (demoVisible) return
            demoVisible = true
            if (animationsEnabled) {
                scope.launch {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = motion.pageSpringDamping,
                            stiffness = motion.pageSpringStiffness,
                        ),
                    )
                }
            } else {
                scope.launch { progress.snapTo(1f) }
            }
        }

        fun closeDemo() {
            if (!demoVisible) return
            if (animationsEnabled) {
                scope.launch {
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = motion.pageSpringDamping,
                            stiffness = motion.pageSpringStiffness,
                        ),
                    )
                    demoVisible = false
                }
            } else {
                demoVisible = false
                scope.launch { progress.snapTo(0f) }
            }
        }

        BackHandler(enabled = demoVisible) { closeDemo() }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val pageWidth = constraints.maxWidth.toFloat()

            // 前页：只移动绘制位置，保持组合与滚动状态。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -pageWidth * motion.pageRecedeFraction * progress.value
                    },
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(text = stringResource(R.string.prism_preview_title)) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.prism_preview_back),
                                    )
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    PreviewContent(
                        style = style,
                        dark = dark,
                        onStyleChange = { style = it },
                        onDarkChange = { dark = it },
                        onOpenTransition = ::openDemo,
                        glassQuality = glassQuality,
                        onGlassQualityChange = onGlassQualityChange,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }

            // 前页暗化：始终存在，alpha 由进度驱动，避免逐帧重组。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = motion.pageScrimAlpha * progress.value }
                    .background(Color.Black),
            )

            // 详情页：从右侧推入；返回时反向滑出。
            if (demoVisible) {
                PrismDemoDetailScreen(
                    onBack = ::closeDemo,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = pageWidth * (1f - progress.value)
                        },
                )
            }
        }
    }
}

@Composable
private fun PreviewContent(
    style: PrismColorStyle,
    dark: Boolean,
    onStyleChange: (PrismColorStyle) -> Unit,
    onDarkChange: (Boolean) -> Unit,
    onOpenTransition: () -> Unit,
    glassQuality: GlassQuality,
    onGlassQualityChange: (GlassQuality) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PreviewControls(
            style = style,
            dark = dark,
            onStyleChange = onStyleChange,
            onDarkChange = onDarkChange,
        )

        TransitionEntry(onClick = onOpenTransition)

        SectionLabel(text = stringResource(R.string.prism_glass_section))
        PrismGlassDemo(
            quality = glassQuality,
            onQualityChange = onGlassQualityChange,
        )

        SectionLabel(text = stringResource(R.string.prism_preview_topbar))
        SampleTopBar()

        SectionLabel(text = stringResource(R.string.prism_preview_card))
        SampleCard()

        SectionLabel(text = stringResource(R.string.prism_preview_list))
        SampleList()

        SectionLabel(text = stringResource(R.string.prism_preview_buttons))
        SampleButtons()

        SectionLabel(text = stringResource(R.string.prism_preview_bottombar))
        SampleBottomBar()

        Text(
            text = stringResource(R.string.prism_preview_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

/** 「转场预览」入口：点击推入演示详情页。 */
@Composable
private fun TransitionEntry(onClick: () -> Unit) {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .prismPressFeedback(onClick = onClick),
        shape = shapes.card,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.prism_preview_transition),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.prism_preview_transition_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PreviewControls(
    style: PrismColorStyle,
    dark: Boolean,
    onStyleChange: (PrismColorStyle) -> Unit,
    onDarkChange: (Boolean) -> Unit,
) {
    val colors = PrismTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.prism_preview_style),
            style = MaterialTheme.typography.titleSmall,
            color = colors.onBackground,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrismColorStyle.entries.forEach { candidate ->
                PrismFilterChip(
                    selected = style == candidate,
                    onClick = { onStyleChange(candidate) },
                    label = stringResource(candidate.styleLabelRes()),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.prism_preview_mode),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onBackground,
            )
            Spacer(modifier = Modifier.width(12.dp))
            PrismFilterChip(
                selected = !dark,
                onClick = { onDarkChange(false) },
                label = stringResource(R.string.prism_preview_light),
            )
            Spacer(modifier = Modifier.width(8.dp))
            PrismFilterChip(
                selected = dark,
                onClick = { onDarkChange(true) },
                label = stringResource(R.string.prism_preview_dark),
            )
        }
    }
}

/**
 * 预览页控件仍使用 M3 FilterChip，但颜色改用 Prism token：
 * 避免预览明暗与全局主题不一致时出现「白底白字」。
 */
@Composable
private fun PrismFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    val colors = PrismTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = colors.onSurfaceVariant,
            selectedContainerColor = colors.primaryContainer,
            selectedLabelColor = colors.onPrimaryContainer,
        ),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = PrismTheme.colors.onSurfaceVariant,
    )
}

@Composable
private fun SampleTopBar() {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.large,
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colors.onSurface,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.knowledge_detail_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.prism_preview_sample_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SampleCard() {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.card,
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.prism_preview_sample_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.history_just_now),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.prism_preview_sample_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrismTag(text = stringResource(R.string.settings_prism_style_tinted))
                PrismTag(text = stringResource(R.string.settings_prism_style_soft))
            }
        }
    }
}

@Composable
private fun PrismTag(text: String) {
    val colors = PrismTheme.colors
    Surface(
        shape = PrismTheme.shapes.pill,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun SampleList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrismListRow(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = stringResource(R.string.tab_chat),
            subtitle = stringResource(R.string.prism_preview_sample_body),
            selected = false,
            onClick = {},
        )
        PrismListRow(
            icon = Icons.Filled.History,
            title = stringResource(R.string.tab_history),
            subtitle = stringResource(R.string.history_just_now),
            selected = true,
            onClick = {},
        )
        PrismListRow(
            icon = Icons.Filled.AutoStories,
            title = stringResource(R.string.tab_knowledge),
            subtitle = stringResource(R.string.tab_settings),
            selected = false,
            onClick = {},
        )
    }
}

@Composable
private fun PrismListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = PrismTheme.colors
    val container = if (selected) colors.secondaryContainer else colors.surface
    val content = if (selected) colors.onSecondaryContainer else colors.onSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .prismPressFeedback(onClick = onClick),
        shape = PrismTheme.shapes.listRow,
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (selected) colors.primary else colors.primaryContainer,
                        shape = PrismTheme.shapes.pill,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) colors.onPrimary else colors.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) content.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = content.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SampleButtons() {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrismButton(
            text = stringResource(R.string.chat_send),
            container = colors.primary,
            content = colors.onPrimary,
            onClick = {},
        )
        PrismButton(
            text = stringResource(R.string.settings_cancel),
            container = colors.secondaryContainer,
            content = colors.onSecondaryContainer,
            onClick = {},
        )
        Surface(
            modifier = Modifier.prismPressFeedback(onClick = {}),
            shape = shapes.pill,
            color = Color.Transparent,
            contentColor = colors.primary,
            border = BorderStroke(1.dp, colors.outline),
        ) {
            Text(
                text = stringResource(R.string.action_retry),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun PrismButton(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.prismPressFeedback(onClick = onClick),
        shape = PrismTheme.shapes.pill,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SampleBottomBar() {
    val colors = PrismTheme.colors
    val items = listOf(
        Icons.AutoMirrored.Filled.Chat to stringResource(R.string.tab_chat),
        Icons.Filled.History to stringResource(R.string.tab_history),
        Icons.Filled.AutoStories to stringResource(R.string.tab_knowledge),
        Icons.Filled.Settings to stringResource(R.string.tab_settings),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PrismTheme.shapes.large,
        color = colors.surfaceHigh,
        contentColor = colors.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEachIndexed { index, (icon, label) ->
                val selected = index == 1
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Surface(
                        shape = PrismTheme.shapes.pill,
                        color = if (selected) colors.primaryContainer else Color.Transparent,
                        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 读取系统动画缩放：关闭动画时转场直接落到终态。 */
@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        scale > 0f
    }
}

@StringRes
private fun PrismColorStyle.styleLabelRes(): Int = when (this) {
    PrismColorStyle.VIVID -> R.string.settings_prism_style_vivid
    PrismColorStyle.TINTED -> R.string.settings_prism_style_tinted
    PrismColorStyle.SOFT -> R.string.settings_prism_style_soft
    PrismColorStyle.MONOCHROME -> R.string.settings_prism_style_monochrome
}

@Preview(showBackground = true)
@Composable
private fun PrismPreviewScreenPreview() {
    RecAIteLABTheme {
        PrismPreviewScreen(
            initialStyle = PrismColorStyle.SOFT,
            seedColorArgb = DEFAULT_PRISM_SEED_ARGB,
            initialDark = false,
            glassQuality = GlassQuality.STANDARD,
            onGlassQualityChange = {},
            onBack = {},
        )
    }
}
