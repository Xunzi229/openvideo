package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSubtitleLoadPolicyTest {

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
