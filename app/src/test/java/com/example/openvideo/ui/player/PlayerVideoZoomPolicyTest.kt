package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVideoZoomPolicyTest {

    @Test
    fun manualZoomAllowedForFitStyleAspectRatios() {
        assertTrue(PlayerVideoZoomPolicy.allowsManualZoom(AspectRatio.FIT))
        assertTrue(PlayerVideoZoomPolicy.allowsManualZoom(AspectRatio.RATIO_16_9))
        assertFalse(PlayerVideoZoomPolicy.allowsManualZoom(AspectRatio.FILL))
        assertFalse(PlayerVideoZoomPolicy.allowsManualZoom(AspectRatio.STRETCH))
    }

    @Test
    fun clampScaleWithinBounds() {
        assertEquals(1f, PlayerVideoZoomPolicy.clampScale(0.5f), 0.001f)
        assertEquals(4f, PlayerVideoZoomPolicy.clampScale(8f), 0.001f)
        assertEquals(2f, PlayerVideoZoomPolicy.clampScale(2f), 0.001f)
    }

    @Test
    fun applyScaleFactorMultipliesCurrentScale() {
        assertEquals(2f, PlayerVideoZoomPolicy.applyScaleFactor(1f, 2f), 0.001f)
        assertEquals(4f, PlayerVideoZoomPolicy.applyScaleFactor(2.5f, 2f), 0.001f)
    }

    @Test
    fun centerAnchorTranslationPullsContentBackWhenZoomed() {
        val anchor = PlayerVideoZoomPolicy.centerAnchorTranslation(
            baseScale = 1f,
            manualScale = 2f,
            frameWidth = 1000,
            frameHeight = 800
        )
        assertEquals(-500f, anchor.first, 0.001f)
        assertEquals(-400f, anchor.second, 0.001f)
    }

    @Test
    fun centerAnchorTranslationZeroWhenNotZoomed() {
        val anchor = PlayerVideoZoomPolicy.centerAnchorTranslation(
            baseScale = 2f,
            manualScale = 1f,
            frameWidth = 1080,
            frameHeight = 1920
        )
        assertEquals(0f, anchor.first, 0.001f)
        assertEquals(0f, anchor.second, 0.001f)
    }

    @Test
    fun panClampZeroWhenNotZoomed() {
        val pan = PlayerVideoZoomPolicy.clampPanOffset(
            panX = 120f,
            panY = -80f,
            baseScale = 1f,
            manualScale = 1f,
            frameWidth = 1080,
            frameHeight = 1920,
            viewportWidth = 1080,
            viewportHeight = 1920
        )
        assertEquals(0f, pan.first, 0.001f)
        assertEquals(0f, pan.second, 0.001f)
    }

    @Test
    fun panClampLimitsDragWhenZoomed() {
        val pan = PlayerVideoZoomPolicy.clampPanOffset(
            panX = 9999f,
            panY = -9999f,
            baseScale = 1f,
            manualScale = 2f,
            frameWidth = 1000,
            frameHeight = 800,
            viewportWidth = 1000,
            viewportHeight = 800
        )
        assertEquals(500f, pan.first, 0.001f)
        assertEquals(-400f, pan.second, 0.001f)
    }

    @Test
    fun composeTransformKeepsContentCenterOnViewportWhenZoomed() {
        val base = PlayerContentFrameTransform.IDENTITY
        val manual = PlayerVideoZoomState(scale = 2f)
        val composed = PlayerVideoZoomPolicy.composeTransform(
            base = base,
            manual = manual,
            frameWidth = 1000,
            frameHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000
        )
        assertEquals(2f, composed.scale, 0.001f)
        // center: 500 * 2 + tx = 500 -> tx = -500
        assertEquals(-500f, composed.translationX, 0.001f)
        assertEquals(-500f, composed.translationY, 0.001f)
    }

    @Test
    fun composeTransformMultipliesBaseScaleAndAddsPan() {
        val base = PlayerContentFrameTransform(scale = 2f, translationX = 10f, translationY = -5f)
        val manual = PlayerVideoZoomState(scale = 1.5f, panX = 20f, panY = 0f)
        val composed = PlayerVideoZoomPolicy.composeTransform(
            base = base,
            manual = manual,
            frameWidth = 1000,
            frameHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000
        )
        assertEquals(3f, composed.scale, 0.001f)
        // centerX = 1000 * 2 * (1 - 1.5) / 2 = -500
        assertEquals(-470f, composed.translationX, 0.001f)
        assertEquals(-505f, composed.translationY, 0.001f)
    }

    @Test
    fun isActiveWhenScaleOrPanChanged() {
        assertFalse(PlayerVideoZoomPolicy.isActive(PlayerVideoZoomState.IDENTITY))
        assertTrue(PlayerVideoZoomPolicy.isActive(PlayerVideoZoomState(scale = 1.2f)))
        assertTrue(PlayerVideoZoomPolicy.isActive(PlayerVideoZoomState(panX = 12f)))
    }
}
