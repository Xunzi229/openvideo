package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTimelineTest {

    @Test
    fun usesRealMillisecondsWhenDurationFitsSeekBar() {
        val state = PlayerTimeline.seekBarState(positionMs = 30_000, durationMs = 90_000)

        assertTrue(state.enabled)
        assertEquals(90_000, state.max)
        assertEquals(30_000, state.progress)
    }

    @Test
    fun scalesHugeDurationsWithoutOverflowingSeekBar() {
        val state = PlayerTimeline.seekBarState(
            positionMs = Int.MAX_VALUE.toLong() + 1_000L,
            durationMs = Int.MAX_VALUE.toLong() * 2L
        )

        assertTrue(state.enabled)
        assertEquals(PlayerTimeline.SCALED_SEEK_BAR_MAX, state.max)
        assertEquals(5_000, state.progress)
    }

    @Test
    fun disablesSeekBarForInvalidDurations() {
        assertFalse(PlayerTimeline.seekBarState(positionMs = 0, durationMs = 0).enabled)
        assertFalse(PlayerTimeline.seekBarState(positionMs = 0, durationMs = Long.MAX_VALUE).enabled)
        assertFalse(PlayerTimeline.seekBarState(positionMs = 0, durationMs = -9_223_372_036_854_775_807L).enabled)
    }

    @Test
    fun mapsScaledProgressBackToLongPosition() {
        val position = PlayerTimeline.positionFromSeekBar(
            progress = 5_000,
            max = PlayerTimeline.SCALED_SEEK_BAR_MAX,
            durationMs = Int.MAX_VALUE.toLong() * 2L
        )

        assertEquals(Int.MAX_VALUE.toLong(), position)
    }

    @Test
    fun usesUnknownLabelForUnseekableTotalDuration() {
        assertEquals("--:--", PlayerTimeline.durationText(0, PlayerTimeFormatter::format))
        assertEquals("--:--", PlayerTimeline.durationText(Long.MAX_VALUE, PlayerTimeFormatter::format))
        assertEquals(
            "--:--",
            PlayerTimeline.durationText(-9_223_372_036_854_775_807L, PlayerTimeFormatter::format)
        )
    }

    @Test
    fun formatsSeekableTotalDuration() {
        assertEquals("01:05", PlayerTimeline.durationText(65_000, PlayerTimeFormatter::format))
    }
}
