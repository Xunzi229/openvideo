package com.example.openvideo.data.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreScanProgressPolicyTest {

    @Test
    fun fullScanProgressOnlyOnFirstLoad() {
        assertTrue(MediaStoreRefreshPolicy.shouldReportFullScanProgress(cachedCount = 0))
        assertFalse(MediaStoreRefreshPolicy.shouldReportFullScanProgress(cachedCount = 100))
    }

    @Test
    fun progressEmitsOnFirstItemAndInterval() {
        assertTrue(MediaStoreRefreshPolicy.shouldEmitScanProgress(scannedCount = 1, lastEmittedCount = 0))
        assertFalse(MediaStoreRefreshPolicy.shouldEmitScanProgress(scannedCount = 10, lastEmittedCount = 1))
        assertTrue(MediaStoreRefreshPolicy.shouldEmitScanProgress(scannedCount = 26, lastEmittedCount = 1))
    }
}
