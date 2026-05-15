package com.example.openvideo.ui.player

import com.example.openvideo.R
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerPlayPausePolicyTest {

    @Test
    fun isPlayingStateUsesPauseIcon() {
        assertEquals(
            R.drawable.ic_pause,
            PlayerPlayPausePolicy.iconFor(isPlaying = true, playWhenReady = false)
        )
    }

    @Test
    fun playWhenReadyKeepsPauseIconDuringTransientNonPlayingState() {
        assertEquals(
            R.drawable.ic_pause,
            PlayerPlayPausePolicy.iconFor(isPlaying = false, playWhenReady = true)
        )
    }

    @Test
    fun pausedOrStoppedStateUsesPlayIcon() {
        assertEquals(
            R.drawable.ic_play,
            PlayerPlayPausePolicy.iconFor(isPlaying = false, playWhenReady = false)
        )
    }

    @Test
    fun toggleActionPredictsNextIconAndPlaybackRequest() {
        assertEquals(
            PlayPauseToggleDecision(
                targetPlayWhenReady = false,
                iconRes = R.drawable.ic_play
            ),
            PlayerPlayPausePolicy.toggleDecision(
                isPlaying = true,
                playWhenReady = true
            )
        )
        assertEquals(
            PlayPauseToggleDecision(
                targetPlayWhenReady = true,
                iconRes = R.drawable.ic_pause
            ),
            PlayerPlayPausePolicy.toggleDecision(
                isPlaying = false,
                playWhenReady = false
            )
        )
    }
}
