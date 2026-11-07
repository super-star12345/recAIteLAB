package com.recaitelab.app.ui.prism

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.recaitelab.app.domain.model.PrismColorStyle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Prism 设计系统色板。
 *
 * 由单个种子色派生 4 种风格 × 明暗两套；不依赖 Material 3 的 tonal palette，
 * 也不引入新依赖。纯 Kotlin 颜色运算（HSL），便于后续抽成设计 token。
 *
 * 可读性约定：
 * - 正文 `onBackground` 与 `background` 明度差 ≥ 0.75；
 * - `onSurfaceVariant` 在浅色/深色下分别取 0.38 / 0.72 明度，保证 ≥ 4.5:1；
 * - 强调色上的文字按相对亮度自动选黑/白，避免低对比。
 */
@Immutable
data class PrismColors(
    val style: PrismColorStyle,
    val isDark: Boolean,
    val seed: Color,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
) {
    companion object {

        /** 由种子色与风格生成色板；同一组入参结果稳定。 */
        fun from(seed: Color, style: PrismColorStyle, dark: Boolean): PrismColors {
            val hsl = seed.toHsl()
            val hue = hsl[0]
            val seedSat = hsl[1]

            val accentSat = when (style) {
                PrismColorStyle.VIVID, PrismColorStyle.TINTED -> seedSat
                PrismColorStyle.SOFT -> seedSat * 0.50f
                PrismColorStyle.MONOCHROME -> 0f
            }

            return if (dark) {
                darkPalette(seed, style, hue, seedSat, accentSat)
            } else {
                lightPalette(seed, style, hue, seedSat, accentSat)
            }
        }

        private fun lightPalette(
            seed: Color,
            style: PrismColorStyle,
            hue: Float,
            seedSat: Float,
            accentSat: Float,
        ): PrismColors {
            fun color(sat: Float, light: Float): Color = colorOf(hue, sat, light)

            val backgroundSat = when (style) {
                PrismColorStyle.VIVID -> seedSat * 0.05f
                PrismColorStyle.TINTED -> seedSat * 0.30f
                PrismColorStyle.SOFT -> seedSat * 0.10f
                PrismColorStyle.MONOCHROME -> 0f
            }
            val background = color(backgroundSat, 0.970f)
            val surface = color(backgroundSat * 0.70f, 0.992f)
            val surfaceHigh = color(backgroundSat * 0.85f, 0.950f)
            val primaryLight = when (style) {
                PrismColorStyle.VIVID -> 0.38f
                PrismColorStyle.TINTED -> 0.40f
                PrismColorStyle.SOFT -> 0.44f
                PrismColorStyle.MONOCHROME -> 0.30f
            }
            val primary = color(accentSat, primaryLight)
            val primaryContainer = color(accentSat * 0.30f, 0.900f)
            val secondaryContainer = color(accentSat * 0.22f, 0.930f)
            val onBackground = color(seedSat * 0.06f, 0.14f)

            return PrismColors(
                style = style,
                isDark = false,
                seed = seed,
                background = background,
                surface = surface,
                surfaceHigh = surfaceHigh,
                onBackground = onBackground,
                onSurface = onBackground,
                onSurfaceVariant = color(seedSat * 0.10f, 0.38f),
                primary = primary,
                onPrimary = readableOn(primary),
                primaryContainer = primaryContainer,
                onPrimaryContainer = color(accentSat * 0.55f, 0.16f),
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = color(accentSat * 0.50f, 0.20f),
                outline = color(seedSat * 0.10f, 0.55f),
                outlineVariant = color(seedSat * 0.12f, 0.84f),
                error = Color(0xFFBA1A1A),
                onError = Color.White,
            )
        }

        private fun darkPalette(
            seed: Color,
            style: PrismColorStyle,
            hue: Float,
            seedSat: Float,
            accentSat: Float,
        ): PrismColors {
            fun color(sat: Float, light: Float): Color = colorOf(hue, sat, light)

            val backgroundSat = when (style) {
                PrismColorStyle.VIVID -> seedSat * 0.06f
                PrismColorStyle.TINTED -> seedSat * 0.25f
                PrismColorStyle.SOFT -> seedSat * 0.10f
                PrismColorStyle.MONOCHROME -> 0f
            }
            val background = color(backgroundSat, 0.100f)
            val surface = color(backgroundSat * 0.85f, 0.135f)
            val surfaceHigh = color(backgroundSat * 0.95f, 0.175f)
            val primaryLight = when (style) {
                PrismColorStyle.VIVID -> 0.72f
                PrismColorStyle.TINTED -> 0.74f
                PrismColorStyle.SOFT -> 0.70f
                PrismColorStyle.MONOCHROME -> 0.85f
            }
            val primary = color(accentSat * 0.90f, primaryLight)
            val primaryContainer = color(accentSat * 0.45f, 0.240f)
            val secondaryContainer = color(accentSat * 0.30f, 0.220f)
            val onBackground = color(seedSat * 0.04f, 0.93f)

            return PrismColors(
                style = style,
                isDark = true,
                seed = seed,
                background = background,
                surface = surface,
                surfaceHigh = surfaceHigh,
                onBackground = onBackground,
                onSurface = onBackground,
                onSurfaceVariant = color(seedSat * 0.08f, 0.72f),
                primary = primary,
                onPrimary = readableOn(primary),
                primaryContainer = primaryContainer,
                onPrimaryContainer = color(accentSat * 0.25f, 0.92f),
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = color(accentSat * 0.20f, 0.90f),
                outline = color(seedSat * 0.08f, 0.62f),
                outlineVariant = color(seedSat * 0.10f, 0.30f),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
            )
        }
    }
}

/** 设置页与预览页可选的种子色。 */
val PrismSeedPresets: List<Color> = listOf(    Color(0xFF006A6A), // 青绿（默认）
    Color(0xFF2E5FA3), // 商务蓝
    Color(0xFF3A6B35), // 护眼绿
    Color(0xFF7B4FA3), // 紫
    Color(0xFFB4552D), // 橙
    Color(0xFF4A4A4A), // 中性灰
)

/** HSL：hue 0..360，sat/light 0..1。 */
private fun Color.toHsl(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val light = (max + min) / 2f
    if (max == min) return floatArrayOf(0f, 0f, light)

    val delta = max - min
    val saturation = if (light > 0.5f) {
        delta / (2f - max - min)
    } else {
        delta / (max + min)
    }
    val hue = when (max) {
        r -> ((g - b) / delta + if (g < b) 6f else 0f)
        g -> ((b - r) / delta + 2f)
        else -> ((r - g) / delta + 4f)
    } / 6f
    return floatArrayOf(hue * 360f, saturation, light)
}

private fun colorOf(hue: Float, saturation: Float, light: Float): Color {
    val s = saturation.coerceIn(0f, 1f)
    val l = light.coerceIn(0f, 1f)
    if (s <= 0f) return Color(l, l, l)

    val h = ((hue % 360f) + 360f) % 360f / 360f
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun channel(t0: Float): Float {
        var t = t0
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 0.5f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }

    return Color(
        red = channel(h + 1f / 3f),
        green = channel(h),
        blue = channel(h - 1f / 3f),
    )
}

/** 相对亮度（WCAG）。 */
private fun luminance(color: Color): Float {
    fun linear(value: Float): Float =
        if (value <= 0.03928f) value / 12.92f else ((value + 0.055f) / 1.055f).pow(2.4f)

    return 0.2126f * linear(color.red) +
        0.7152f * linear(color.green) +
        0.0722f * linear(color.blue)
}

/** 在给定背景上选择对比度更高的前景色（白或近黑）。 */
private fun readableOn(background: Color): Color {
    val backgroundLuminance = luminance(background)
    val whiteContrast = 1.05f / (backgroundLuminance + 0.05f)
    val blackContrast = (backgroundLuminance + 0.05f) / 0.05f
    return if (whiteContrast >= blackContrast) Color.White else Color(0xFF141414)
}
