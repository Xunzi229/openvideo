package com.openvideo.app.ui

import com.openvideo.app.core.ui.ScreenBreakpoint
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseAdaptiveLayoutPolicyTest {

    @Test
    fun contentListsUseMoreColumnsOnlyOnTabletBreakpoints() {
        assertEquals(1, BrowseAdaptiveLayoutPolicy.contentSpanCount(ScreenBreakpoint.COMPACT))
        assertEquals(2, BrowseAdaptiveLayoutPolicy.contentSpanCount(ScreenBreakpoint.MEDIUM))
        assertEquals(3, BrowseAdaptiveLayoutPolicy.contentSpanCount(ScreenBreakpoint.EXPANDED))
    }
}
