package com.example.openvideo.data.scanner

object MediaStoreRefreshPolicy {

    const val DEBOUNCE_MS = 500L
    const val INCREMENTAL_THRESHOLD = 100
    const val FULL_REBUILD_CHANGE_RATIO = 0.35f
    const val SCAN_PROGRESS_EMIT_INTERVAL = 25

    fun debounceDelayMs(): Long = DEBOUNCE_MS

    fun shouldDropRefreshWhileScanning(isScanInFlight: Boolean): Boolean = isScanInFlight

    fun shouldPublishRefresh(
        pendingRefreshSignals: Int,
        isScanInFlight: Boolean
    ): Boolean = pendingRefreshSignals > 0 && !isScanInFlight

    fun shouldUseIncrementalRefresh(cachedCount: Int): Boolean = cachedCount >= INCREMENTAL_THRESHOLD

    fun shouldFallbackToFullScan(cachedCount: Int, diff: MediaStoreDiff): Boolean {
        if (cachedCount == 0) return true
        return diff.mutationCount.toFloat() / cachedCount > FULL_REBUILD_CHANGE_RATIO
    }

    fun shouldReportFullScanProgress(cachedCount: Int): Boolean = cachedCount == 0

    fun shouldEmitScanProgress(scannedCount: Int, lastEmittedCount: Int): Boolean =
        scannedCount == 1 || scannedCount - lastEmittedCount >= SCAN_PROGRESS_EMIT_INTERVAL
}
