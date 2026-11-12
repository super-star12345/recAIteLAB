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

    private companion object {
        const val DELTA = 0.001f
    }
}
