package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerPlaybackTickPolicyTest {

    @Test
    fun abLoopSeekWinsOverIntroAndClipLoopOnSameTick() {
        val result = PlayerPlaybackTickPolicy.seekTarget(
            currentPositionMs = 20_000,
            durationMs = 120_000,
            abLoopState = PlayerAbLoopState.LOOPING,
            abLoopPointA = 5_000,
            abLoopPointB = 20_000,
            skipIntroOutro = true,
            introSeconds = 30,
            outroSeconds = 0,
            hasSkippedIntro = false,
            hasSkippedOutro = false,
            clipLoopPreview = true,
            clipStartMs = 3_000,
            clipEndMs = 10_000
        )

        assertEquals(PlayerPlaybackTickKind.AB_LOOP, result!!.kind)
        assertEquals(5_000L, result.positionMs)
        assertEquals(false, result.hasSkippedIntro)
        assertEquals(false, result.hasSkippedOutro)
    }

    @Test
    fun introSkipWinsOverClipLoopOnSameTick() {
        val result = PlayerPlaybackTickPolicy.seekTarget(
            currentPositionMs = 500,
            durationMs = 120_000,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1,
            abLoopPointB = -1,
            skipIntroOutro = true,
            introSeconds = 30,
            outroSeconds = 0,
            hasSkippedIntro = false,
            hasSkippedOutro = false,
            clipLoopPreview = true,
            clipStartMs = 0,
            clipEndMs = 400
        )

        assertEquals(PlayerPlaybackTickKind.INTRO, result!!.kind)
        assertEquals(30_000L, result.positionMs)
        assertEquals(true, result.hasSkippedIntro)
        assertEquals(false, result.hasSkippedOutro)
    }

    @Test
    fun outroSkipWinsOverClipLoopOnSameTick() {
        val result = PlayerPlaybackTickPolicy.seekTarget(
            currentPositionMs = 111_000,
            durationMs = 120_000,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1,
            abLoopPointB = -1,
            skipIntroOutro = true,
            introSeconds = 0,
            outroSeconds = 10,
            hasSkippedIntro = true,
            hasSkippedOutro = false,
            clipLoopPreview = true,
            clipStartMs = 3_000,
            clipEndMs = 10_000
        )

        assertEquals(PlayerPlaybackTickKind.OUTRO, result!!.kind)
        assertEquals(120_000L, result.positionMs)
        assertEquals(true, result.hasSkippedIntro)
        assertEquals(true, result.hasSkippedOutro)
    }

    @Test
    fun clipLoopSeeksToStartWhenNoHigherPrioritySeekExists() {
        val result = PlayerPlaybackTickPolicy.seekTarget(
            currentPositionMs = 10_000,
            durationMs = 120_000,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1,
            abLoopPointB = -1,
            skipIntroOutro = false,
            introSeconds = 0,
            outroSeconds = 0,
            hasSkippedIntro = false,
            hasSkippedOutro = false,
            clipLoopPreview = true,
            clipStartMs = 3_000,
            clipEndMs = 10_000
        )

        assertEquals(PlayerPlaybackTickKind.CLIP_LOOP, result!!.kind)
        assertEquals(3_000L, result.positionMs)
    }

    @Test
    fun invalidClipLoopDoesNotSeek() {
        val result = PlayerPlaybackTickPolicy.seekTarget(
            currentPositionMs = 10_000,
            durationMs = 120_000,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1,
            abLoopPointB = -1,
            skipIntroOutro = false,
            introSeconds = 0,
            outroSeconds = 0,
            hasSkippedIntro = false,
            hasSkippedOutro = false,
            clipLoopPreview = true,
            clipStartMs = 10_000,
            clipEndMs = 10_000
        )

        assertNull(result)
    }
}
