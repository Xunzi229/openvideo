package com.example.openvideo.ui.player

import android.content.pm.ActivityInfo
import com.example.openvideo.core.prefs.AspectRatio
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerVideoLayoutPolicyTest {

    @Test
    fun unknownVideoDimensionsKeepSafeDefaults() {
        assertEquals(
            PlayerOrientationPolicy.defaultOrientation(),
            PlayerVideoLayoutPolicy.orientationForVideo(width = 0, height = 720)
        )
        assertEquals(
            0f,
            PlayerVideoLayoutPolicy.contentAspectRatio(
                preferredAspectRatio = AspectRatio.FIT,
                width = 0,
                height = 720
            )
        )
    }

    @Test
    fun portraitVideoChoosesPortraitOrientation() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            PlayerVideoLayoutPolicy.orientationForVideo(width = 720, height = 1280)
        )
    }

    @Test
    fun landscapeVideoChoosesLandscapeOrientation() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            PlayerVideoLayoutPolicy.orientationForVideo(width = 1280, height = 720)
        )
    }

    @Test
    fun rotationMetadataFlipsDisplayOrientationAndAspectRatioConsistently() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            PlayerVideoLayoutPolicy.orientationForVideo(
                width = 1080,
                height = 1920,
                unappliedRotationDegrees = 90
            )
        )
        assertEquals(
            1920f / 1080f,
            PlayerVideoLayoutPolicy.contentAspectRatio(
                preferredAspectRatio = AspectRatio.FIT,
                width = 1080,
                height = 1920,
                unappliedRotationDegrees = 90
            ),
            0.0001f
        )
    }

    @Test
    fun pixelAspectRatioFeedsOrientationAndContentRatioConsistently() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            PlayerVideoLayoutPolicy.orientationForVideo(
                width = 720,
                height = 576,
                pixelWidthHeightRatio = 16f / 15f
            )
        )
        assertEquals(
            4f / 3f,
            PlayerVideoLayoutPolicy.contentAspectRatio(
                preferredAspectRatio = AspectRatio.FIT,
                width = 720,
                height = 576,
                pixelWidthHeightRatio = 16f / 15f
            ),
            0.0001f
        )
    }

    @Test
    fun fixedAspectPreferenceOverridesDecodedVideoShape() {
        assertEquals(
            4f / 3f,
            PlayerVideoLayoutPolicy.contentAspectRatio(
                preferredAspectRatio = AspectRatio.RATIO_4_3,
                width = 1920,
                height = 1080
            ),
            0.0001f
        )
    }
}
