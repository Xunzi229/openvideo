package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSessionQueueChromePolicyTest {

    @Test
    fun hidesSessionListForSingleItemQueue() {
        assertFalse(PlayerSessionQueueChromePolicy.shouldShowSessionListButton(1))
        assertFalse(PlayerSessionQueueChromePolicy.shouldShowSessionListButton(0))
    }

    @Test
    fun showsSessionListForMultiItemQueue() {
        assertTrue(PlayerSessionQueueChromePolicy.shouldShowSessionListButton(2))
    }

    @Test
    fun canOpenPanelMatchesVisibilityRule() {
        assertFalse(PlayerSessionQueueChromePolicy.canOpenSessionListPanel(1))
        assertTrue(PlayerSessionQueueChromePolicy.canOpenSessionListPanel(2))
    }
}
