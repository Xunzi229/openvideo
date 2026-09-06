package com.openvideo.app.ui.player

import org.junit.Assert.*
import org.junit.Test

class PlayerBlackBorderBoundaryTest {
    @Test fun invalidDimensionsNeverSamplePixels() {
        for ((width, height) in listOf(0 to 10, 10 to 0)) {
            val empty = PlayerSmartCropBlackBorders(false, false, false, false)
            assertEquals(empty, PlayerSmartCropBlackBorderDetector.detect(width, height) { _, _ -> error("invalid frame sampled") })
            assertEquals(empty, PlayerSmartCropBlackBorderDetector.detectFromContentRect(width, height, 0, 0, 1, 1))
            assertNull(PlayerSmartCropBlackBorderDetector.detectContentBounds(width, height) { _, _ -> error("invalid frame sampled") })
        }
    }

    @Test fun fullyBlackFrameHasFourBordersButNoContentBounds() {
        assertEquals(PlayerSmartCropBlackBorders(true, true, true, true), PlayerSmartCropBlackBorderDetector.detect(10, 10) { _, _ -> true })
        assertNull(PlayerSmartCropBlackBorderDetector.detectContentBounds(10, 10) { _, _ -> true })
        assertNull(PlayerSmartCropBlackBorderDetector.detectContentBounds(2, 2) { _, _ -> false })
    }

    @Test fun geometricBorderThresholdIsInclusiveOnEverySide() {
        for (inset in listOf(7, 8, 10)) {
            val actual = PlayerSmartCropBlackBorderDetector.detectFromContentRect(100, 100, inset, inset, 100 - inset, 100 - inset)
            assertEquals(PlayerSmartCropBlackBorders(inset >= 8, inset >= 8, inset >= 8, inset >= 8), actual)
        }
        assertEquals(PlayerSmartCropBlackBorders(false, false, false, false), PlayerSmartCropBlackBorderDetector.detectFromContentRect(100, 100, -1, -1, 101, 101))
    }

    @Test fun contentBoundsRequireContinuousContentAlongBothAxes() {
        val bounds = PlayerSmartCropBlackBorderDetector.detectContentBounds(20, 20) { x, y -> x !in 3..15 || y !in 4..16 }!!
        assertEquals(PlayerSmartCropBlackBorderDetector.ContentBounds(3, 4, 16, 17), bounds)
        assertEquals(13, bounds.width)
        assertEquals(13, bounds.height)
        assertNull(PlayerSmartCropBlackBorderDetector.detectContentBounds(12, 12) { x, y -> x / 3 != y % 3 })
    }
}
