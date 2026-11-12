package com.recaitelab.app.ui.prism

import kotlin.math.floor

/**
 * Dock 指示器与拖动的纯几何计算，不依赖 Compose。
 *
 * [slotWidth] / [slotIndexAt] 自 P1 起由单测覆盖；P3 起 UI 也直接复用，
 * 拖动、RTL 与边界 clamp 全部走这里的函数，保证可测。
 *
 * P3.5 起拖动 morph 的高度/宽度/顶边/水平位置也集中在这里，progress 统一 clamp 到 0..1。
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

/**
 * 拖动 morph 后的指示器高度。
 *
 * [progress] 统一 clamp 到 0..1；progress = 0 返回静止高度，progress = 1 返回 morph 目标高度。
 */
fun morphIndicatorHeight(
    baseHeightPx: Float,
    morphHeightPx: Float,
    progress: Float,
): Float {
    if (baseHeightPx <= 0f && morphHeightPx <= 0f) return 0f
    val clampedProgress = progress.coerceIn(0f, 1f)
    return baseHeightPx + (morphHeightPx - baseHeightPx) * clampedProgress
}

/**
 * 拖动 morph 后的指示器宽度：槽宽按 [scale] 放大，[progress] clamp 到 0..1。
 *
 * 槽宽非法（未测量/空列表）时返回 0，调用方据此跳过绘制。
 */
fun morphIndicatorWidth(
    slotWidthPx: Float,
    scale: Float,
    progress: Float,
): Float {
    if (slotWidthPx <= 0f) return 0f
    val clampedProgress = progress.coerceIn(0f, 1f)
    return slotWidthPx * (1f + (scale - 1f) * clampedProgress)
}

/**
 * 指示器顶边的 y 坐标：底边固定在 `layoutHeight - inset`，高度增加时向上生长。
 *
 * 极端小容器下不小于 0，避免负偏移把指示器画到 Dock 外部。
 */
fun morphIndicatorTop(
    layoutHeightPx: Float,
    insetPx: Float,
    indicatorHeightPx: Float,
): Float = (layoutHeightPx - insetPx - indicatorHeightPx).coerceAtLeast(0f)

/**
 * 指示器水平位置：以槽位中心为轴放大，再按 Dock 内边距与宽度做边缘保护。
 *
 * 未测量、宽度非法或可用空间小于内边距时退回未 clamp 的中心位置，绝不抛异常。
 */
fun morphIndicatorLeft(
    baseLeftPx: Float,
    dragOffsetPx: Float,
    slotWidthPx: Float,
    indicatorWidthPx: Float,
    dockWidthPx: Float,
    paddingPx: Float,
): Float {
    val centeredLeft = baseLeftPx + dragOffsetPx - (indicatorWidthPx - slotWidthPx) / 2f
    if (dockWidthPx <= 0f || indicatorWidthPx <= 0f || slotWidthPx <= 0f) return centeredLeft
    val maxLeft = dockWidthPx - paddingPx - indicatorWidthPx
    return if (maxLeft < paddingPx) centeredLeft else centeredLeft.coerceIn(paddingPx, maxLeft)
}
