package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerSmartCropBlackBorderDetectorTest {

    @Test
    fun detectsAllEdgesWhenContentIsCenteredInsideLargeBlackCanvas() {
        val width = 100
        val height = 100
        val borders = PlayerSmartCropBlackBorderDetector.detect(width, height) { x, y ->
            x < 20 || x >= 80 || y < 20 || y >= 80
        }

        assertEquals(
            PlayerSmartCropBlackBorders(left = true, top = true, right = true, bottom = true),
            borders
        )
    }

    @Test
    fun doesNotDetectMissingHorizontalEdgeAsAllBlackBorders() {
        val width = 100
        val height = 100
        val borders = PlayerSmartCropBlackBorderDetector.detect(width, height) { x, y ->
            x < 20 || x >= 80 || y < 20
        }

        assertEquals(
            PlayerSmartCropBlackBorders(left = true, top = true, right = true, bottom = false),
            borders
        )
    }

    @Test
    fun ignoresThinBlackLines() {
        val width = 100
        val height = 100
        val borders = PlayerSmartCropBlackBorderDetector.detect(width, height) { x, y ->
            x < 2 || x >= 98 || y < 2 || y >= 98
        }

        assertEquals(
            PlayerSmartCropBlackBorders(left = false, top = false, right = false, bottom = false),
            borders
        )
    }

    @Test
    fun detectsAllEdgesFromCenteredContentGeometry() {
        val borders = PlayerSmartCropBlackBorderDetector.detectFromContentRect(
            viewportWidth = 2000,
            viewportHeight = 1000,
            contentLeft = 800,
            contentTop = 180,
            contentRight = 1200,
            contentBottom = 700
        )

        assertEquals(
            PlayerSmartCropBlackBorders(left = true, top = true, right = true, bottom = true),
            borders
        )
    }

    @Test
    fun geometryRequiresEveryEdgeToHaveEnoughMargin() {
        val borders = PlayerSmartCropBlackBorderDetector.detectFromContentRect(
            viewportWidth = 2000,
            viewportHeight = 1000,
            contentLeft = 10,
            contentTop = 180,
            contentRight = 1200,
            contentBottom = 700
        )

        assertEquals(
            PlayerSmartCropBlackBorders(left = false, top = true, right = true, bottom = true),
            borders
        )
    }

    @Test
    fun detectsNonBlackContentBounds() {
        val bounds = PlayerSmartCropBlackBorderDetector.detectContentBounds(100, 80) { x, y ->
            x < 20 || x >= 70 || y < 10 || y >= 60
        }

        assertEquals(
            PlayerSmartCropBlackBorderDetector.ContentBounds(
                left = 20,
                top = 10,
                right = 70,
                bottom = 60
            ),
            bounds
        )
    }

    @Test
    fun contentBoundsIgnoreSparseCompressionNoiseInBlackBorder() {
        val bounds = PlayerSmartCropBlackBorderDetector.detectContentBounds(100, 80) { x, y ->
            val insideContent = x in 20 until 70 && y in 10 until 60
            val sparseBorderNoise = (x + y) % 97 == 0
            !insideContent && !sparseBorderNoise
        }

        assertEquals(
            PlayerSmartCropBlackBorderDetector.ContentBounds(
                left = 20,
                top = 10,
                right = 70,
                bottom = 60
            ),
            bounds
        )
    }

    @Test
    fun contentBoundsReturnsNullForAllBlackFrame() {
        val bounds = PlayerSmartCropBlackBorderDetector.detectContentBounds(100, 80) { _, _ -> true }

        assertNull(bounds)
    }
}
