package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistInsertionTest {

    @Test
    fun returnsNullWhenVideoAlreadyExistsInPlaylist() {
        val video = sampleVideo(id = 7)
        val existing = listOf(samplePlaylistVideo(videoId = 7, position = 0))

        val result = PlaylistInsertion.createEntry(playlistId = 1, existing = existing, video = video)

        assertNull(result)
    }

    @Test
    fun appendsNewVideoAtNextPosition() {
        val video = sampleVideo(id = 9)
        val existing = listOf(
            samplePlaylistVideo(videoId = 3, position = 0),
            samplePlaylistVideo(videoId = 4, position = 1)
        )

        val result = PlaylistInsertion.createEntry(playlistId = 12, existing = existing, video = video)

        assertEquals(
            PlaylistVideoEntity(
                playlistId = 12,
                videoId = 9,
                videoTitle = "Video 9",
                videoPath = "content://video/9",
                videoDuration = 90_000,
                position = 2
            ),
            result
        )
    }

    private fun sampleVideo(id: Long): PlaylistInsertion.VideoInput {
        return PlaylistInsertion.VideoInput(
            id = id,
            title = "Video $id",
            uri = "content://video/$id",
            duration = 90_000
        )
    }

    private fun samplePlaylistVideo(videoId: Long, position: Int): PlaylistVideoEntity {
        return PlaylistVideoEntity(
            playlistId = 1,
            videoId = videoId,
            videoTitle = "Existing $videoId",
            videoPath = "content://video/$videoId",
            videoDuration = 1_000,
            position = position
        )
    }
}
