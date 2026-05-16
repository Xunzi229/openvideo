package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSubtitleAutoloadTest {

    @Test
    fun doesNotTreatContentVideoUriAsSubtitleFile() {
        assertFalse(PlayerSubtitleAutoload.canLoadAsSubtitleUri("content://media/external/video/media/42"))
    }

    @Test
    fun allowsExplicitSubtitleDocuments() {
        assertTrue(PlayerSubtitleAutoload.canLoadAsSubtitleUri("file:///storage/emulated/0/Movies/demo.srt"))
        assertTrue(PlayerSubtitleAutoload.canLoadAsSubtitleUri("file:///storage/emulated/0/Movies/demo.ass"))
        assertTrue(PlayerSubtitleAutoload.canLoadAsSubtitleUri("file:///storage/emulated/0/Movies/demo.vtt"))
    }

    @Test
    fun exactBaseNameMatchesRankAheadOfLanguageSuffixMatches() {
        assertEquals(
            listOf(
                "demo.ass",
                "demo.srt",
                "demo.en.srt",
                "demo.zh-CN.ass"
            ),
            PlayerSubtitleAutoload.rankSidecarCandidates(
                videoBaseName = "demo",
                candidateFileNames = listOf(
                    "demo.en.srt",
                    "demo.ass",
                    "demo.zh-CN.ass",
                    "demo.srt"
                )
            )
        )
    }

    @Test
    fun unsupportedOrUnrelatedFilesAreExcludedFromSidecarCandidates() {
        assertEquals(
            listOf("demo.en.srt"),
            PlayerSubtitleAutoload.rankSidecarCandidates(
                videoBaseName = "demo",
                candidateFileNames = listOf(
                    "demo.txt",
                    "demo.en.srt",
                    "other.srt",
                    "demo.extra.notes.srt"
                )
            )
        )
    }
}
