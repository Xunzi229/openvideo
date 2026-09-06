package com.openvideo.app.ui

import com.openvideo.app.core.ui.ScreenBreakpoint

object BrowseAdaptiveLayoutPolicy {

    fun contentSpanCount(breakpoint: ScreenBreakpoint): Int =
        when (breakpoint) {
            ScreenBreakpoint.COMPACT -> 1
            ScreenBreakpoint.MEDIUM -> 2
            ScreenBreakpoint.EXPANDED -> 3
        }
}
