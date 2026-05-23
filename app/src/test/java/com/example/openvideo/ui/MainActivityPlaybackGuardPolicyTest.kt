package com.example.openvideo.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityPlaybackGuardPolicyTest {

    @Test
    fun pausesPlayingVideoWhenMainActivityIsForegroundWithoutBackgroundAudio() {
        val decision = MainActivityPlaybackGuardPolicy.onResume(
            backgroundAudio = false,
            playerExists = true,
            isPlayingOrRequested = true
        )

        assertEquals(true, decision.pausePlayer)
        assertEquals(true, decision.stopPlaybackService)
    }

    @Test
    fun doesNotPauseWhenBackgroundAudioIsEnabled() {
        val decision = MainActivityPlaybackGuardPolicy.onResume(
            backgroundAudio = true,
            playerExists = true,
            isPlayingOrRequested = true
        )

        assertEquals(false, decision.pausePlayer)
        assertEquals(false, decision.stopPlaybackService)
    }

    @Test
    fun doesNotTouchPlaybackWhenNoPlayerExists() {
        val decision = MainActivityPlaybackGuardPolicy.onResume(
            backgroundAudio = false,
            playerExists = false,
            isPlayingOrRequested = false
        )

        assertEquals(false, decision.pausePlayer)
        assertEquals(false, decision.stopPlaybackService)
    }
}
