package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLifecyclePolicyTest {

    @Test
    fun pauseOutsidePipWithPauseOnExitSavesPausesAndStopsService() {
        val decision = PlayerLifecyclePolicy.onPause(
            isInPictureInPicture = false,
            pauseOnExit = true,
            backgroundAudio = true,
            isPlaying = true
        )

        assertEquals(true, decision.saveHistory)
        assertEquals(true, decision.pausePlayer)
        assertEquals(true, decision.unlockBeforePause)
        assertEquals(true, decision.stopPlaybackService)
        assertEquals(false, decision.startPlaybackService)
    }

    @Test
    fun pauseOutsidePipWithoutBackgroundAudioPausesEvenWhenPauseOnExitIsOff() {
        val decision = PlayerLifecyclePolicy.onPause(
            isInPictureInPicture = false,
            pauseOnExit = false,
            backgroundAudio = false,
            isPlaying = true
        )

        assertEquals(true, decision.saveHistory)
        assertEquals(true, decision.pausePlayer)
        assertEquals(true, decision.stopPlaybackService)
        assertEquals(false, decision.startPlaybackService)
    }

    @Test
    fun pauseOutsidePipWithBackgroundAudioKeepsPlayingAndStartsServiceOnlyWhenPlaying() {
        val playing = PlayerLifecyclePolicy.onPause(
            isInPictureInPicture = false,
            pauseOnExit = false,
            backgroundAudio = true,
            isPlaying = true
        )
        val paused = PlayerLifecyclePolicy.onPause(
            isInPictureInPicture = false,
            pauseOnExit = false,
            backgroundAudio = true,
            isPlaying = false
        )

        assertEquals(true, playing.saveHistory)
        assertEquals(false, playing.pausePlayer)
        assertEquals(false, playing.stopPlaybackService)
        assertEquals(true, playing.startPlaybackService)

        assertEquals(true, paused.saveHistory)
        assertEquals(false, paused.pausePlayer)
        assertEquals(false, paused.stopPlaybackService)
        assertEquals(false, paused.startPlaybackService)
    }

    @Test
    fun pauseInPipDoesNotSavePauseOrTouchPlaybackService() {
        val decision = PlayerLifecyclePolicy.onPause(
            isInPictureInPicture = true,
            pauseOnExit = true,
            backgroundAudio = false,
            isPlaying = true
        )

        assertEquals(false, decision.saveHistory)
        assertEquals(false, decision.pausePlayer)
        assertEquals(false, decision.unlockBeforePause)
        assertEquals(false, decision.stopPlaybackService)
        assertEquals(false, decision.startPlaybackService)
    }

    @Test
    fun resumeStopsPlaybackServiceAndRestartsObservation() {
        val decision = PlayerLifecyclePolicy.onResume()

        assertEquals(true, decision.stopPlaybackService)
        assertEquals(true, decision.observeState)
    }

    @Test
    fun systemDestroyKeepsActiveBackgroundPlaybackSession() {
        val decision = PlayerLifecyclePolicy.onDestroy(
            activityIsFinishing = false,
            exitRequested = false,
            backgroundAudio = true,
            hasPlayer = true,
            playWhenReady = true
        )

        assertEquals(true, decision.keepPlaybackSession)
        assertEquals(false, decision.releasePlayer)
        assertEquals(false, decision.dismissPlaybackNotification)
    }

    @Test
    fun explicitExitReleasesPlayerAndDismissesNotification() {
        val decision = PlayerLifecyclePolicy.onDestroy(
            activityIsFinishing = false,
            exitRequested = true,
            backgroundAudio = true,
            hasPlayer = true,
            playWhenReady = true
        )

        assertEquals(false, decision.keepPlaybackSession)
        assertEquals(true, decision.releasePlayer)
        assertEquals(true, decision.dismissPlaybackNotification)
    }

    @Test
    fun destroyWithoutBackgroundAudioReleasesPlayer() {
        val decision = PlayerLifecyclePolicy.onDestroy(
            activityIsFinishing = false,
            exitRequested = false,
            backgroundAudio = false,
            hasPlayer = true,
            playWhenReady = true
        )

        assertEquals(true, decision.releasePlayer)
    }

    @Test
    fun pausedBackgroundSessionDoesNotLeakAfterActivityDestroy() {
        val decision = PlayerLifecyclePolicy.onDestroy(
            activityIsFinishing = false,
            exitRequested = false,
            backgroundAudio = true,
            hasPlayer = true,
            playWhenReady = false
        )

        assertEquals(false, decision.keepPlaybackSession)
        assertEquals(true, decision.releasePlayer)
    }
}
