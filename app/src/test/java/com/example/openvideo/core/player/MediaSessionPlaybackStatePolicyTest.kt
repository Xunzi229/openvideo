package com.example.openvideo.core.player

import android.support.v4.media.session.PlaybackStateCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSessionPlaybackStatePolicyTest {

    @Test
    fun playingStateIncludesPlayPauseAndSeekActions() {
        val update = MediaSessionPlaybackStatePolicy.resolve(
            isPlaying = true,
            positionMs = 12_000L,
            speed = 1.25f,
            canSkipToNext = false,
            canSkipToPrevious = false
        )

        assertEquals(PlaybackStateCompat.STATE_PLAYING, update.state)
        assertEquals(12_000L, update.positionMs)
        assertEquals(1.25f, update.speed)
        assertTrue(update.actions and PlaybackStateCompat.ACTION_PLAY != 0L)
        assertTrue(update.actions and PlaybackStateCompat.ACTION_PAUSE != 0L)
        assertTrue(update.actions and PlaybackStateCompat.ACTION_SEEK_TO != 0L)
        assertFalse(update.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L)
        assertFalse(update.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L)
    }

    @Test
    fun pausedStateZeroesSpeedAndAddsSkipActionsWhenAllowed() {
        val update = MediaSessionPlaybackStatePolicy.resolve(
            isPlaying = false,
            positionMs = 4_000L,
            speed = 1f,
            canSkipToNext = true,
            canSkipToPrevious = true
        )

        assertEquals(PlaybackStateCompat.STATE_PAUSED, update.state)
        assertEquals(0f, update.speed)
        assertTrue(update.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L)
        assertTrue(update.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L)
    }

    @Test
    fun skipActionsAreIndependent() {
        val nextOnly = MediaSessionPlaybackStatePolicy.resolve(
            isPlaying = true,
            positionMs = 0L,
            speed = 1f,
            canSkipToNext = true,
            canSkipToPrevious = false
        )
        assertTrue(nextOnly.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L)
        assertFalse(nextOnly.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L)
    }
}
