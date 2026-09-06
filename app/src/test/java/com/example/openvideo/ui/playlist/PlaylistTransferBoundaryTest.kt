package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity
import org.junit.Assert.*
import org.junit.Test

class PlaylistTransferBoundaryTest {
    @Test fun jsonRoundTripPreservesEscapesAndBracketsInsideStrings() {
        val title = "brackets []{} quote \" tab\t line\n return\r slash\\"
        val row = PlaylistVideoEntity(playlistId = 1, videoId = 2, videoTitle = title, videoPath = "C:\\video\\clip.mp4", videoDuration = 1000, position = 0)
        val result = PlaylistTransferFormat.parseJson(PlaylistTransferFormat.exportJson(title, listOf(row))) as PlaylistTransferFormat.ParseResult.Success
        assertEquals(title, result.playlistName)
        assertEquals(title, result.items.single().title)
        assertEquals(row.videoPath, result.items.single().path)
        assertEquals("playlist.openvideo-playlist.json", PlaylistTransferFormat.suggestedJsonFileName(" "))
        assertEquals("a_b.openvideo-playlist.json", PlaylistTransferFormat.suggestedJsonFileName("a/b"))
    }

    @Test fun missingFieldsUnsupportedVersionsAndUnterminatedArraysFail() {
        val cases = listOf("[]" to PlaylistTransferFormat.FailureReason.INVALID_JSON,
            "{}" to PlaylistTransferFormat.FailureReason.INVALID_JSON,
            "{\"schemaVersion\":2}" to PlaylistTransferFormat.FailureReason.UNSUPPORTED_VERSION,
            "{\"schemaVersion\":1}" to PlaylistTransferFormat.FailureReason.INVALID_JSON,
            "{\"schemaVersion\":1,\"videos\":[}" to PlaylistTransferFormat.FailureReason.INVALID_JSON,
            "{\"schemaVersion\":1,\"videos\":[]}" to PlaylistTransferFormat.FailureReason.EMPTY_PLAYLIST,
            "{\"schemaVersion\":1,\"videos\":[{}, {\"title\":\"x\"}]}" to PlaylistTransferFormat.FailureReason.EMPTY_PLAYLIST)
        cases.forEach { (text, reason) -> assertEquals(text, PlaylistTransferFormat.ParseResult.Failure(reason), PlaylistTransferFormat.parseJson(text)) }
        val result = PlaylistTransferFormat.parseJson("""{"schemaVersion":1,"videos":[{"title":"x","path":"/x"}]}""") as PlaylistTransferFormat.ParseResult.Success
        assertNull(result.playlistName)
        assertEquals(0L, result.items.single().durationMs)
        assertEquals(0, result.items.single().position)
    }

    @Test fun m3uMissingMetadataFallsBackAndDoesNotLeakIntoFollowingRows() {
        val result = PlaylistTransferFormat.parseM3u("\n#EXTM3U\n#extinf:invalid\n/path/a.mp4\n/\n#EXTINF:3,Named\n/b.mp4\n/c.mp4") as PlaylistTransferFormat.ParseResult.Success
        assertEquals(listOf("a.mp4", "/", "Named", "c.mp4"), result.items.map { it.title })
        assertEquals(listOf(0L, 0L, 3000L, 0L), result.items.map { it.durationMs })
    }

    @Test fun importDropsBlankAndDuplicatePathsAndStartsAfterExistingRows() {
        val existing = listOf(PlaylistVideoEntity(playlistId = 1, videoId = 10, videoTitle = "existing", videoPath = "/a", videoDuration = 1, position = 4))
        val candidates = listOf(" ", " /a ", " /b ", "/b", "/c").mapIndexed { index, path -> PlaylistTransferFormat.ImportCandidate("", path, -1, index) }
        val rows = PlaylistImportPolicy.createRows(1, existing, candidates)
        assertEquals(listOf("/b", "/c"), rows.map { it.videoPath })
        assertEquals(listOf("b", "c"), rows.map { it.videoTitle })
        assertEquals(listOf(5, 6), rows.map { it.position })
        assertEquals(listOf(-1L, -2L), rows.map { it.videoId })
        assertEquals(listOf(0L, 0L), rows.map { it.videoDuration })
        assertEquals(-4L, PlaylistImportPolicy.createRows(1, existing + existing[0].copy(videoId = -3, videoPath = "/d", position = 7), candidates).first().videoId)
    }
}
