package com.example.openvideo.ui.sources

import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.local.NetworkRecentItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRecentPlaybackPolicyTest {

    @Test
    fun buildItemsMergesLocalAndNetworkRecentPlaybackByLastPlayedTime() {
        val items = SourceRecentPlaybackPolicy.buildItems(
            history = listOf(localHistory(timestamp = 2_000L)),
            networkRecent = listOf(networkRecent(lastPlayedAt = 3_000L)),
            labels = SourceRecentPlaybackLabels.englishDefaults(),
            maxItems = 10,
            localFileExists = { true }
        )

        assertEquals(2, items.size)
        assertEquals(SourceRecentPlaybackType.NETWORK_URL, items[0].type)
        assertEquals("URL", items[0].sourceLabel)
        assertEquals("https://example.com/video.mp4?token=***", items[0].detailLabel)
        assertEquals(SourceRecentPlaybackType.LOCAL, items[1].type)
        assertEquals("Local", items[1].sourceLabel)
        assertEquals("25%", items[1].detailLabel)
    }

    @Test
    fun buildItemsMarksMissingLocalFilesButKeepsNetworkUrlsPlayable() {
        val items = SourceRecentPlaybackPolicy.buildItems(
            history = listOf(localHistory(path = "/missing.mp4", timestamp = 4_000L)),
            networkRecent = listOf(networkRecent(lastPlayedAt = 3_000L)),
            labels = SourceRecentPlaybackLabels.englishDefaults(),
            maxItems = 10,
            localFileExists = { false }
        )

        assertEquals(2, items.size)
        assertFalse(items[0].isPlayable)
        assertEquals("Missing file", items[0].detailLabel)
        assertTrue(items[1].isPlayable)
    }

    @Test
    fun buildItemsLimitsCombinedRecentPlaybackRows() {
        val items = SourceRecentPlaybackPolicy.buildItems(
            history = listOf(localHistory(videoId = 1L, timestamp = 1_000L)),
            networkRecent = listOf(
                networkRecent(recentId = 1L, lastPlayedAt = 3_000L),
                networkRecent(recentId = 2L, lastPlayedAt = 2_000L)
            ),
            labels = SourceRecentPlaybackLabels.englishDefaults(),
            maxItems = 2,
            localFileExists = { true }
        )

        assertEquals(listOf("network:1", "network:2"), items.map { it.stableId })
    }

    @Test
    fun buildItemsDoesNotDuplicateNetworkHistoryAsLocalRecentPlayback() {
        val items = SourceRecentPlaybackPolicy.buildItems(
            history = listOf(
                localHistory(
                    videoId = 99L,
                    path = "https://example.com/video.mp4",
                    timestamp = 4_000L
                )
            ),
            networkRecent = listOf(networkRecent(recentId = 1L, lastPlayedAt = 3_000L)),
            labels = SourceRecentPlaybackLabels.englishDefaults(),
            maxItems = 10,
            localFileExists = { false }
        )

        assertEquals(1, items.size)
        assertEquals(SourceRecentPlaybackType.NETWORK_URL, items[0].type)
    }

    private fun localHistory(
        videoId: Long = 10L,
        path: String = "/video.mp4",
        timestamp: Long = 1_000L
    ) = HistoryEntity(
        videoId = videoId,
        title = "Local video",
        path = path,
        duration = 100_000L,
        lastPosition = 25_000L,
        timestamp = timestamp
    )

    private fun networkRecent(
        recentId: Long = 20L,
        lastPlayedAt: Long = 1_000L
    ) = NetworkRecentItemEntity(
        recentId = recentId,
        uri = "https://example.com/video.mp4?token=secret",
        normalizedUrl = "https://example.com/video.mp4?token=secret",
        displayUrl = "https://example.com/video.mp4?token=***",
        title = "Network video",
        lastPlayedAt = lastPlayedAt
    )
}
