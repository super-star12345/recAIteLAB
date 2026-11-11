package com.recaitelab.app.ui.prism

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Prism 动效 token。
 *
 * T1 只定义参数；T2 的预览页转场与按压反馈从这里取参数，
 * 避免把 300ms、30% 位移、10% 暗化等魔法数字散落到页面里。
 */
@Immutable
data class PrismMotion(
    /** 名义转场时长，仅用于文档与降级判断；实际运动由下面的弹簧参数驱动。 */
    val pageTransitionMillis: Int = 300,
    /** 页面转场弹簧：临界阻尼，无回弹。 */
    val pageSpringDamping: Float = 1f,
    val pageSpringStiffness: Float = 400f,
    /** 前页联动后退比例（屏宽 30%）与黑色暗化透明度（10%）。 */
    val pageRecedeFraction: Float = 0.30f,
    val pageScrimAlpha: Float = 0.10f,
    /** 统一按压反馈：轻微缩放 + 透明度变化，弹簧过渡；阻尼略高，避免果冻感。 */
    val pressScale: Float = 0.975f,
    val pressAlpha: Float = 0.96f,
    val pressSpringDamping: Float = 0.90f,
    val pressSpringStiffness: Float = 650f,
    /** Dock 选中指示器：位置/宽度弹簧；快速切换时由同一 Animatable 改道。 */
    val dockIndicatorDamping: Float = 0.90f,
    val dockIndicatorStiffness: Float = 500f,
)

val LocalPrismMotion = staticCompositionLocalOf { PrismMotion() }
