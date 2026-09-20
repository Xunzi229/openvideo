package com.openvideo.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSubtitleLoadPolicyTest {
    @Test fun explicitDocumentsAndKnownSubtitleFilesWinOverLocalVideoSidecars() {
        for (uri in listOf("content://documents/123", "file:///private/copy.subtitle")) {
            assertEquals(PlayerSubtitleLoadRequest.SubtitleUri(uri),
                PlayerSubtitleLoadPolicy.resolve(uri, "/local/movie.mp4", explicitSubtitle = true))
        }
        assertEquals(PlayerSubtitleLoadRequest.SubtitleUri("/local/chosen.ass"),
            PlayerSubtitleLoadPolicy.resolve("/local/chosen.ass", "/local/movie.mp4"))
    }

    @Test
    fun fileUriUsesItsOwnVideoPathForSidecarLookup() {
        assertEquals(
            PlayerSubtitleLoadRequest.SidecarFile("/storage/emulated/0/Movies/demo.mkv"),
            PlayerSubtitleLoadPolicy.resolve(
                uriString = "file:///storage/emulated/0/Movies/demo.mkv",
                videoPath = ""
            )
        )
    }

    @Test
    fun localVideoPathFallsBackToSidecarLookupWhenUriIsNotFile() {
        assertEquals(
            PlayerSubtitleLoadRequest.SidecarFile("/storage/emulated/0/Movies/demo.mp4"),
            PlayerSubtitleLoadPolicy.resolve(
                uriString = "content://media/external/video/media/42",
                videoPath = "/storage/emulated/0/Movies/demo.mp4"
            )
        )
    }

    @Test
    fun explicitSubtitleUriLoadsDirectly() {
        assertEquals(
            PlayerSubtitleLoadRequest.SubtitleUri("content://docs/subtitles/demo.srt"),
            PlayerSubtitleLoadPolicy.resolve(
                uriString = "content://docs/subtitles/demo.srt",
                videoPath = ""
            )
        )
    }

    @Test
    fun unsupportedSourceProducesNoSubtitleLoadRequest() {
        assertEquals(
            PlayerSubtitleLoadRequest.None,
            PlayerSubtitleLoadPolicy.resolve(
                uriString = "content://media/external/video/media/42",
                videoPath = ""
            )
        )
    }
}
