package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistReorderPolicyTest {

    @Test
    fun movePlacesItemAtTargetIndexAndNormalizesPositions() {
        val result = PlaylistReorderPolicy.move(
            videos = listOf(
                video(videoId = 1, position = 0),
                video(videoId = 2, position = 1),
                video(videoId = 3, position = 2)
            ),
            fromIndex = 0,
            toIndex = 2
        )

        assertEquals(listOf(2L, 3L, 1L), result.map { it.videoId })
        assertEquals(listOf(0, 1, 2), result.map { it.position })
    }

    @Test
    fun moveClampsIndexesIntoListBounds() {
        val result = PlaylistReorderPolicy.move(
            videos = listOf(
                video(videoId = 1, position = 10),
                video(videoId = 2, position = 20),
                video(videoId = 3, position = 30)
            ),
            fromIndex = -10,
            toIndex = 50
        )

        assertEquals(listOf(2L, 3L, 1L), result.map { it.videoId })
        assertEquals(listOf(0, 1, 2), result.map { it.position })
    }

    @Test
    fun moveReturnsPositionNormalizedListWhenSourceAndTargetMatch() {
        val result = PlaylistReorderPolicy.move(
            videos = listOf(
                video(videoId = 1, position = 10),
                video(videoId = 2, position = 20)
            ),
            fromIndex = 1,
            toIndex = 1
        )

        assertEquals(listOf(1L, 2L), result.map { it.videoId })
        assertEquals(listOf(0, 1), result.map { it.position })
    }

    private fun video(videoId: Long, position: Int): PlaylistVideoEntity =
        PlaylistVideoEntity(
            playlistId = 7,
            videoId = videoId,
            videoTitle = "Video $videoId",
            videoPath = "content://video/$videoId",
            videoDuration = 60_000,
            position = position
        )
}
