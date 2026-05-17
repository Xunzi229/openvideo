package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerContentFramePolicyTest {

    @Test
    fun centerBandCropRectForPortraitNestedLandscape() {
        val crop = PlayerContentFramePolicy.centerAspectBandCropRect(
            sourceWidth = 1080,
            sourceHeight = 1920,
            contentAspectRatio = 16f / 9f
        )
        assertEquals(0f, crop.left, 0.001f)
        assertEquals(1f, crop.right, 0.001f)
        assertEquals(0.342f, crop.top, 0.01f)
        assertEquals(0.658f, crop.bottom, 0.01f)
    }

    @Test
    fun centerBandCropRectForLandscapeNestedFourThree() {
        val crop = PlayerContentFramePolicy.centerAspectBandCropRect(
            sourceWidth = 1920,
            sourceHeight = 1080,
            contentAspectRatio = 4f / 3f
        )
        assertEquals(0.125f, crop.left, 0.01f)
        assertEquals(0.875f, crop.right, 0.01f)
        assertEquals(0f, crop.top, 0.001f)
        assertEquals(1f, crop.bottom, 0.001f)
    }

    @Test
    fun centerBandReturnsFullFrameWhenContentMatchesSource() {
        val crop = PlayerContentFramePolicy.centerAspectBandCropRect(
            sourceWidth = 1920,
            sourceHeight = 1080,
            contentAspectRatio = 16f / 9f
        )
        assertTrue(PlayerContentFramePolicy.isFullFrameCrop(crop))
    }

    @Test
    fun fittedVideoRectLetterboxesLandscapeVideoInPortraitViewport() {
        val fitted = PlayerContentFramePolicy.fittedVideoRect(
            sourceWidth = 1920,
            sourceHeight = 1080,
            viewportWidth = 1080,
            viewportHeight = 1920
        )
        assertEquals(1080f, fitted.width, 0.1f)
        assertEquals(607.5f, fitted.height, 0.5f)
        assertEquals(656.25f, fitted.top, 0.5f)
        assertEquals(0f, fitted.left, 0.1f)
    }

    @Test
    fun contentFrameInViewportMapsNormalizedCropUnderFit() {
        val fitted = PlayerContentFramePolicy.fittedVideoRect(
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 1080,
            viewportHeight = 1920
        )
        val crop = PlayerContentFramePolicy.centerAspectBandCropRect(
            sourceWidth = 1080,
            sourceHeight = 1920,
            contentAspectRatio = 16f / 9f
        )
        val content = PlayerContentFramePolicy.contentFrameInViewport(fitted, crop)

        assertEquals(0f, content.left, 0.5f)
        assertEquals(1080f, content.width, 0.5f)
        assertEquals(607.5f, content.height, 1f)
    }

    @Test
    fun transformToFillViewportZoomsLetterboxedBand() {
        val fitted = PlayerContentFramePolicy.fittedVideoRect(
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 1080,
            viewportHeight = 1920
        )
        val crop = PlayerContentFramePolicy.centerAspectBandCropRect(
            sourceWidth = 1080,
            sourceHeight = 1920,
            contentAspectRatio = 16f / 9f
        )
        val content = PlayerContentFramePolicy.contentFrameInViewport(fitted, crop)
        val transform = PlayerContentFramePolicy.transformToFillViewport(
            viewportWidth = 1080,
            viewportHeight = 1920,
            content = content
        )

        assertTrue(transform.scale > 2.5f)
        assertEquals(1080 / 2f, transform.translationX + transform.scale * (content.left + content.width / 2f), 1f)
        assertEquals(1920 / 2f, transform.translationY + transform.scale * (content.top + content.height / 2f), 1f)
    }

    @Test
    fun resolveTransformIsIdentityForFitFullOrFullCrop() {
        val crop = PlayerContentFramePolicy.centerAspectBandCropRect(
            sourceWidth = 1080,
            sourceHeight = 1920,
            contentAspectRatio = 16f / 9f
        )
        assertEquals(
            PlayerContentFrameTransform.IDENTITY,
            PlayerContentFramePolicy.resolveTransform(
                mode = PlayerContentFrameMode.FIT_FULL,
                sourceWidth = 1080,
                sourceHeight = 1920,
                viewportWidth = 1080,
                viewportHeight = 1920,
                crop = crop
            )
        )
        assertEquals(
            PlayerContentFrameTransform.IDENTITY,
            PlayerContentFramePolicy.resolveTransform(
                mode = PlayerContentFrameMode.ZOOM_CROP,
                sourceWidth = 1920,
                sourceHeight = 1080,
                viewportWidth = 1920,
                viewportHeight = 1080,
                crop = NormalizedRect.FULL
            )
        )
    }

    @Test
    fun resolveTransformZoomsNestedBandInZoomCropMode() {
        val crop = PlayerContentFramePolicy.centerAspectBandCropRect(
            sourceWidth = 1080,
            sourceHeight = 1920,
            contentAspectRatio = 16f / 9f
        )
        val transform = PlayerContentFramePolicy.resolveTransform(
            mode = PlayerContentFrameMode.ZOOM_CROP,
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 1080,
            viewportHeight = 1920,
            crop = crop
        )
        assertTrue(transform.scale > 1f)
    }

    @Test
    fun contentFrameAdjustmentAllowedOnlyForFitStyleRatios() {
        assertTrue(PlayerContentFramePolicy.allowsContentFrameAdjustment(AspectRatio.FIT))
        assertTrue(PlayerContentFramePolicy.allowsContentFrameAdjustment(AspectRatio.RATIO_16_9))
        assertFalse(PlayerContentFramePolicy.allowsContentFrameAdjustment(AspectRatio.FILL))
        assertFalse(PlayerContentFramePolicy.allowsContentFrameAdjustment(AspectRatio.CROP))
        assertFalse(PlayerContentFramePolicy.allowsContentFrameAdjustment(AspectRatio.STRETCH))
    }

    @Test
    fun effectiveModeDisablesWhenAspectRatioIncompatible() {
        assertEquals(
            ContentFrameMode.OFF,
            PlayerContentFramePolicy.effectiveMode(ContentFrameMode.CENTER_16_9, AspectRatio.FILL)
        )
        assertEquals(
            ContentFrameMode.CENTER_16_9,
            PlayerContentFramePolicy.effectiveMode(ContentFrameMode.CENTER_16_9, AspectRatio.FIT)
        )
    }

    @Test
    fun playbackPlanZoomsPortraitNestedLandscape() {
        val plan = PlayerContentFramePolicy.playbackPlan(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 1080,
            sourceHeight = 1920
        )
        assertEquals(ContentFrameMode.CENTER_16_9, plan.frameMode)
        assertEquals(PlayerContentFrameMode.ZOOM_CROP, plan.transformMode)
        assertFalse(PlayerContentFramePolicy.isFullFrameCrop(plan.crop))
    }

    @Test
    fun playbackPlanStaysOffWhenModeDisabled() {
        val plan = PlayerContentFramePolicy.playbackPlan(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.CROP,
            sourceWidth = 1080,
            sourceHeight = 1920
        )
        assertEquals(ContentFrameMode.OFF, plan.frameMode)
        assertEquals(PlayerContentFrameMode.FIT_FULL, plan.transformMode)
    }

    @Test
    fun invalidDimensionsReturnSafeDefaults() {
        val fitted = PlayerContentFramePolicy.fittedVideoRect(0, 0, 1080, 1920)
        assertEquals(1080f, fitted.width, 0.1f)
        assertEquals(1920f, fitted.height, 0.1f)

        assertEquals(
            PlayerContentFrameTransform.IDENTITY,
            PlayerContentFramePolicy.transformToFillViewport(0, 0, ContentFrameRect(0f, 0f, 0f, 0f))
        )
        assertTrue(
            PlayerContentFramePolicy.isFullFrameCrop(
                PlayerContentFramePolicy.centerAspectBandCropRect(0, 720, 16f / 9f)
            )
        )
    }
}
