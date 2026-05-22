package com.example.openvideo.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackNotificationProgressPolicyTest {

    @Test
    fun barHiddenWhenDurationUnknown() {
        val bar = PlaybackNotificationProgressPolicy.barState(positionMs = 30_000, durationMs = 0)
        assertFalse(bar.visible)
    }

    @Test
    fun barMapsPositionIntoScaledRange() {
        val bar = PlaybackNotificationProgressPolicy.barState(positionMs = 50_000, durationMs = 100_000)
        assertTrue(bar.visible)
        assertEquals(PlaybackNotificationProgressPolicy.BAR_MAX, bar.max)
        assertEquals(500, bar.progress)
    }

    @Test
    fun progressBucketGroupsByInterval() {
        assertEquals(100L, PlaybackNotificationProgressPolicy.progressBucket(50_499))
        assertEquals(101L, PlaybackNotificationProgressPolicy.progressBucket(50_500))
    }

    @Test
    fun timeLabelsFormatElapsedAndDuration() {
        val labels = PlaybackNotificationProgressPolicy.timeLabels(
            positionMs = 90_000,
            durationMs = 366_1000
        )
        assertEquals("01:30", labels.elapsed)
        assertEquals("1:01:01", labels.duration)
    }

    @Test
    fun timeLabelsShowPlaceholderWhenDurationUnknown() {
        val labels = PlaybackNotificationProgressPolicy.timeLabels(
            positionMs = 45_000,
            durationMs = 0
        )
        assertEquals("00:45", labels.elapsed)
        assertEquals("--:--", labels.duration)
    }
}
