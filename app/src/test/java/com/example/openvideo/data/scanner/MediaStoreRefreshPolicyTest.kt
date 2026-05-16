package com.example.openvideo.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreRefreshPolicyTest {

    @Test
    fun debounceDelayMatchesObserverCoalescingWindow() {
        assertEquals(500L, MediaStoreRefreshPolicy.debounceDelayMs())
    }

    @Test
    fun dropsRefreshWhileScanIsAlreadyInFlight() {
        assertTrue(MediaStoreRefreshPolicy.shouldDropRefreshWhileScanning(isScanInFlight = true))
        assertFalse(MediaStoreRefreshPolicy.shouldDropRefreshWhileScanning(isScanInFlight = false))
    }

    @Test
    fun publishesRefreshOnlyWhenSignalsExistAndScanIsIdle() {
        assertFalse(MediaStoreRefreshPolicy.shouldPublishRefresh(pendingRefreshSignals = 0, isScanInFlight = false))
        assertFalse(MediaStoreRefreshPolicy.shouldPublishRefresh(pendingRefreshSignals = 2, isScanInFlight = true))
        assertTrue(MediaStoreRefreshPolicy.shouldPublishRefresh(pendingRefreshSignals = 1, isScanInFlight = false))
    }

    @Test
    fun incrementalRefreshStartsAfterLibraryThreshold() {
        assertFalse(MediaStoreRefreshPolicy.shouldUseIncrementalRefresh(99))
        assertTrue(MediaStoreRefreshPolicy.shouldUseIncrementalRefresh(100))
    }

    @Test
    fun largeDiffFallsBackToFullScan() {
        val smallDiff = MediaStoreDiff(removedIds = setOf(1L), addedIds = setOf(2L), changedIds = emptySet())
        val largeDiff = MediaStoreDiff(
            removedIds = (1L..20L).toSet(),
            addedIds = (21L..40L).toSet(),
            changedIds = emptySet()
        )

        assertFalse(MediaStoreRefreshPolicy.shouldFallbackToFullScan(cachedCount = 100, diff = smallDiff))
        assertTrue(MediaStoreRefreshPolicy.shouldFallbackToFullScan(cachedCount = 100, diff = largeDiff))
        assertTrue(MediaStoreRefreshPolicy.shouldFallbackToFullScan(cachedCount = 0, diff = smallDiff))
    }
}
