package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.LoopMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerQueueLoopPolicyTest {

    @Test
    fun loopOffStopsAfterCurrentVideoEvenWhenAutoPlayNextIsEnabled() {
        assertNull(
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 0,
                queueSize = 3,
                autoPlayNext = true,
                loopMode = LoopMode.OFF
            )
        )
    }

    @Test
    fun playlistLoopAdvancesAndWrapsWhenAutoPlayNextIsEnabled() {
        assertEquals(
            1,
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 0,
                queueSize = 3,
                autoPlayNext = true,
                loopMode = LoopMode.LIST
            )
        )
        assertEquals(
            0,
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 2,
                queueSize = 3,
                autoPlayNext = true,
                loopMode = LoopMode.LIST
            )
        )
    }

    @Test
    fun playlistLoopDoesNotAdvanceWhenAutoPlayNextIsDisabled() {
        assertNull(
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 0,
                queueSize = 3,
                autoPlayNext = false,
                loopMode = LoopMode.LIST
            )
        )
    }

    @Test
    fun stopsAtLastVideoWhenPlaylistLoopIsOff() {
        assertNull(
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 2,
                queueSize = 3,
                autoPlayNext = true,
                loopMode = LoopMode.OFF
            )
        )
    }

    @Test
    fun doesNotAdvanceWhenPlaylistLoopIsOffOrQueueCannotAdvance() {
        assertNull(
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 0,
                queueSize = 3,
                autoPlayNext = true,
                loopMode = LoopMode.OFF
            )
        )
        assertNull(
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 0,
                queueSize = 1,
                autoPlayNext = true,
                loopMode = LoopMode.LIST
            )
        )
        assertNull(
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = -1,
                queueSize = 3,
                autoPlayNext = true,
                loopMode = LoopMode.LIST
            )
        )
    }
}
