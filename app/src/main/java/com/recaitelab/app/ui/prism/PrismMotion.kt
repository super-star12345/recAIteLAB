package com.recaitelab.app.ui.prism

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Prism 动效 token。
 *
 * T1 只定义参数、不启用动画；T2 的页面转场与按压反馈直接读取这里，
 * 避免到时候把 300ms、临界阻尼、0.8 缩放等魔法数字散落在页面里。
 */
@Immutable
data class PrismMotion(
    /** 临界阻尼弹簧：dampingRatio = 1 时无回弹。 */
    val springDamping: Float = 1f,
    val springStiffness: Float = 320f,
    /** 按压时的缩放比例。 */
    val pressScale: Float = 0.97f,
    val pressDurationMillis: Int = 120,
    /** push 转场时长与旧页联动参数。 */
    val pageTransitionMillis: Int = 300,
    val pageRecedeFraction: Float = 0.30f,
    val pageScrimAlpha: Float = 0.10f,
)

val LocalPrismMotion = staticCompositionLocalOf { PrismMotion() }
