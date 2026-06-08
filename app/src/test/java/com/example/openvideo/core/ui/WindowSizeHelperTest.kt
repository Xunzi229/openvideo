package com.example.openvideo.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowSizeHelperTest {

    @Test
    fun widthBreakpointsFollowMaterialCompactMediumExpandedBoundaries() {
        assertEquals(ScreenBreakpoint.COMPACT, WindowSizeHelper.fromWidthDp(0f))
        assertEquals(ScreenBreakpoint.COMPACT, WindowSizeHelper.fromWidthDp(599.9f))

        assertEquals(ScreenBreakpoint.MEDIUM, WindowSizeHelper.fromWidthDp(600f))
        assertEquals(ScreenBreakpoint.MEDIUM, WindowSizeHelper.fromWidthDp(839.9f))

        assertEquals(ScreenBreakpoint.EXPANDED, WindowSizeHelper.fromWidthDp(840f))
        assertEquals(ScreenBreakpoint.EXPANDED, WindowSizeHelper.fromWidthDp(1200f))
    }

    @Test
    fun breakpointFlagsExposeTabletEligibleStates() {
        assertTrue(ScreenBreakpoint.COMPACT.isCompact)
        assertFalse(ScreenBreakpoint.COMPACT.isAtLeastMedium)

        assertTrue(ScreenBreakpoint.MEDIUM.isMedium)
        assertTrue(ScreenBreakpoint.MEDIUM.isAtLeastMedium)

        assertTrue(ScreenBreakpoint.EXPANDED.isExpanded)
        assertTrue(ScreenBreakpoint.EXPANDED.isAtLeastMedium)
    }
}
