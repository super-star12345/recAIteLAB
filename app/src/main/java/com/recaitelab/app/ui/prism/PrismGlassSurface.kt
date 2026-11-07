package com.recaitelab.app.ui.prism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recaitelab.app.domain.model.GlassQuality
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** 当前玻璃质量档位；由 RecAIteLABTheme 统一提供。 */
val LocalGlassQuality = staticCompositionLocalOf { GlassQuality.STANDARD }

/*
 * 标准玻璃共享参数；预览页与正式页面（底栏 / 顶栏 / Sheet）复用同一套。
 * Ultra 不提供参数——本阶段仍未实现。
 */
internal val PrismGlassBlurRadius = 24.dp
internal val PrismGlassBorderWidth = 1.dp
internal const val PrismGlassContainerAlpha = 0.30f
internal const val PrismGlassTintAlpha = 0.42f
internal const val PrismGlassFallbackAlpha = 0.86f
internal const val PrismGlassNoiseFactor = 0.04f

/**
 * 标准玻璃表面：
 * - STANDARD：Haze 背景模糊 + tint + 1dp 高光 + 阴影；低版本由 Haze 自动 scrim 降级；
 * - OFF / ULTRA（未实现）：不透明 surfaceHigh + 1dp 描边 + 轻阴影。
 *
 * 调用方负责把 `hazeSource` 放在该表面背后的内容上；本 Modifier 不创建模糊层缓存，
 * HazeState 由页面 `remember`，滚动时不会重建。
 */
@Composable
fun Modifier.prismGlassSurface(
    quality: GlassQuality,
    hazeState: HazeState,
    shape: Shape,
    shadowElevation: Dp = 8.dp,
): Modifier {
    val colors = PrismTheme.colors
    val useGlass = quality == GlassQuality.STANDARD
    val highlight = Color.White.copy(alpha = if (colors.isDark) 0.20f else 0.60f)

    return if (useGlass) {
        val style = remember(colors) {
            HazeStyle(
                backgroundColor = colors.surface.copy(alpha = PrismGlassContainerAlpha),
                tints = listOf(HazeTint(colors.surface.copy(alpha = PrismGlassTintAlpha))),
                blurRadius = PrismGlassBlurRadius,
                noiseFactor = PrismGlassNoiseFactor,
                fallbackTint = HazeTint(colors.surface.copy(alpha = PrismGlassFallbackAlpha)),
            )
        }
        this
            .shadow(elevation = shadowElevation, shape = shape, clip = false)
            .clip(shape)
            .hazeEffect(state = hazeState, style = style)
            .border(PrismGlassBorderWidth, highlight, shape)
    } else {
        this
            .shadow(elevation = 2.dp, shape = shape, clip = false)
            .clip(shape)
            .background(colors.surfaceHigh)
            .border(PrismGlassBorderWidth, colors.outlineVariant, shape)
    }
}