package com.recaitelab.app.ui.prism

import androidx.compose.ui.unit.dp

/**
 * Prism Dock 的几何常量。
 *
 * P1 只把当前实现里已有的数值集中到这里，不引入高度、模糊半径等新视觉参数（留给 P2）。
 */
object PrismDockSpec {

    /** Dock 与屏幕左右边缘的距离。 */
    val HorizontalMargin = 20.dp

    /** Dock 与系统导航栏之间的额外间距。 */
    val BottomGap = 8.dp

    /** Dock 内部 Row 的四周内边距，同时用于计算槽位宽度。 */
    val InnerPadding = 8.dp
}
