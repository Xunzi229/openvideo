package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerContentFrameApplyPolicyTest {

    @Test
    fun resolveTransformZoomsPortraitNestedBand() {
        val transform = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatio = AspectRatio.FIT,
            sourceWidth = 1080,
            sourceHeight = 1920,
            viewportWidth = 1080,
            viewportHeight = 1920
        )
        assertTrue(transform.scale > 1f)
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
