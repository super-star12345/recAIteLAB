package com.recaitelab.app.ui.prism

import androidx.compose.ui.unit.dp

/**
 * Prism Dock 的几何与材质常量。
 *
 * P2 起 Dock 使用固定的 64dp 胶囊高度与 56dp 指示器；材质参数只作用于 ULTRA 档，
 * STANDARD/OFF 继续走 [GlassStyle] 的默认值，避免历史参数漂移。
 *
 * P3.5 起根容器抬高到 [LayoutHeight]，给拖动时 76dp 的 morph 指示器留出上方 headroom；
 * 玻璃胶囊仍为 [Height]，视觉位置不变。
 */
object PrismDockSpec {

    /** Dock 根容器高度：玻璃胶囊 64dp + 拖动 morph 指示器的 16dp headroom。 */
    val LayoutHeight = 80.dp

    /** Dock 胶囊高度，指示器静止时垂直居中于其中。 */
    val Height = 64.dp

    /** Dock 与屏幕左右边缘的距离。 */
    val HorizontalMargin = 20.dp

    /** Dock 与系统导航栏之间的额外间距。 */
    val BottomGap = 8.dp

    /** Dock 内部 Row 的四周内边距，同时用于计算槽位宽度。 */
    val InnerPadding = 4.dp

    /** 选中指示器高度；垂直居中，上下各留 [IndicatorInset]。 */
    val IndicatorHeight = 56.dp

    /** 指示器相对胶囊上下边缘的内缩量。 */
    val IndicatorInset = 4.dp

    /** 拖动 morph 时指示器目标高度：底边不动，只向上生长。 */
    val MorphHeight = 76.dp

    /** 拖动 morph 时指示器最大宽度相对槽宽的倍数。 */
    const val MorphWidthScale = 1.04f

    /** 图标边长；外层另有 32dp 的图标盒以稳定文字基线。 */
    val IconSize = 24.dp

    /** ULTRA 档的玻璃模糊半径：比 STANDARD 更薄，保留背景可读性。 */
    val BlurRadius = 4.dp

    /** ULTRA 浅色 tint 不透明度。 */
    const val LightTintAlpha = 0.40f

    /** ULTRA 深色 tint 不透明度。 */
    const val DarkTintAlpha = 0.50f

    /** ULTRA 选中指示器的 tint 强度。 */
    const val IndicatorTintAlpha = 0.55f
}
