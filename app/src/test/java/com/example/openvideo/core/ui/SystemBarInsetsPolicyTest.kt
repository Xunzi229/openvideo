package com.example.openvideo.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemBarInsetsPolicyTest {

    @Test
    fun unionTakesTheLargerInsetOnEachEdge() {
        val bars = SystemBarInsetsPolicy.Edges(left = 0, top = 48, right = 0, bottom = 24)
        val cutout = SystemBarInsetsPolicy.Edges(left = 12, top = 36, right = 8, bottom = 0)

        val union = SystemBarInsetsPolicy.union(bars, cutout)

        assertEquals(12, union.left)
        assertEquals(48, union.top)
        assertEquals(8, union.right)
        assertEquals(24, union.bottom)
    }

    @Test
    fun unionIgnoresNegativeInsets() {
        val bars = SystemBarInsetsPolicy.Edges(left = -4, top = 20, right = 0, bottom = -8)
        val cutout = SystemBarInsetsPolicy.Edges(left = 0, top = -2, right = -1, bottom = 16)

        val union = SystemBarInsetsPolicy.union(bars, cutout)

        assertEquals(0, union.left)
        assertEquals(20, union.top)
        assertEquals(0, union.right)
        assertEquals(16, union.bottom)
    }

    @Test
    fun overlayBottomPaddingKeepsContentAboveSystemBar() {
        assertEquals(56, SystemBarInsetsPolicy.overlayBottomPadding(baseBottom = 8, insetBottom = 48, extraBottom = 8))
        assertEquals(16, SystemBarInsetsPolicy.overlayBottomPadding(baseBottom = 16, insetBottom = 0, extraBottom = 8))
        assertEquals(0, SystemBarInsetsPolicy.overlayBottomPadding(baseBottom = 0, insetBottom = -4, extraBottom = 0))
    }

    @Test
    fun overlayMaxHeightKeepsPanelAboveBottomInset() {
        assertEquals(400, SystemBarInsetsPolicy.overlayMaxHeight(
            containerHeight = 800,
            topOffset = 320,
            bottomInset = 48,
            extraBottom = 32
        ))
        assertEquals(0, SystemBarInsetsPolicy.overlayMaxHeight(
            containerHeight = 100,
            topOffset = 80,
            bottomInset = 40,
            extraBottom = 8
        ))
    }
}
