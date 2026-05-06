package com.example.openvideo.ui.player

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerOrientationPolicyTest {

    @Test
    fun portraitResolutionUsesPortraitOrientation() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            PlayerOrientationPolicy.orientationForVideo(width = 720, height = 1280)
        )
    }

    @Test
    fun landscapeResolutionUsesLandscapeOrientation() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            PlayerOrientationPolicy.orientationForVideo(width = 1280, height = 720)
        )
    }

    @Test
    fun squareishResolutionUsesSensorOrientation() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR,
            PlayerOrientationPolicy.orientationForVideo(width = 1000, height = 1000)
        )
    }

    @Test
    fun invalidResolutionFallsBackToDefaultOrientation() {
        assertEquals(
            PlayerOrientationPolicy.defaultOrientation(),
            PlayerOrientationPolicy.orientationForVideo(width = 0, height = 1280)
        )
    }

    @Test
    fun initialOrientationUsesVideoResolutionWhenAutoOrientationIsEnabled() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            PlayerOrientationPolicy.initialOrientationForVideo(
                width = 720,
                height = 1280,
                autoOrientationByVideo = true
            )
        )
    }

    @Test
    fun initialOrientationUsesDefaultWhenAutoOrientationIsDisabled() {
        assertEquals(
            PlayerOrientationPolicy.defaultOrientation(),
            PlayerOrientationPolicy.initialOrientationForVideo(
                width = 720,
                height = 1280,
                autoOrientationByVideo = false
            )
        )
    }
}
