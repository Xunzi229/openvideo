package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerSmartCropPolicyTest {

    @Test
    fun portraitCanvasSuggestsCenteredSixteenNineAndFitOverrideInLandscapeViewport() {
        val decision = PlayerSmartCropPolicy.quickToggleDecision(
            currentMode = ContentFrameMode.OFF,
            currentAspectRatio = AspectRatio.STRETCH,
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 2296,
            viewportHeight = 1080
        )

        assertEquals(ContentFrameMode.CENTER_16_9, decision.contentFrameMode)
        assertEquals(AspectRatio.FIT, decision.aspectRatioOverride)
        assertEquals(0.88f, decision.viewportFillFraction ?: 0f, 0.001f)
        assertEquals(PlayerContentFrameViewportScale.FIT_INSIDE, decision.viewportScale)
        assertEquals(0.25f, decision.cropExpansionFraction, 0.001f)
    }

    @Test
    fun portraitViewportHasNoSuggestion() {
        val decision = PlayerSmartCropPolicy.quickToggleDecision(
            currentMode = ContentFrameMode.OFF,
            currentAspectRatio = AspectRatio.FIT,
            sourceWidth = 720,
            sourceHeight = 1280,
            viewportWidth = 1080,
            viewportHeight = 2296
        )

        assertNull(decision.contentFrameMode)
        assertNull(decision.aspectRatioOverride)
        assertNull(decision.viewportFillFraction)
        assertNull(decision.viewportScale)
        assertEquals(0f, decision.cropExpansionFraction, 0.001f)
    }

    @Test
    fun standardLandscapeVideoHasNoSuggestion() {
        val decision = PlayerSmartCropPolicy.quickToggleDecision(
            currentMode = ContentFrameMode.OFF,
            currentAspectRatio = AspectRatio.FIT,
            sourceWidth = 1920,
            sourceHeight = 1080,
            viewportWidth = 2296,
            viewportHeight = 1080
        )

        assertNull(decision.contentFrameMode)
        assertNull(decision.aspectRatioOverride)
        assertNull(decision.viewportFillFraction)
        assertNull(decision.viewportScale)
        assertEquals(0f, decision.cropExpansionFraction, 0.001f)
    }

    @Test
    fun activeSmartCropTogglesOffWithoutAspectOverride() {
        val decision = PlayerSmartCropPolicy.quickToggleDecision(
            currentMode = ContentFrameMode.CENTER_16_9,
            currentAspectRatio = AspectRatio.FIT,
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 1080,
            viewportHeight = 2296
        )

        assertEquals(ContentFrameMode.OFF, decision.contentFrameMode)
        assertNull(decision.aspectRatioOverride)
        assertNull(decision.viewportFillFraction)
        assertNull(decision.viewportScale)
        assertEquals(0f, decision.cropExpansionFraction, 0.001f)
    }

    @Test
    fun restoredCenteredSixteenNineUsesSmartCropViewportInLandscape() {
        val decision = PlayerSmartCropPolicy.restoredViewportDecision(
            restoredMode = ContentFrameMode.CENTER_16_9,
            sourceWidth = 720,
            sourceHeight = 1280,
            viewportWidth = 2296,
            viewportHeight = 1080
        )

        assertEquals(0.88f, decision.viewportFillFraction ?: 0f, 0.001f)
        assertEquals(PlayerContentFrameViewportScale.FIT_INSIDE, decision.viewportScale)
        assertEquals(0.25f, decision.cropExpansionFraction, 0.001f)
    }

    @Test
    fun restoredCenteredSixteenNineUsesStandardViewportInPortrait() {
        val decision = PlayerSmartCropPolicy.restoredViewportDecision(
            restoredMode = ContentFrameMode.CENTER_16_9,
            sourceWidth = 720,
            sourceHeight = 1280,
            viewportWidth = 1080,
            viewportHeight = 2296
        )

        assertNull(decision.viewportFillFraction)
        assertNull(decision.viewportScale)
        assertEquals(0f, decision.cropExpansionFraction, 0.001f)
    }

    @Test
    fun invalidDimensionsHaveNoSuggestion() {
        val decision = PlayerSmartCropPolicy.quickToggleDecision(
            currentMode = ContentFrameMode.OFF,
            currentAspectRatio = AspectRatio.FIT,
            sourceWidth = 0,
            sourceHeight = 1920,
            viewportWidth = 2296,
            viewportHeight = 1080
        )

        assertNull(decision.contentFrameMode)
        assertNull(decision.aspectRatioOverride)
        assertNull(decision.viewportFillFraction)
        assertNull(decision.viewportScale)
        assertEquals(0f, decision.cropExpansionFraction, 0.001f)
    }
}
