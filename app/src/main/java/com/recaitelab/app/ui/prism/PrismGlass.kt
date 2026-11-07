package com.recaitelab.app.ui.prism

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.GlassQuality
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/*
 * Prism 标准玻璃（STANDARD）参数：集中在此，方便 T4 铺开时复用。
 * 不提供 Ultra 参数——本阶段 Ultra 未实现。
 */
private val GlassBlurRadius = 24.dp
private val GlassBorderWidth = 1.dp
private val GlassShadowElevation = 8.dp
private val GlassContainerAlpha = 0.38f
private val GlassTintAlpha = 0.52f
private val GlassFallbackAlpha = 0.90f
private const val GlassNoiseFactor = 0.06f
private val GlassTopBarHeight = 56.dp
private val GlassBottomBarHeight = 72.dp

/**
 * 标准 Liquid Glass 演示（仅预览页使用）。
 *
 * - 一块可滚动内容作为背景源（`hazeSource`），顶部/底部 Prism 栏叠加其上（`hazeEffect`）；
 * - STANDARD 才启用模糊；OFF / ULTRA 使用不透明表面（ULTRA 不伪造效果）；
 * - Android 11 及以下由 Haze 自动降级为 frost/tint 遮罩，并在此提示；
 * - 玻璃栏位于滚动容器之外，HazeState 与样式均 `remember`，滚动时不会反复创建模糊层。
 */
@Composable
fun PrismGlassDemo(
    quality: GlassQuality,
    onQualityChange: (GlassQuality) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // STANDARD 在低版本仍走 Haze 的 fallback scrim（frost/tint），而不是直接变不透明。
    val useHaze = quality == GlassQuality.STANDARD
    val hazeState = rememberHazeState(blurEnabled = useHaze && blurSupported)
    val hazeStyle = rememberGlassStyle(colors)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassQuality.entries.forEach { candidate ->
                FilterChip(
                    selected = quality == candidate,
                    onClick = { onQualityChange(candidate) },
                    label = { Text(text = stringResource(candidate.glassLabelRes())) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = colors.onSurfaceVariant,
                        selectedContainerColor = colors.primaryContainer,
                        selectedLabelColor = colors.onPrimaryContainer,
                    ),
                )
            }
        }
        Text(
            text = glassHint(quality = quality, blurSupported = blurSupported),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        val topShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
        val bottomShape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(shapes.card)
                .background(colors.background),
        ) {
            GlassBackdrop(
                glassEnabled = useHaze,
                hazeState = hazeState,
                modifier = Modifier.fillMaxSize(),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(GlassTopBarHeight)
                    .glassSurface(
                        glassEnabled = useHaze,
                        hazeState = hazeState,
                        style = hazeStyle,
                        shape = topShape,
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.onSurface,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.prism_glass_topbar),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(GlassBottomBarHeight)
                    .glassSurface(
                        glassEnabled = useHaze,
                        hazeState = hazeState,
                        style = hazeStyle,
                        shape = bottomShape,
                    )
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassBottomItem(Icons.AutoMirrored.Filled.Chat, stringResource(R.string.tab_chat), selected = false)
                GlassBottomItem(Icons.Filled.History, stringResource(R.string.tab_history), selected = true)
                GlassBottomItem(Icons.Filled.AutoStories, stringResource(R.string.tab_knowledge), selected = false)
                GlassBottomItem(Icons.Filled.Settings, stringResource(R.string.tab_settings), selected = false)
            }
        }
    }
}

/** 玻璃 / 不透明表面两种形态；统一 1dp 描边与阴影。 */
@Composable
private fun Modifier.glassSurface(
    glassEnabled: Boolean,
    hazeState: HazeState,
    style: HazeStyle,
    shape: Shape,
): Modifier {
    val colors = PrismTheme.colors
    val highlight = Color.White.copy(alpha = if (colors.isDark) 0.20f else 0.60f)
    return if (glassEnabled) {
        this
            .shadow(elevation = GlassShadowElevation, shape = shape, clip = false)
            .clip(shape)
            .hazeEffect(state = hazeState, style = style)
            .border(GlassBorderWidth, highlight, shape)
    } else {
        this
            .shadow(elevation = 2.dp, shape = shape, clip = false)
            .clip(shape)
            .background(colors.surfaceHigh)
            .border(GlassBorderWidth, colors.outlineVariant, shape)
    }
}

/** 背景源：滚动内容。玻璃关闭时也保留（只是不加 hazeSource）。 */
@Composable
private fun GlassBackdrop(
    glassEnabled: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    Column(
        modifier = modifier
            .then(if (glassEnabled) Modifier.hazeSource(state = hazeState) else Modifier)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = GlassTopBarHeight + 12.dp,
                bottom = GlassBottomBarHeight + 12.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(6) { index ->
            val container = when (index % 3) {
                0 -> colors.primaryContainer
                1 -> colors.secondaryContainer
                else -> colors.surfaceHigh
            }
            val contentColor = when (index % 3) {
                0 -> colors.onPrimaryContainer
                1 -> colors.onSecondaryContainer
                else -> colors.onSurface
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = PrismTheme.shapes.medium,
                color = container,
                contentColor = contentColor,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.prism_glass_backdrop_item, index + 1),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.prism_glass_backdrop_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassBottomItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
) {
    val colors = PrismTheme.colors
    val tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Surface(
            shape = PrismTheme.shapes.pill,
            color = if (selected) colors.primaryContainer else Color.Transparent,
            contentColor = tint,
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
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun rememberGlassStyle(colors: PrismColors): HazeStyle = remember(colors) {
    HazeStyle(
        backgroundColor = colors.surface.copy(alpha = GlassContainerAlpha),
        tints = listOf(HazeTint(colors.surface.copy(alpha = GlassTintAlpha))),
        blurRadius = GlassBlurRadius,
        noiseFactor = GlassNoiseFactor,
        fallbackTint = HazeTint(colors.surface.copy(alpha = GlassFallbackAlpha)),
    )
}

@Composable
private fun glassHint(quality: GlassQuality, blurSupported: Boolean): String = when {
    quality == GlassQuality.ULTRA -> stringResource(R.string.prism_glass_ultra_unimplemented)
    quality == GlassQuality.OFF -> stringResource(R.string.prism_glass_off_hint)
    !blurSupported -> stringResource(R.string.prism_glass_fallback_hint)
    else -> stringResource(R.string.prism_glass_standard_hint)
}

@androidx.annotation.StringRes
private fun GlassQuality.glassLabelRes(): Int = when (this) {
    GlassQuality.OFF -> R.string.settings_glass_off
    GlassQuality.STANDARD -> R.string.settings_glass_standard
    GlassQuality.ULTRA -> R.string.settings_glass_ultra
}
