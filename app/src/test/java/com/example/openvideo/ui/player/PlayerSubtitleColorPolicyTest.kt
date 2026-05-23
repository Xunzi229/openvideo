package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSubtitleColorPolicyTest {

    @Test
    fun optionForKnownColorReturnsMatch() {
        val yellow = 0xFFFFEB3B.toInt()
        assertEquals(yellow, PlayerSubtitleColorPolicy.optionFor(yellow).color)
    }

    @Test
    fun optionForUnknownColorFallsBackToFirstOption() {
        assertEquals(
            PlayerSubtitleColorPolicy.options.first().color,
            PlayerSubtitleColorPolicy.optionFor(0x12345678).color
        )
    }

    @Test
    fun nextIndexCyclesThroughOptions() {
        val last = PlayerSubtitleColorPolicy.options.lastIndex
        assertEquals(0, PlayerSubtitleColorPolicy.nextIndex(last))
        assertEquals(1, PlayerSubtitleColorPolicy.nextIndex(0))
    }

    @Test
    fun swatchStrokeUsesAccentWhenSelected() {
        val white = PlayerSubtitleColorPolicy.options.first().color
        assertEquals(
            PlayerSubtitleColorPolicy.SWATCH_STROKE_SELECTED,
            PlayerSubtitleColorPolicy.swatchStrokeColor(white, selected = true)
        )
    }

    @Test
    fun swatchStrokeUsesDarkBorderOnWhiteWhenUnselected() {
        val white = PlayerSubtitleColorPolicy.options.first().color
        assertEquals(0x99000000.toInt(), PlayerSubtitleColorPolicy.swatchStrokeColor(white, selected = false))
    }
}
