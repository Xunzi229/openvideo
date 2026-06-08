package com.example.openvideo.ui.home

import com.example.openvideo.core.ui.ScreenBreakpoint
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAdaptiveLayoutPolicyTest {

    @Test
    fun listModeAlwaysUsesSingleColumn() {
        ScreenBreakpoint.entries.forEach { breakpoint ->
            assertEquals(
                1,
                HomeAdaptiveLayoutPolicy.spanCount(
                    viewMode = ViewMode.LIST,
                    breakpoint = breakpoint
                )
            )
        }
    }

    @Test
    fun gridModeUsesMoreColumnsOnTabletBreakpoints() {
        assertEquals(
            2,
            HomeAdaptiveLayoutPolicy.spanCount(
                viewMode = ViewMode.GRID,
                breakpoint = ScreenBreakpoint.COMPACT
            )
        )
        assertEquals(
            3,
            HomeAdaptiveLayoutPolicy.spanCount(
                viewMode = ViewMode.GRID,
                breakpoint = ScreenBreakpoint.MEDIUM
            )
        )
        assertEquals(
            4,
            HomeAdaptiveLayoutPolicy.spanCount(
                viewMode = ViewMode.GRID,
                breakpoint = ScreenBreakpoint.EXPANDED
            )
        )
    }
}
