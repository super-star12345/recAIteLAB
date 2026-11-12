package com.recaitelab.app.ui.prism

import kotlin.math.floor

/**
 * Dock 指示器与拖动的纯几何计算，不依赖 Compose。
 *
 * [slotWidth] / [slotIndexAt] 自 P1 起由单测覆盖；P3 起 UI 也直接复用，
 * 拖动、RTL 与边界 clamp 全部走这里的函数，保证可测。
 *
 * P4 起指示器 morph 改为 graphicsLayer 缩放，布局几何仍只有槽宽与水平位置。
 */

/**
 * 单个槽位的宽度：Dock 实测宽度扣除左右内边距后按 [count] 等分。
 *
 * 宽度或数量非法时返回 0，调用方据此跳过指示器绘制。
 */
fun slotWidth(dockWidthPx: Float, count: Int, paddingPx: Float): Float {
    if (dockWidthPx <= 0f || count <= 0) return 0f
    val contentWidthPx = dockWidthPx - paddingPx * 2f
    return if (contentWidthPx > 0f) contentWidthPx / count else 0f
}

/**
 * 槽位左边缘的物理 x 坐标（LTR 从左侧数，RTL 从右侧数）。
 *
 * 宽度/数量非法时返回 0，调用方在未测量阶段不绘制指示器；
 * [index] 越界会被 clamp 到合法槽位。
 */
fun indicatorLeftPx(
    dockWidthPx: Float,
    count: Int,
    paddingPx: Float,
    index: Int,
    rtl: Boolean,
): Float {
    val widthPx = slotWidth(dockWidthPx, count, paddingPx)
    if (widthPx <= 0f) return 0f
    val clampedIndex = index.coerceIn(0, count - 1)
    return if (rtl) {
        dockWidthPx - paddingPx - (clampedIndex + 1) * widthPx
    } else {
        paddingPx + clampedIndex * widthPx
    }
}

/**
 * 拖动偏移量的合法范围：LTR 向左右分别受首/末槽限制，RTL 镜像。
 *
 * 空列表或未测量时返回 `0f..0f`，调用方据此跳过拖动。
 */
fun dragOffsetRange(
    selectedIndex: Int,
    count: Int,
    slotWidthPx: Float,
    rtl: Boolean,
): ClosedFloatingPointRange<Float> {
    if (count <= 0 || slotWidthPx <= 0f) return 0f..0f
    val index = selectedIndex.coerceIn(0, count - 1)
    return if (rtl) {
        -(count - 1 - index) * slotWidthPx..index * slotWidthPx
    } else {
        -index * slotWidthPx..(count - 1 - index) * slotWidthPx
    }
}

/**
 * 根据横向坐标定位槽位索引，结果始终 clamp 到 `0 until count`。
 *
 * RTL 下以 Dock 中线镜像 x，使最左侧的触摸落到最后一个逻辑槽位。
 * 宽度非法（空列表、未测量、内边距过大）时返回 0。
 */
fun slotIndexAt(
    xPx: Float,
    dockWidthPx: Float,
    count: Int,
    paddingPx: Float,
    rtl: Boolean,
): Int {
    if (count <= 0) return 0
    val widthPx = slotWidth(dockWidthPx, count, paddingPx)
    if (widthPx <= 0f) return 0

    val mirroredX = if (rtl) dockWidthPx - xPx else xPx
    val slot = floor((mirroredX - paddingPx) / widthPx).toInt()
    return slot.coerceIn(0, count - 1)
}
