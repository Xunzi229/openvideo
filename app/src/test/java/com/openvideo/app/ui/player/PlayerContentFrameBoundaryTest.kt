package com.openvideo.app.ui.player

import org.junit.Assert.*
import org.junit.Test

class PlayerContentFrameBoundaryTest {
    @Test fun normalizedCropRejectsEachInvalidComponentAndEmptyAxis() {
        for (index in 0..3) for (value in listOf(-0.1f, 1.1f, Float.NaN)) {
            val parts = mutableListOf(0f, 0f, 1f, 1f).apply { set(index, value) }
            assertThrows(IllegalArgumentException::class.java) { NormalizedRect(parts[0], parts[1], parts[2], parts[3]) }
        }
        assertThrows(IllegalArgumentException::class.java) { NormalizedRect(1f, 0f, 1f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { NormalizedRect(0f, 1f, 1f, 1f) }
        val crop = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f)
        for (invalid in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) assertSame(crop, crop.expanded(invalid))
        assertEquals(NormalizedRect.FULL, crop.expanded(2f))
    }

    @Test fun eachInvalidDimensionAndAspectUsesSafeDefaults() {
        for (index in 0..3) {
            val sizes = mutableListOf(100, 100, 100, 100).apply { set(index, 0) }
            val rect = PlayerContentFramePolicy.fittedVideoRect(sizes[0], sizes[1], sizes[2], sizes[3])
            assertEquals(sizes[2].toFloat(), rect.width, 0f)
            assertEquals(sizes[3].toFloat(), rect.height, 0f)
        }
        assertEquals(NormalizedRect.FULL, PlayerContentFramePolicy.centerAspectBandCropRect(100, 0, 1f))
        for (aspect in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(NormalizedRect.FULL, PlayerContentFramePolicy.centerAspectBandCropRect(100, 100, aspect))
        }
        assertEquals(NormalizedRect.FULL, PlayerContentFramePolicy.centerAspectBandCropRect(100, 200, 0.25f))
        val changes = listOf(NormalizedRect(0.1f, 0f, 1f, 1f), NormalizedRect(0f, 0.1f, 1f, 1f),
            NormalizedRect(0f, 0f, 0.9f, 1f), NormalizedRect(0f, 0f, 1f, 0.9f))
        changes.forEach { assertFalse(PlayerContentFramePolicy.isFullFrameCrop(it)) }
    }

    @Test fun transformRejectsEmptyAxesAndClampsInvalidFillFractions() {
        val content = ContentFrameRect(0f, 0f, 50f, 25f)
        assertEquals(PlayerContentFrameTransform.IDENTITY, PlayerContentFramePolicy.transformToFillViewport(100, 0, content))
        for (invalid in listOf(content.copy(width = 0f), content.copy(height = 0f))) {
            assertEquals(PlayerContentFrameTransform.IDENTITY, PlayerContentFramePolicy.transformToFillViewport(100, 100, invalid))
        }
        for (fraction in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f, 2f)) {
            val transform = PlayerContentFramePolicy.transformToFillViewport(100, 100, content, fraction)
            assertEquals(4f, transform.scale, 0f)
            assertEquals(50f, transform.translationX + transform.scale * 25f, 0f)
            assertEquals(50f, transform.translationY + transform.scale * 12.5f, 0f)
        }
        val fit = PlayerContentFramePolicy.transformToFillViewport(100, 100, content, 0.5f, PlayerContentFrameViewportScale.FIT_INSIDE)
        assertEquals(1f, fit.scale, 0f)
    }
}
