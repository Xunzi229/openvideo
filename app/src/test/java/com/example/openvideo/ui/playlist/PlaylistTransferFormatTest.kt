package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistTransferFormatTest {

    @Test
    fun exportsPlaylistVideosAsM3uWithExtinfRows() {
        val text = PlaylistTransferFormat.exportM3u(
            videos = listOf(
                video(videoId = 2, title = "Second", path = "/Movies/b.mp4", duration = 65_000, position = 1),
                video(videoId = 1, title = "First", path = "/Movies/a.mp4", duration = 125_000, position = 0)
            )
        )

        assertEquals(
            """
            #EXTM3U
            #EXTINF:125,First
            /Movies/a.mp4
            #EXTINF:65,Second
            /Movies/b.mp4
            """.trimIndent(),
            text
        )
    }

    @Test
    fun parsesM3uIntoImportCandidates() {
        val result = PlaylistTransferFormat.parseM3u(
            """
            #EXTM3U
            #EXTINF:125,First
            /Movies/a.mp4
            #EXTINF:65,Second
            content://media/external/video/media/2
            """.trimIndent()
        )

        assertTrue(result is PlaylistTransferFormat.ParseResult.Success)
        val items = (result as PlaylistTransferFormat.ParseResult.Success).items
        assertEquals(2, items.size)
        assertEquals("First", items[0].title)
        assertEquals("/Movies/a.mp4", items[0].path)
        assertEquals(125_000L, items[0].durationMs)
        assertEquals(0, items[0].position)
        assertEquals("Second", items[1].title)
        assertEquals("content://media/external/video/media/2", items[1].path)
    }

    @Test
    fun exportsPlaylistVideosAsJsonDocument() {
        val text = PlaylistTransferFormat.exportJson(
            playlistName = "Favorites",
            videos = listOf(video(videoId = 1, title = "A \"quote\"", path = "/Movies/a.mp4", duration = 1000))
        )

        assertTrue(text.contains(""""schemaVersion":1"""))
        assertTrue(text.contains(""""playlistName":"Favorites""""))
        assertTrue(text.contains(""""title":"A \"quote\"""""))
        assertTrue(text.contains(""""path":"/Movies/a.mp4""""))
        assertTrue(text.contains(""""durationMs":1000"""))
    }

    @Test
    fun parsesJsonDocumentIntoImportCandidatesSortedByPosition() {
        val result = PlaylistTransferFormat.parseJson(
            """
            {
              "schemaVersion": 1,
              "playlistName": "Favorites",
              "videos": [
                {"title": "Second", "path": "/Movies/b.mp4", "durationMs": 2000, "position": 1},
                {"title": "First", "path": "/Movies/a.mp4", "durationMs": 1000, "position": 0}
              ]
            }
            """.trimIndent()
        )

        assertTrue(result is PlaylistTransferFormat.ParseResult.Success)
        val success = result as PlaylistTransferFormat.ParseResult.Success
        assertEquals("Favorites", success.playlistName)
        assertEquals(listOf("First", "Second"), success.items.map { it.title })
        assertEquals(listOf(0, 1), success.items.map { it.position })
    }

    @Test
    fun rejectsMalformedJsonAndEmptyImports() {
        val badJson = PlaylistTransferFormat.parseJson("{not-json")
        val emptyM3u = PlaylistTransferFormat.parseM3u("#EXTM3U\n# comment only")

        assertEquals(
            PlaylistTransferFormat.FailureReason.INVALID_JSON,
            (badJson as PlaylistTransferFormat.ParseResult.Failure).reason
        )
        assertEquals(
            PlaylistTransferFormat.FailureReason.EMPTY_PLAYLIST,
            (emptyM3u as PlaylistTransferFormat.ParseResult.Failure).reason
        )
    }

    private fun video(
        videoId: Long,
        title: String,
        path: String,
        duration: Long,
        position: Int = 0
    ): PlaylistVideoEntity =
        PlaylistVideoEntity(
            playlistId = 7,
            videoId = videoId,
            videoTitle = title,
            videoPath = path,
            videoDuration = duration,
            position = position
        )
}
