package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerContentFrameApplyPolicyTest {

    @Test
    fun resolveTransformKeepsPortraitViewportUncropped() {
        val transform = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 1080,
            viewportHeight = 1920
        )
        assertEquals(PlayerContentFrameTransform.IDENTITY, transform)
    }

    @Test
    fun resolveTransformCanLeaveSmartCropMargins() {
        val full = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 1920,
            viewportHeight = 1080
        )
        val softened = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 1920,
            viewportHeight = 1080,
            viewportFillFraction = 0.88f
        )

        assertEquals(full.scale * 0.88f, softened.scale, 0.001f)
        assertTrue(softened.scale < full.scale)
    }

    @Test
    fun resolveTransformCanFitSmartCropInsideViewport() {
        val fillMargins = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 720,
            sourceHeight = 1280,
            viewportWidth = 2296,
            viewportHeight = 1080,
            viewportFillFraction = 0.88f
        )
        val fitInsideMargins = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 720,
            sourceHeight = 1280,
            viewportWidth = 2296,
            viewportHeight = 1080,
            viewportFillFraction = 0.88f,
            viewportScale = PlayerContentFrameViewportScale.FIT_INSIDE
        )
        val content = PlayerContentFramePolicy.contentFrameInViewport(
            fitted = PlayerContentFramePolicy.fittedVideoRect(
                sourceWidth = 720,
                sourceHeight = 1280,
                viewportWidth = 2296,
                viewportHeight = 1080
            ),
            crop = PlayerContentFramePolicy.cropForMode(
                mode = ContentFrameMode.CENTER_16_9,
                sourceWidth = 720,
                sourceHeight = 1280
            )
        )

        assertTrue(fitInsideMargins.scale < fillMargins.scale)
        assertTrue(content.width * fitInsideMargins.scale <= 2296f * 0.88f + 0.001f)
        assertTrue(content.height * fitInsideMargins.scale <= 1080f * 0.88f + 0.001f)
    }

    @Test
    fun smartCropTransformCentersContentWithinAlreadyFittedContentFrame() {
        val transform = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 720,
            sourceHeight = 1280,
            viewportWidth = 2296,
            viewportHeight = 1080,
            viewportFillFraction = 0.88f,
            viewportScale = PlayerContentFrameViewportScale.FIT_INSIDE
        )
        val fitted = PlayerContentFramePolicy.fittedVideoRect(
            sourceWidth = 720,
            sourceHeight = 1280,
            viewportWidth = 2296,
            viewportHeight = 1080
        )
        val crop = PlayerContentFramePolicy.cropForMode(
            mode = ContentFrameMode.CENTER_16_9,
            sourceWidth = 720,
            sourceHeight = 1280
        )
        val localCropCenterX = crop.left * fitted.width + crop.widthFraction * fitted.width / 2f
        val localCropCenterY = crop.top * fitted.height + crop.heightFraction * fitted.height / 2f
        val screenCenterX = fitted.left + transform.translationX + transform.scale * localCropCenterX
        val screenCenterY = fitted.top + transform.translationY + transform.scale * localCropCenterY

        assertEquals(2296f / 2f, screenCenterX, 0.001f)
        assertEquals(1080f / 2f, screenCenterY, 0.001f)
    }

    @Test
    fun smartCropExpansionKeepsMoreVerticalContentForEmbeddedSubtitles() {
        val tight = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 720,
            sourceHeight = 1280,
            viewportWidth = 2296,
            viewportHeight = 1080,
            viewportFillFraction = 0.88f,
            viewportScale = PlayerContentFrameViewportScale.FIT_INSIDE
        )
        val relaxed = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 720,
            sourceHeight = 1280,
            viewportWidth = 2296,
            viewportHeight = 1080,
            viewportFillFraction = 0.88f,
            viewportScale = PlayerContentFrameViewportScale.FIT_INSIDE,
            cropExpansionFraction = 0.25f
        )

        assertTrue(relaxed.scale < tight.scale)
        assertTrue(relaxed.translationY > tight.translationY)
    }

    @Test
    fun resolveTransformIdentityWhenFillBlocksContentFrame() {
        val transform = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FILL,
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 1080,
            viewportHeight = 1920
        )
        assertEquals(PlayerContentFrameTransform.IDENTITY, transform)
    }

    @Test
    fun resolveTransformWithManualZoomComposesOnTopOfBase() {
        val transform = PlayerContentFrameApplyPolicy.resolveTransformWithManualZoom(
            contentFrameMode = ContentFrameMode.OFF,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 1920,
            sourceHeight = 1080,
            viewportWidth = 1080,
            viewportHeight = 1920,
            manualZoom = PlayerVideoZoomState(scale = 2f),
            frameWidth = 1080,
            frameHeight = 1920
        )
        assertEquals(2f, transform.scale, 0.001f)
        assertEquals(-540f, transform.translationX, 0.001f)
        assertEquals(-960f, transform.translationY, 0.001f)
    }
}
