package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackProgressPolicyTest {

    @Test
    fun ignoresNonPositivePositions() {
        assertFalse(
            PlaybackProgressPolicy.shouldSaveProgress(
                positionMs = 0,
                lastSavedPositionMs = 0
            )
        )
        assertFalse(
            PlaybackProgressPolicy.shouldSaveProgress(
                positionMs = -1,
                lastSavedPositionMs = 0
            )
        )
    }

    @Test
    fun skipsSmallMovementSinceLastSave() {
        assertFalse(
            PlaybackProgressPolicy.shouldSaveProgress(
                positionMs = 14_999,
                lastSavedPositionMs = 10_000
            )
        )
    }

    @Test
    fun savesAtFiveSecondBoundary() {
        assertTrue(
            PlaybackProgressPolicy.shouldSaveProgress(
                positionMs = 15_000,
                lastSavedPositionMs = 10_000
            )
        )
    }

    @Test
    fun savesWhenPositionJumpsBackwardByAtLeastFiveSeconds() {
        assertTrue(
            PlaybackProgressPolicy.shouldSaveProgress(
                positionMs = 10_000,
                lastSavedPositionMs = 16_000
            )
        )
    }

    @Test
    fun tickKeepsLastSavedPositionWhenSkippingSave() {
        val decision = PlaybackProgressPolicy.onPositionTick(
            positionMs = 12_000,
            lastSavedPositionMs = 10_000
        )

        assertFalse(decision.shouldSaveHistory)
        assertTrue(decision.nextLastSavedPositionMs == 10_000L)
    }

    @Test
    fun tickAdvancesLastSavedPositionWhenSaving() {
        val decision = PlaybackProgressPolicy.onPositionTick(
            positionMs = 15_000,
            lastSavedPositionMs = 10_000
        )

        assertTrue(decision.shouldSaveHistory)
        assertTrue(decision.nextLastSavedPositionMs == 15_000L)
    }

    @Test
    fun newMediaResetsLastSavedPosition() {
        assertTrue(PlaybackProgressPolicy.onNewMedia() == 0L)
    }
}
