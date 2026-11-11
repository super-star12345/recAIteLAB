package com.recaitelab.app.ui.prism

import kotlin.math.floor

/**
 * Dock 指示器的纯几何计算，不依赖 Compose。
 *
 * P1 只由单元测试覆盖，供 P3 的命中测试（按点击位置定位槽位）复用；
 * 当前 UI 仍使用迁移前的内联计算，避免本阶段引入行为变化。
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
