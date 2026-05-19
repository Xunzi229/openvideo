package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerTimeFormatterTest {

    @Test
    fun zeroAndNegativeFormatAsMinuteSecondZero() {
        assertEquals("00:00", PlayerTimeFormatter.format(0L))
        assertEquals("00:00", PlayerTimeFormatter.format(-1L))
        assertEquals("00:00", PlayerTimeFormatter.format(-3_600_000L))
    }

    @Test
    fun subSecondTruncatesToZero() {
        assertEquals("00:00", PlayerTimeFormatter.format(999L))
    }

    @Test
    fun singleDigitMinutesArePaddedWithZero() {
        assertEquals("00:05", PlayerTimeFormatter.format(5_000L))
        assertEquals("00:59", PlayerTimeFormatter.format(59_000L))
        assertEquals("01:00", PlayerTimeFormatter.format(60_000L))
        assertEquals("09:09", PlayerTimeFormatter.format(9 * 60_000L + 9_000L))
    }

    @Test
    fun hourBoundarySwitchesToHourFormat() {
        assertEquals("59:59", PlayerTimeFormatter.format(3_599_000L))
        assertEquals("1:00:00", PlayerTimeFormatter.format(3_600_000L))
        assertEquals("1:30:45", PlayerTimeFormatter.format(3_600_000L + 30 * 60_000L + 45_000L))
    }

    @Test
    fun longVideosKeepHoursUnpadded() {
        assertEquals("12:34:56", PlayerTimeFormatter.format(12L * 3_600_000L + 34L * 60_000L + 56_000L))
        assertEquals("100:00:00", PlayerTimeFormatter.format(100L * 3_600_000L))
    }
}
