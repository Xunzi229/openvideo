package com.example.openvideo.ui.player

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPipPolicyTest {

    @Test
    fun unsupportedSdkNeverEntersPip() {
        val decision = PlayerPipPolicy.enterDecision(
            sdkInt = Build.VERSION_CODES.N,
            supportsPictureInPicture = true,
            isAlreadyInPictureInPicture = false,
            videoWidth = 1920,
            videoHeight = 1080
        )

        assertFalse(decision.shouldEnter)
        assertEquals(null, decision.aspectRatio)
    }

    @Test
    fun unsupportedFeatureOrExistingPipSessionDoesNotReenter() {
        val missingFeature = PlayerPipPolicy.enterDecision(
            sdkInt = Build.VERSION_CODES.O,
            supportsPictureInPicture = false,
            isAlreadyInPictureInPicture = false,
            videoWidth = 1920,
            videoHeight = 1080
        )
        val alreadyInPip = PlayerPipPolicy.enterDecision(
            sdkInt = Build.VERSION_CODES.O,
            supportsPictureInPicture = true,
            isAlreadyInPictureInPicture = true,
            videoWidth = 1920,
            videoHeight = 1080
        )

        assertFalse(missingFeature.shouldEnter)
        assertFalse(alreadyInPip.shouldEnter)
    }

    @Test
    fun invalidDimensionsUseSafeFallbackAspectRatio() {
        val decision = PlayerPipPolicy.enterDecision(
            sdkInt = Build.VERSION_CODES.O,
            supportsPictureInPicture = true,
            isAlreadyInPictureInPicture = false,
            videoWidth = 0,
            videoHeight = 1080
        )

        assertTrue(decision.shouldEnter)
        assertEquals(PlayerPipAspectRatio(16, 9), decision.aspectRatio)
    }

    @Test
    fun validVideoDimensionsProduceDisplayAspectRatio() {
        val decision = PlayerPipPolicy.enterDecision(
            sdkInt = Build.VERSION_CODES.O,
            supportsPictureInPicture = true,
            isAlreadyInPictureInPicture = false,
            videoWidth = 1080,
            videoHeight = 1920,
            unappliedRotationDegrees = 90
        )

        assertTrue(decision.shouldEnter)
        assertEquals(PlayerPipAspectRatio(1777, 1000), decision.aspectRatio)
    }

    @Test
    fun exposesSixteenByNineFallbackConstant() {
        assertEquals(PlayerPipAspectRatio(16, 9), PlayerPipPolicy.FALLBACK_ASPECT_RATIO)
    }
}
