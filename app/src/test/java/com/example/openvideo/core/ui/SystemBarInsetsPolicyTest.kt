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
}
