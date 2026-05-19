package com.example.openvideo.core.player

import com.example.openvideo.core.prefs.LoopMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackNotificationSkipCapabilityPolicyTest {

    @Test
    fun nullSnapshotHasNoSkipControls() {
        val caps = PlaybackNotificationSkipCapabilityPolicy.fromSnapshot(null)
        assertFalse(caps.canSkipToNext)
        assertFalse(caps.canSkipToPrevious)
        assertFalse(caps.hasAnySkip)
    }

    @Test
    fun singleItemQueueHasNoSkipControls() {
        val caps = PlaybackNotificationSkipCapabilityPolicy.fromQueueIndex(
            currentIndex = 0,
            queueSize = 1,
            loopMode = LoopMode.LIST
        )

        assertFalse(caps.canSkipToNext)
        assertFalse(caps.canSkipToPrevious)
    }

    @Test
    fun middleItemInNonLoopModeCanSkipBothDirections() {
        val caps = PlaybackNotificationSkipCapabilityPolicy.fromQueueIndex(
            currentIndex = 1,
            queueSize = 3,
            loopMode = LoopMode.OFF
        )

        assertTrue(caps.canSkipToNext)
        assertTrue(caps.canSkipToPrevious)
    }

    @Test
    fun firstItemInOffModeCanOnlySkipForward() {
        val caps = PlaybackNotificationSkipCapabilityPolicy.fromQueueIndex(
            currentIndex = 0,
            queueSize = 2,
            loopMode = LoopMode.OFF
        )

        assertTrue(caps.canSkipToNext)
        assertFalse(caps.canSkipToPrevious)
    }

    @Test
    fun listLoopModeAllowsSkipFromBothEnds() {
        val first = PlaybackNotificationSkipCapabilityPolicy.fromQueueIndex(
            currentIndex = 0,
            queueSize = 3,
            loopMode = LoopMode.LIST
        )
        assertTrue(first.canSkipToNext)
        assertTrue(first.canSkipToPrevious)

        val last = PlaybackNotificationSkipCapabilityPolicy.fromQueueIndex(
            currentIndex = 2,
            queueSize = 3,
            loopMode = LoopMode.LIST
        )
        assertTrue(last.canSkipToNext)
        assertTrue(last.canSkipToPrevious)
    }
}
