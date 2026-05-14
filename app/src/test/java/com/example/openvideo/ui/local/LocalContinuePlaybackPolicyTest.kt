package com.example.openvideo.ui.local

import com.example.openvideo.data.local.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalContinuePlaybackPolicyTest {

    @Test
    fun selectsNewestHistoryEntryThatStillExistsInVisibleLibrary() {
        val history = listOf(
            history(videoId = 10, path = "/Movies/deleted.mp4", timestamp = 3000),
            history(videoId = 20, path = "/Movies/visible.mp4", timestamp = 2000)
        )

        val selected = LocalContinuePlaybackPolicy.latestPlayableVideoId(
            history = history,
            visibleVideoIds = setOf(20),
            visibleVideoPaths = setOf("/Movies/visible.mp4")
        )

        assertEquals(20L, selected)
    }

    @Test
    fun matchesByPathWhenMediaStoreIdChangedAfterRescan() {
        val history = listOf(history(videoId = 10, path = "/Movies/visible.mp4", timestamp = 3000))

        val selected = LocalContinuePlaybackPolicy.latestPlayableVideoId(
            history = history,
            visibleVideoIds = setOf(99),
            visibleVideoPaths = setOf("/Movies/visible.mp4")
        )

        assertEquals(10L, selected)
    }

    @Test
    fun returnsNullWhenHistoryOnlyContainsDeletedOrHiddenFiles() {
        val history = listOf(history(videoId = 10, path = "/Private/hidden.mp4", timestamp = 3000))

        val selected = LocalContinuePlaybackPolicy.latestPlayableVideoId(
            history = history,
            visibleVideoIds = emptySet(),
            visibleVideoPaths = emptySet()
        )

        assertNull(selected)
    }

    @Test
    fun skipsHistoryEntriesWithoutSavedResumePosition() {
        val history = listOf(
            history(videoId = 10, path = "/Movies/from-start.mp4", timestamp = 3000, lastPosition = 0),
            history(videoId = 20, path = "/Movies/resumable.mp4", timestamp = 2000, lastPosition = 1_000)
        )

        val selected = LocalContinuePlaybackPolicy.latestPlayableVideoId(
            history = history,
            visibleVideoIds = setOf(10, 20),
            visibleVideoPaths = setOf("/Movies/from-start.mp4", "/Movies/resumable.mp4")
        )

        assertEquals(20L, selected)
    }

    private fun history(
        videoId: Long,
        path: String,
        timestamp: Long,
        lastPosition: Long = 1_000
    ): HistoryEntity =
        HistoryEntity(
            videoId = videoId,
            title = "Video $videoId",
            path = path,
            duration = 60_000,
            lastPosition = lastPosition,
            timestamp = timestamp
        )
}
