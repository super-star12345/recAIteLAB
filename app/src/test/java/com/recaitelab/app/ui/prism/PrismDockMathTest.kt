package com.recaitelab.app.ui.prism

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [slotWidth] / [slotIndexAt] 的几何单测。
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

    private companion object {
        const val DELTA = 0.001f
    }
}
