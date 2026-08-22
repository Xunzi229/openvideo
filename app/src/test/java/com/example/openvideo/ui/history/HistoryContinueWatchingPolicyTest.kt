package com.example.openvideo.ui.history

import com.example.openvideo.data.local.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryContinueWatchingPolicyTest {

    @Test
    fun buildItemsIncludesProgressAndRecentWatchLabelsForAvailableFiles() {
        val labels = HistoryContinueWatchingLabels.englishDefaults()
        val items = HistoryContinueWatchingPolicy.buildItems(
            history = listOf(history(duration = 120_000, lastPosition = 30_000, timestamp = 3_540_000)),
            labels = labels,
            nowMs = 3_600_000,
            isAvailable = { true }
        )

        assertEquals(1, items.size)
        assertTrue(items[0].isAvailable)
        assertEquals("25%", items[0].progressLabel)
        assertEquals("1 min ago", items[0].watchedTimeLabel)
    }

    @Test
    fun buildItemsMarksMissingFilesButKeepsThemVisible() {
        val labels = HistoryContinueWatchingLabels.englishDefaults()
        val items = HistoryContinueWatchingPolicy.buildItems(
            history = listOf(history(path = "/missing.mp4")),
            labels = labels,
            nowMs = 10_000,
            isAvailable = { false }
        )

        assertEquals(1, items.size)
        assertFalse(items[0].isAvailable)
        assertEquals("Missing file", items[0].progressLabel)
    }

    @Test
    fun buildItemsShowsCompletedWhenResumeProgressIsReset() {
        val labels = HistoryContinueWatchingLabels.englishDefaults()
        val items = HistoryContinueWatchingPolicy.buildItems(
            history = listOf(history(duration = 90_000, lastPosition = 0)),
            labels = labels,
            nowMs = 10_000,
            isAvailable = { true }
        )

        assertEquals("Completed", items[0].progressLabel)
    }

    @Test
    fun buildItemsKeepsLibraryVideosAvailableWhenCheckerUsesVideoId() {
        val labels = HistoryContinueWatchingLabels.englishDefaults()
        val items = HistoryContinueWatchingPolicy.buildItems(
            history = listOf(history(path = "content://media/external/video/media/9")),
            labels = labels,
            nowMs = 10_000,
            isAvailable = { entity -> entity.videoId == 1L }
        )

        assertTrue(items[0].isAvailable)
        assertEquals("25%", items[0].progressLabel)
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
