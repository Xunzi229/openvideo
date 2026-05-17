package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerBackgroundServicePolicyTest {

    @Test
    fun startsServiceOnlyForBackgroundAudioPlaybackOutsideForegroundAndPip() {
        val decision = PlayerBackgroundServicePolicy.startDecision(
            backgroundAudio = true,
            isPlaying = true,
            isActivityForeground = false,
            isInPictureInPicture = false
        )

        assertEquals(true, decision.shouldStart)
    }

    @Test
    fun doesNotStartWhenBackgroundAudioIsDisabled() {
        val decision = PlayerBackgroundServicePolicy.startDecision(
            backgroundAudio = false,
            isPlaying = true,
            isActivityForeground = false,
            isInPictureInPicture = false
        )

        assertEquals(false, decision.shouldStart)
    }

    @Test
    fun doesNotStartWhenPlaybackIsNotActive() {
        val decision = PlayerBackgroundServicePolicy.startDecision(
            backgroundAudio = true,
            isPlaying = false,
            isActivityForeground = false,
            isInPictureInPicture = false
        )

        assertEquals(false, decision.shouldStart)
    }

    @Test
    fun doesNotStartWhileActivityIsForegroundOrInPip() {
        val foreground = PlayerBackgroundServicePolicy.startDecision(
            backgroundAudio = true,
            isPlaying = true,
            isActivityForeground = true,
            isInPictureInPicture = false
        )
        val pip = PlayerBackgroundServicePolicy.startDecision(
            backgroundAudio = true,
            isPlaying = true,
            isActivityForeground = false,
            isInPictureInPicture = true
        )

        assertEquals(false, foreground.shouldStart)
        assertEquals(false, pip.shouldStart)
    }

    @Test
    fun doesNotStartWhenPlaybackNotificationToggleIsDisabled() {
        val decision = PlayerBackgroundServicePolicy.startDecision(
            backgroundAudio = true,
            isPlaying = true,
            isActivityForeground = false,
            isInPictureInPicture = false,
            notificationEnabled = false
        )

        assertEquals(false, decision.shouldStart)
    }
}
