package com.example.openvideo.ui.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerPlaybackReadyTracePolicyTest {

    @Test
    fun bufferingSetsLatchForNextReady() {
        val update = PlayerPlaybackReadyTracePolicy.onPlaybackStateChanged(
            playbackState = Player.STATE_BUFFERING,
            wasBuffering = false,
            hasRecordedPrepareReady = true
        )

        assertNull(update.readyTraceEvent)
        assertEquals(true, update.nextWasBuffering)
    }

    @Test
    fun firstReadyUsesPrepareReadyEventEvenAfterBuffering() {
        val update = PlayerPlaybackReadyTracePolicy.onPlaybackStateChanged(
            playbackState = Player.STATE_READY,
            wasBuffering = true,
            hasRecordedPrepareReady = false
        )

        assertEquals(
            PlayerPlaybackReadyTracePolicy.ReadyTraceEvent.FIRST_PREPARE_READY,
            update.readyTraceEvent
        )
        assertEquals(false, update.nextWasBuffering)
    }

    @Test
    fun laterReadyAfterBufferingUsesDistinctEventName() {
        val update = PlayerPlaybackReadyTracePolicy.onPlaybackStateChanged(
            playbackState = Player.STATE_READY,
            wasBuffering = true,
            hasRecordedPrepareReady = true
        )

        assertEquals(
            PlayerPlaybackReadyTracePolicy.ReadyTraceEvent.RECOVERED_AFTER_BUFFERING,
            update.readyTraceEvent
        )
    }

    @Test
    fun laterReadyWithoutBufferingDoesNotEmitTraceEvent() {
        val update = PlayerPlaybackReadyTracePolicy.onPlaybackStateChanged(
            playbackState = Player.STATE_READY,
            wasBuffering = false,
            hasRecordedPrepareReady = true
        )

        assertNull(update.readyTraceEvent)
    }
}
