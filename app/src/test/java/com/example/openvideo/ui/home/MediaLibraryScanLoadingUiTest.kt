package com.example.openvideo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryScanLoadingUiTest {

    @Test
    fun scanProgressDataClassHoldsCount() {
        val progress = MediaLibraryScanProgress(scannedCount = 125)

        assertEquals(125, progress.scannedCount)
    }
}
