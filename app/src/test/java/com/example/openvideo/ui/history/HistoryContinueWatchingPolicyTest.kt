package com.example.openvideo.ui.history

import com.example.openvideo.data.local.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryContinueWatchingPolicyTest {

    @Test
    fun buildItemsIncludesProgressAndRecentWatchLabelsForAvailableFiles() {
        val items = HistoryContinueWatchingPolicy.buildItems(
            history = listOf(history(duration = 120_000, lastPosition = 30_000, timestamp = 3_540_000)),
            nowMs = 3_600_000,
            localFileExists = { true }
        )

        assertEquals(1, items.size)
        assertTrue(items[0].isAvailable)
        assertEquals("25%", items[0].progressLabel)
        assertEquals("1 min ago", items[0].watchedTimeLabel)
    }

    @Test
    fun buildItemsMarksMissingFilesButKeepsThemVisible() {
        val items = HistoryContinueWatchingPolicy.buildItems(
            history = listOf(history(path = "/missing.mp4")),
            nowMs = 10_000,
            localFileExists = { false }
        )

        assertEquals(1, items.size)
        assertFalse(items[0].isAvailable)
        assertEquals("Missing file", items[0].progressLabel)
    }

    @Test
    fun buildItemsShowsCompletedWhenResumeProgressIsReset() {
        val items = HistoryContinueWatchingPolicy.buildItems(
            history = listOf(history(duration = 90_000, lastPosition = 0)),
            nowMs = 10_000,
            localFileExists = { true }
        )

        assertEquals("Completed", items[0].progressLabel)
    }

    private fun history(
        path: String = "/video.mp4",
        duration: Long = 100_000,
        lastPosition: Long = 25_000,
        timestamp: Long = 1_000
    ) = HistoryEntity(
        videoId = 1L,
        title = "Video",
        path = path,
        duration = duration,
        lastPosition = lastPosition,
        timestamp = timestamp
    )
}
