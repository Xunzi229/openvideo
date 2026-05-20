package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistVideoAvailabilityPolicyTest {

    @Test
    fun staleIdsExcludeMissingLocalFiles() {
        val entities = listOf(
            sample(path = "/missing-${System.nanoTime()}.mp4", id = 1),
            sample(path = "content://media/external/video/media/9", id = 2)
        )
        assertEquals(listOf(1L), PlaylistVideoAvailabilityPolicy.staleVideoIds(entities))
    }

    @Test
    fun filterPlayableKeepsContentUris() {
        val entities = listOf(
            sample(path = "/gone-${System.nanoTime()}.mp4", id = 1),
            sample(path = "content://media/external/video/media/9", id = 2)
        )
        val playable = PlaylistVideoAvailabilityPolicy.filterPlayable(entities)
        assertEquals(1, playable.size)
        assertEquals(2L, playable.first().videoId)
    }

    private fun sample(path: String, id: Long) = PlaylistVideoEntity(
        playlistId = 1,
        videoId = id,
        videoTitle = "t",
        videoPath = path,
        videoDuration = 1000,
        position = 0
    )
}
