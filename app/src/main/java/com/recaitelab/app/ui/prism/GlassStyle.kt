package com.recaitelab.app.ui.prism

import androidx.compose.ui.unit.dp

/**
 * Prism Standard Glass 的唯一材质参数入口。
 *
 * 目标为「半透明 + 轻磨砂」：10dp 左右的背景模糊、按深浅色分别控制的 tint，
 * 1dp 细边缘高光与轻微阴影。T5 若扩展 Ultra，在保持该基线的同时新增参数，
 * 不要把魔法值散落回页面。
 */
object GlassStyle {
    /** 轻磨砂：8–12dp 起步，真机可继续微调。 */
    val BlurRadius = 10.dp

    val BorderWidth = 1.dp
    val ShadowElevation = 8.dp

    /** tint 的不透明度：浅色稍低、深色稍高，保证文字可读。 */
    const val LightTintAlpha = 0.45f
    const val DarkTintAlpha = 0.55f

    /** 仅 Snapshot Sheet 需要的内容承托底色；顶栏/输入区保持透明。 */
    const val SheetContainerAlpha = 0.55f

    /** 不支持模糊/关闭模糊时，Haze fallback scrim 的不透明度。 */
    const val FallbackAlpha = 0.86f

    /** Dock 选中指示器只做轻量 tint，不单独建立背景采样层。 */
    const val IndicatorTintAlpha = 0.72f

    const val NoiseFactor = 0.03f
}
