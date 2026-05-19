package com.example.openvideo.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreVideoDimensionsPolicyTest {

    @Test
    fun landscapeVideoKeepsRawDimensions() {
        assertEquals(
            1920 to 1080,
            MediaStoreVideoDimensionsPolicy.displayDimensions(
                width = 1920,
                height = 1080,
                orientationDegrees = 0
            )
        )
    }

    @Test
    fun portraitPhoneRecordingSwapsDimensionsWhenOrientationIs90() {
        assertEquals(
            1080 to 1920,
            MediaStoreVideoDimensionsPolicy.displayDimensions(
                width = 1920,
                height = 1080,
                orientationDegrees = 90
            )
        )
    }

    @Test
    fun invalidDimensionsStayUnchanged() {
        assertEquals(
            0 to 1080,
            MediaStoreVideoDimensionsPolicy.displayDimensions(
                width = 0,
                height = 1080,
                orientationDegrees = 90
            )
        )
    }
}
