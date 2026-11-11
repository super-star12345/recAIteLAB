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
import androidx.compose.ui.graphics.DefaultShadowColor
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

/**
 * 玻璃表面：
 * - STANDARD：背景模糊 + 半透明 tint + 1dp 高光 + 阴影，低版本由 Haze 自动 scrim 降级；
 * - ULTRA：默认仍是历史的不透明 surfaceHigh，只有显式传 `glassInUltra = true` 的表面
 *   （当前仅 Prism Dock）才走玻璃分支，避免波及 ULTRA 下的顶栏 / 输入区 / Sheet；
 * - OFF：不透明 surfaceHigh + 1dp 描边 + 轻阴影。
 *
 * 所有材质参数都带有与历史行为一致的默认值；只有显式传参的调用点（如 ULTRA Dock）
 * 才会偏离 [GlassStyle]。调用方负责把 `hazeSource` 放在该表面背后的内容上；
 * 本 Modifier 不创建模糊层缓存，HazeState 由页面 `remember`，滚动时不会重建。
 */
@Composable
fun Modifier.prismGlassSurface(
    quality: GlassQuality,
    hazeState: HazeState,
    shape: Shape,
    shadowElevation: Dp = GlassStyle.ShadowElevation,
    hazeBackground: Color = Color.Transparent,
    showBorder: Boolean = true,
    shadowAmbientColor: Color = DefaultShadowColor,
    shadowSpotColor: Color = DefaultShadowColor,
    blurRadius: Dp = GlassStyle.BlurRadius,
    tintAlphaLight: Float = GlassStyle.LightTintAlpha,
    tintAlphaDark: Float = GlassStyle.DarkTintAlpha,
    borderWidth: Dp = GlassStyle.BorderWidth,
    glassInUltra: Boolean = false,
): Modifier {
    val colors = PrismTheme.colors
    val useGlass = quality == GlassQuality.STANDARD ||
        (quality == GlassQuality.ULTRA && glassInUltra)
    val highlight = Color.White.copy(alpha = if (colors.isDark) 0.20f else 0.60f)

    return if (useGlass) {
        val style = remember(colors, hazeBackground, blurRadius, tintAlphaLight, tintAlphaDark) {
            val tintAlpha = if (colors.isDark) tintAlphaDark else tintAlphaLight
            HazeStyle(
                // 顶栏/输入区默认不加 surface 白底；Sheet 显式传承托色。
                backgroundColor = hazeBackground,
                // tint 跟随页面底色而不是 surface（surface 更白，会在空内容区形成白色底板）。
                tints = listOf(HazeTint(colors.background.copy(alpha = tintAlpha))),
                blurRadius = blurRadius,
                noiseFactor = GlassStyle.NoiseFactor,
                fallbackTint = HazeTint(colors.background.copy(alpha = GlassStyle.FallbackAlpha)),
            )
        }
        this
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                clip = false,
                ambientColor = shadowAmbientColor,
                spotColor = shadowSpotColor,
            )
            .clip(shape)
            .hazeEffect(state = hazeState, style = style)
            .then(
                if (showBorder) {
                    Modifier.border(borderWidth, highlight, shape)
                } else {
                    Modifier
                },
            )
    } else {
        this
            .shadow(elevation = 2.dp, shape = shape, clip = false)
            .clip(shape)
            .background(colors.surfaceHigh)
            .border(GlassStyle.BorderWidth, colors.outlineVariant, shape)
    }
}
