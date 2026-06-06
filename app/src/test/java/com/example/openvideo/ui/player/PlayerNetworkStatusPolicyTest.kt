package com.example.openvideo.ui.player

import androidx.media3.common.Player
import com.example.openvideo.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerNetworkStatusPolicyTest {

    @Test
    fun networkBufferingShowsBufferingStatus() {
        val presentation = PlayerNetworkStatusPolicy.present(
            playbackState = Player.STATE_BUFFERING,
            isNetworkUri = true,
            isLive = false,
            durationMs = 60_000,
            autoRetryPending = false
        )

        assertTrue(presentation.visible)
        assertEquals(R.string.player_network_status_buffering, presentation.labelRes)
    }

    @Test
    fun autoRetryShowsReconnectingStatus() {
        val presentation = PlayerNetworkStatusPolicy.present(
            playbackState = Player.STATE_IDLE,
            isNetworkUri = true,
            isLive = false,
            durationMs = 0,
            autoRetryPending = true
        )

        assertTrue(presentation.visible)
        assertEquals(R.string.player_network_status_reconnecting, presentation.labelRes)
    }

    @Test
    fun networkReadyLiveOrUnknownDurationShowsLiveStatus() {
        val live = PlayerNetworkStatusPolicy.present(
            playbackState = Player.STATE_READY,
            isNetworkUri = true,
            isLive = true,
            durationMs = 0,
            autoRetryPending = false
        )
        val unknownDuration = PlayerNetworkStatusPolicy.present(
            playbackState = Player.STATE_READY,
            isNetworkUri = true,
            isLive = false,
            durationMs = -9_223_372_036_854_775_807L,
            autoRetryPending = false
        )

        assertEquals(R.string.player_network_status_live, live.labelRes)
        assertEquals(R.string.player_network_status_live, unknownDuration.labelRes)
    }

    @Test
    fun localOrKnownDurationReadyPlaybackHidesStatus() {
        assertFalse(
            PlayerNetworkStatusPolicy.present(
                playbackState = Player.STATE_BUFFERING,
                isNetworkUri = false,
                isLive = false,
                durationMs = 60_000,
                autoRetryPending = false
            ).visible
        )
        assertFalse(
            PlayerNetworkStatusPolicy.present(
                playbackState = Player.STATE_READY,
                isNetworkUri = true,
                isLive = false,
                durationMs = 60_000,
                autoRetryPending = false
            ).visible
        )
    }
}
