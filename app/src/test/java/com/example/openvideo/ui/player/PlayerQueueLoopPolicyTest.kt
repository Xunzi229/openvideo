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
                loopMode = LoopMode.OFF
            )
        )
    }

    @Test
    fun playlistLoopAdvancesAndWrapsEvenWhenAutoPlayNextIsDisabled() {
        assertEquals(
            1,
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 0,
                queueSize = 3,
                loopMode = LoopMode.LIST
            )
        )
        assertEquals(
            0,
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 2,
                queueSize = 3,
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
                loopMode = LoopMode.OFF
            )
        )
        assertNull(
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = 0,
                queueSize = 1,
                loopMode = LoopMode.LIST
            )
        )
        assertNull(
            PlayerQueueLoopPolicy.nextIndexAfterEnded(
                currentIndex = -1,
                queueSize = 3,
                loopMode = LoopMode.LIST
            )
        )
    }
}
