package com.example.openvideo.ui.home

import com.example.openvideo.core.ui.ScreenBreakpoint

object HomeAdaptiveLayoutPolicy {

    fun spanCount(
        viewMode: ViewMode,
        breakpoint: ScreenBreakpoint
    ): Int =
        when (viewMode) {
            ViewMode.LIST -> 1
            ViewMode.GRID -> when (breakpoint) {
                ScreenBreakpoint.COMPACT -> 2
                ScreenBreakpoint.MEDIUM -> 3
                ScreenBreakpoint.EXPANDED -> 4
            }
        }
}
