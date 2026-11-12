package com.recaitelab.app.ui.prism

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [slotWidth] / [slotIndexAt] / [indicatorLeftPx] / [dragOffsetRange] 的几何单测。
 *
 * 全局常量与 P1 的 Dock 一致：4 个槽位、Dock 宽 400px、内边距 8px，
 * 因此内容区宽 384px，单槽宽 96px，槽位边界在 8 / 104 / 200 / 296 / 392。
 */
class PrismDockMathTest {

    private val dockWidthPx = 400f
    private val paddingPx = 8f
    private val count = 4

    @Test
    fun slotWidth_dividesContentAreaEqually() {
        assertEquals(96f, slotWidth(dockWidthPx, count, paddingPx), DELTA)
    }

    @Test
    fun slotIndexAt_leftEdge_returnsFirstSlot() {
        assertEquals(0, slotIndexAt(paddingPx, dockWidthPx, count, paddingPx, rtl = false))
        assertEquals(0, slotIndexAt(103.9f, dockWidthPx, count, paddingPx, rtl = false))
    }

    @Test
    fun slotIndexAt_firstBoundary_returnsSecondSlot() {
        assertEquals(1, slotIndexAt(104f, dockWidthPx, count, paddingPx, rtl = false))
    }

    @Test
    fun slotIndexAt_middle_returnsMatchingSlot() {
        // x = 200 恰好落在第二/第三槽位的分界，floor 归入右侧槽位。
        assertEquals(2, slotIndexAt(200f, dockWidthPx, count, paddingPx, rtl = false))
        assertEquals(2, slotIndexAt(248f, dockWidthPx, count, paddingPx, rtl = false))
    }

    @Test
    fun slotIndexAt_rightEdge_returnsLastSlot() {
        assertEquals(3, slotIndexAt(296f, dockWidthPx, count, paddingPx, rtl = false))
        assertEquals(3, slotIndexAt(391.9f, dockWidthPx, count, paddingPx, rtl = false))
        assertEquals(3, slotIndexAt(392f, dockWidthPx, count, paddingPx, rtl = false))
    }

    @Test
    fun slotIndexAt_clampsOutsideDock() {
        assertEquals(0, slotIndexAt(-100f, dockWidthPx, count, paddingPx, rtl = false))
        assertEquals(0, slotIndexAt(0f, dockWidthPx, count, paddingPx, rtl = false))
        assertEquals(3, slotIndexAt(1000f, dockWidthPx, count, paddingPx, rtl = false))
    }

    @Test
    fun slotIndexAt_rtlMirrorsX() {
        // RTL 下最左（x = 0）应命中最后一个逻辑槽位。
        assertEquals(3, slotIndexAt(0f, dockWidthPx, count, paddingPx, rtl = true))
        assertEquals(0, slotIndexAt(392f, dockWidthPx, count, paddingPx, rtl = true))
        assertEquals(1, slotIndexAt(296f, dockWidthPx, count, paddingPx, rtl = true))
    }

    @Test
    fun slotWidth_defensiveOnInvalidInput() {
        assertEquals(0f, slotWidth(0f, count, paddingPx), DELTA)
        assertEquals(0f, slotWidth(-1f, count, paddingPx), DELTA)
        assertEquals(0f, slotWidth(dockWidthPx, 0, paddingPx), DELTA)
        assertEquals(0f, slotWidth(dockWidthPx, -2, paddingPx), DELTA)
        // 内边距吃满宽度时不应出现负槽宽。
        assertEquals(0f, slotWidth(10f, count, paddingPx), DELTA)
    }

    @Test
    fun slotIndexAt_defensiveOnInvalidInput() {
        assertEquals(0, slotIndexAt(200f, 0f, count, paddingPx, rtl = false))
        assertEquals(0, slotIndexAt(200f, dockWidthPx, 0, paddingPx, rtl = false))
        assertEquals(0, slotIndexAt(200f, dockWidthPx, -1, paddingPx, rtl = true))
        assertEquals(0, slotIndexAt(200f, 10f, count, paddingPx, rtl = false))
    }

    @Test
    fun indicatorLeftPx_ltrReturnsSlotLeftEdges() {
        // 槽位边界 8 / 104 / 200 / 296，与 slotIndexAt 的边界一致。
        assertEquals(8f, indicatorLeftPx(dockWidthPx, count, paddingPx, 0, rtl = false), DELTA)
        assertEquals(104f, indicatorLeftPx(dockWidthPx, count, paddingPx, 1, rtl = false), DELTA)
        assertEquals(200f, indicatorLeftPx(dockWidthPx, count, paddingPx, 2, rtl = false), DELTA)
        assertEquals(296f, indicatorLeftPx(dockWidthPx, count, paddingPx, 3, rtl = false), DELTA)
    }

    @Test
    fun indicatorLeftPx_rtlMirrorsSlotLeftEdges() {
        assertEquals(296f, indicatorLeftPx(dockWidthPx, count, paddingPx, 0, rtl = true), DELTA)
        assertEquals(200f, indicatorLeftPx(dockWidthPx, count, paddingPx, 1, rtl = true), DELTA)
        assertEquals(104f, indicatorLeftPx(dockWidthPx, count, paddingPx, 2, rtl = true), DELTA)
        assertEquals(8f, indicatorLeftPx(dockWidthPx, count, paddingPx, 3, rtl = true), DELTA)
    }

    @Test
    fun slotCenters_mapBackToTheirLogicalSlot() {
        for (index in 0 until count) {
            val ltrCenter = indicatorLeftPx(dockWidthPx, count, paddingPx, index, rtl = false) + 48f
            assertEquals(index, slotIndexAt(ltrCenter, dockWidthPx, count, paddingPx, rtl = false))

            val rtlCenter = indicatorLeftPx(dockWidthPx, count, paddingPx, index, rtl = true) + 48f
            assertEquals(index, slotIndexAt(rtlCenter, dockWidthPx, count, paddingPx, rtl = true))
        }
    }

    @Test
    fun dragOffsetRange_clampsToDockEdges() {
        val ltrFirst = dragOffsetRange(selectedIndex = 0, count = count, slotWidthPx = 96f, rtl = false)
        assertEquals(0f, ltrFirst.start, DELTA)
        assertEquals(288f, ltrFirst.endInclusive, DELTA)
        assertEquals(0f, (-500f).coerceIn(ltrFirst), DELTA)

        val ltrLast = dragOffsetRange(selectedIndex = 3, count = count, slotWidthPx = 96f, rtl = false)
        assertEquals(-288f, ltrLast.start, DELTA)
        assertEquals(0f, ltrLast.endInclusive, DELTA)
        assertEquals(0f, (500f).coerceIn(ltrLast), DELTA)

        val rtlFirst = dragOffsetRange(selectedIndex = 0, count = count, slotWidthPx = 96f, rtl = true)
        assertEquals(-288f, rtlFirst.start, DELTA)
        assertEquals(0f, rtlFirst.endInclusive, DELTA)

        val rtlLast = dragOffsetRange(selectedIndex = 3, count = count, slotWidthPx = 96f, rtl = true)
        assertEquals(0f, rtlLast.start, DELTA)
        assertEquals(288f, rtlLast.endInclusive, DELTA)
    }

    @Test
    fun indicatorAndDragRange_defensiveOnInvalidInput() {
        assertEquals(0f, indicatorLeftPx(0f, count, paddingPx, 0, rtl = false), DELTA)
        assertEquals(0f, indicatorLeftPx(dockWidthPx, 0, paddingPx, 0, rtl = false), DELTA)
        assertEquals(0f, indicatorLeftPx(10f, count, paddingPx, 0, rtl = true), DELTA)
        // 索引越界应 clamp 到首/末槽位。
        assertEquals(8f, indicatorLeftPx(dockWidthPx, count, paddingPx, -1, rtl = false), DELTA)
        assertEquals(296f, indicatorLeftPx(dockWidthPx, count, paddingPx, 99, rtl = false), DELTA)
        assertEquals(0f, dragOffsetRange(0, 0, 96f, rtl = false).start, DELTA)
        assertEquals(0f, dragOffsetRange(0, count, 0f, rtl = false).endInclusive, DELTA)
    }

    @Test
    fun morphIndicatorHeight_interpolatesAndClampsProgress() {
        val base = 224f
        val morph = 304f
        assertEquals(base, morphIndicatorHeight(base, morph, 0f), DELTA)
        assertEquals(morph, morphIndicatorHeight(base, morph, 1f), DELTA)
        assertEquals(264f, morphIndicatorHeight(base, morph, 0.5f), DELTA)
        assertEquals(base, morphIndicatorHeight(base, morph, -0.5f), DELTA)
        assertEquals(morph, morphIndicatorHeight(base, morph, 1.5f), DELTA)
    }

    @Test
    fun morphIndicatorWidth_scalesSlotByProgress() {
        assertEquals(96f, morphIndicatorWidth(96f, 1.04f, 0f), DELTA)
        assertEquals(99.84f, morphIndicatorWidth(96f, 1.04f, 1f), DELTA)
        assertEquals(97.92f, morphIndicatorWidth(96f, 1.04f, 0.5f), DELTA)
        assertEquals(0f, morphIndicatorWidth(0f, 1.04f, 1f), DELTA)
    }

    @Test
    fun morphIndicatorTop_isBottomAnchored() {
        // 80dp 根（320px）、4dp inset（16px）：56dp 指示器 top=80，76dp 指示器 top=0。
        assertEquals(80f, morphIndicatorTop(320f, 16f, 224f), DELTA)
        assertEquals(0f, morphIndicatorTop(320f, 16f, 304f), DELTA)
        // 小容器不应出现负偏移。
        assertEquals(0f, morphIndicatorTop(100f, 16f, 304f), DELTA)
    }

    @Test
    fun morphIndicatorLeft_centersAndClampsAtDockEdges() {
        // 400px Dock、8px padding、4 个 96px 槽；1.04× 后槽宽 99.84px。
        // 首槽放大后中心在 6.08，但左边界受 padding 限制，实际贴到 8。
        assertEquals(8f, morphIndicatorLeft(8f, 0f, 96f, 99.84f, 400f, 8f), DELTA)
        assertEquals(102.08f, morphIndicatorLeft(104f, 0f, 96f, 99.84f, 400f, 8f), DELTA)
        // 末槽放大后顶到右边界：400 - 8 - 99.84 = 292.16。
        assertEquals(292.16f, morphIndicatorLeft(296f, 0f, 96f, 99.84f, 400f, 8f), DELTA)
        // 拖动偏移参与中心计算。
        assertEquals(198.08f, morphIndicatorLeft(104f, 96f, 96f, 99.84f, 400f, 8f), DELTA)
    }

    @Test
    fun morphMath_defensiveOnInvalidInput() {
        assertEquals(0f, morphIndicatorHeight(0f, 0f, 1f), DELTA)
        // 未测量时不 clamp，直接返回中心位置。
        assertEquals(50f, morphIndicatorLeft(50f, 0f, 0f, 0f, 0f, 8f), DELTA)
        assertEquals(-4f, morphIndicatorLeft(0f, 0f, 96f, 104f, 0f, 8f), DELTA)
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
