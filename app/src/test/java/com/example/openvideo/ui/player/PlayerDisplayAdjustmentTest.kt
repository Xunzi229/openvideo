package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerDisplayAdjustmentTest {

    @Test
    fun mirrorScaleXFlipsWhenEnabled() {
        assertEquals(-1f, PlayerDisplayAdjustment.mirrorScaleX(mirror = true))
        assertEquals(1f, PlayerDisplayAdjustment.mirrorScaleX(mirror = false))
    }

    @Test
    fun subtitleTranslationYAtTopIsZero() {
        // position = 1f keeps the subtitle at its natural top -> no translation.
        assertEquals(0f, PlayerDisplayAdjustment.subtitleTranslationY(900, 1f), 0.0001f)
    }

    @Test
    fun subtitleTranslationYAtBottomPushesByFullTravel() {
        // position = 0f pushes the subtitle down by 60% of the view height (negative translation).
        val expected = -(900f * 0.6f)
        assertEquals(expected, PlayerDisplayAdjustment.subtitleTranslationY(900, 0f), 0.0001f)
    }

    @Test
    fun subtitleTranslationYClampsOutOfRangePosition() {
        val tooHigh = PlayerDisplayAdjustment.subtitleTranslationY(900, 1.5f)
        val tooLow = PlayerDisplayAdjustment.subtitleTranslationY(900, -0.2f)
        assertEquals(PlayerDisplayAdjustment.subtitleTranslationY(900, 1f), tooHigh, 0.0001f)
        assertEquals(PlayerDisplayAdjustment.subtitleTranslationY(900, 0f), tooLow, 0.0001f)
    }

    @Test
    fun subtitleTranslationYTreatsNegativeHeightAsZero() {
        // Negative heights can briefly appear before layout; clamp to 0 to avoid weird translation.
        assertEquals(0f, PlayerDisplayAdjustment.subtitleTranslationY(-100, 0f), 0.0001f)
    }
}
