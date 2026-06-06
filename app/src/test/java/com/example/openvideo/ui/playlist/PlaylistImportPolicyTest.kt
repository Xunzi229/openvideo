package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistImportPolicyTest {

    @Test
    fun createsPlaylistRowsForNewImportCandidatesWithContinuousPositions() {
        val rows = PlaylistImportPolicy.createRows(
            playlistId = 9,
            existing = listOf(video(videoId = 10, path = "/Movies/existing.mp4", position = 0)),
            candidates = listOf(
                candidate(title = "First", path = "/Movies/a.mp4", durationMs = 1000, position = 5),
                candidate(title = "Second", path = "/Movies/b.mp4", durationMs = 2000, position = 3)
            )
        )

        assertEquals(listOf("Second", "First"), rows.map { it.videoTitle })
        assertEquals(listOf(-1L, -2L), rows.map { it.videoId })
        assertEquals(listOf(1, 2), rows.map { it.position })
    }

    @Test
    fun skipsBlankPathsDuplicateImportsAndExistingPaths() {
        val rows = PlaylistImportPolicy.createRows(
            playlistId = 9,
            existing = listOf(video(videoId = 10, path = "/Movies/existing.mp4", position = 0)),
            candidates = listOf(
                candidate(title = "Existing", path = "/Movies/existing.mp4", durationMs = 1000, position = 0),
                candidate(title = "Blank", path = " ", durationMs = 1000, position = 1),
                candidate(title = "First", path = "/Movies/a.mp4", durationMs = 1000, position = 2),
                candidate(title = "First Duplicate", path = "/Movies/a.mp4", durationMs = 1000, position = 3)
            )
        )

        assertEquals(1, rows.size)
        assertEquals("First", rows.single().videoTitle)
        assertEquals(1, rows.single().position)
    }

    @Test
    fun allocatesTemporaryIdsBelowExistingNegativeIds() {
        val rows = PlaylistImportPolicy.createRows(
            playlistId = 9,
            existing = listOf(video(videoId = -3, path = "/Movies/imported-before.mp4", position = 0)),
            candidates = listOf(
                candidate(title = "Next", path = "/Movies/next.mp4", durationMs = 1000, position = 0)
            )
        )

        assertEquals(-4L, rows.single().videoId)
    }

    private fun candidate(
        title: String,
        path: String,
        durationMs: Long,
        position: Int
    ): PlaylistTransferFormat.ImportCandidate =
        PlaylistTransferFormat.ImportCandidate(
            title = title,
            path = path,
            durationMs = durationMs,
            position = position
        )

    private fun video(videoId: Long, path: String, position: Int): PlaylistVideoEntity =
        PlaylistVideoEntity(
            playlistId = 9,
            videoId = videoId,
            videoTitle = "Existing",
            videoPath = path,
            videoDuration = 1000,
            position = position
        )
}
