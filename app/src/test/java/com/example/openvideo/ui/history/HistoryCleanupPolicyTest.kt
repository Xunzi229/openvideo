package com.example.openvideo.ui.history

import com.example.openvideo.data.local.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCleanupPolicyTest {

    @Test
    fun removesHistoryWhenFileIsMissingAndNotInLatestScan() {
        val stale = history(videoId = 9L, path = "/gone.mp4")
        val ids = HistoryCleanupPolicy.videoIdsToRemove(
            history = listOf(stale),
            scannedVideoIds = setOf(1L),
            scannedPaths = setOf("/kept.mp4"),
            localFileExists = { false }
        )

        assertEquals(listOf(9L), ids)
    }

    @Test
    fun keepsHistoryWhenLocalFileStillExists() {
        val ids = HistoryCleanupPolicy.videoIdsToRemove(
            history = listOf(history(videoId = 2L, path = "/offline.mp4")),
            scannedVideoIds = emptySet(),
            scannedPaths = emptySet(),
            localFileExists = { true }
        )

        assertTrue(ids.isEmpty())
    }

    @Test
    fun keepsHistoryWhenVideoStillAppearsInLatestScan() {
        val ids = HistoryCleanupPolicy.videoIdsToRemove(
            history = listOf(history(videoId = 3L, path = "/scan-only.mp4")),
            scannedVideoIds = setOf(3L),
            scannedPaths = emptySet(),
            localFileExists = { false }
        )

        assertTrue(ids.isEmpty())
    }

    @Test
    fun keepsHistoryWhenPathStillAppearsInLatestScan() {
        val ids = HistoryCleanupPolicy.videoIdsToRemove(
            history = listOf(history(videoId = 4L, path = "/Movies\\episode.mp4")),
            scannedVideoIds = emptySet(),
            scannedPaths = setOf("/Movies/episode.mp4"),
            localFileExists = { false }
        )

        assertTrue(ids.isEmpty())
    }

    private fun history(videoId: Long, path: String) = HistoryEntity(
        videoId = videoId,
        title = "Video",
        path = path,
        duration = 100_000,
        lastPosition = 10_000,
        timestamp = 1_000
    )
}
