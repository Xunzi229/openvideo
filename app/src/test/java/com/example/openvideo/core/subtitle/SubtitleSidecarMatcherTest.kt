package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleSidecarMatcherTest {

    @Test
    fun sameDirectoryExactMatchesRankAheadOfLanguageSuffixMatches() {
        val candidates = SubtitleSidecarMatcher.matchSameDirectory(
            videoBaseName = "Demo",
            candidatePaths = listOf(
                "/Movies/Demo.zh-CN.ass",
                "/Movies/demo.SRT",
                "/Movies/Demo.en.vtt",
                "/Movies/Other.srt"
            )
        )

        assertEquals(
            listOf(
                SubtitleCandidate.exactBaseName("/Movies/demo.SRT"),
                SubtitleCandidate.languageSuffix("/Movies/Demo.en.vtt"),
                SubtitleCandidate.languageSuffix("/Movies/Demo.zh-CN.ass")
            ),
            candidates
        )
    }

    @Test
    fun unsupportedExtraSuffixAndDirectoryEntriesAreRejected() {
        val candidates = SubtitleSidecarMatcher.matchSameDirectory(
            videoBaseName = "Demo",
            candidatePaths = listOf(
                "/Movies/Demo.txt",
                "/Movies/Demo.extra.notes.srt",
                "/Movies/Demo.zh.srt/",
                "/Movies/Demo.chs.ssa"
            )
        )

        assertEquals(
            listOf(SubtitleCandidate.languageSuffix("/Movies/Demo.chs.ssa")),
            candidates
        )
    }

    @Test
    fun episodeMatchUsesEpisodeNameParserAfterExactAndLanguageSuffixMatches() {
        val candidates = SubtitleSidecarMatcher.matchSameDirectory(
            videoBaseName = "Show.Name.S01E02.1080p.WEB-DL.x264",
            candidatePaths = listOf(
                "/Movies/Show.Name.S01E02.zh.srt",
                "/Movies/Show.Name.S01E03.srt",
                "/Movies/Show.Name.S01E02.1080p.WEB-DL.x264.en.srt"
            )
        )

        assertEquals(
            listOf(
                SubtitleCandidate.languageSuffix("/Movies/Show.Name.S01E02.1080p.WEB-DL.x264.en.srt"),
                SubtitleCandidate.episodeMatch("/Movies/Show.Name.S01E02.zh.srt")
            ),
            candidates
        )
    }

    @Test
    fun episodeMatchSupportsChineseEpisodeMarker() {
        val candidates = SubtitleSidecarMatcher.matchSameDirectory(
            videoBaseName = "\u67d0\u5267 \u7b2c02\u96c6 1080P",
            candidatePaths = listOf(
                "/Movies/\u67d0\u5267 \u7b2c02\u96c6.srt",
                "/Movies/\u67d0\u5267 \u7b2c03\u96c6.srt"
            )
        )

        assertEquals(
            listOf(SubtitleCandidate.episodeMatch("/Movies/\u67d0\u5267 \u7b2c02\u96c6.srt")),
            candidates
        )
    }

    @Test
    fun subtitleDirectoryMatchesRankAfterSameDirectoryEpisodeMatches() {
        val candidates = SubtitleSidecarMatcher.matchCandidates(
            videoBaseName = "Show.Name.S01E02.1080p",
            candidates = listOf(
                SubtitleSidecarMatcher.CandidatePath("/Movies/Subtitles/Show.Name.S01E02.srt", inSubtitleDirectory = true),
                SubtitleSidecarMatcher.CandidatePath("/Movies/Show.Name.S01E02.zh.srt", inSubtitleDirectory = false)
            )
        )

        assertEquals(
            listOf(
                SubtitleCandidate.episodeMatch("/Movies/Show.Name.S01E02.zh.srt"),
                SubtitleCandidate.subtitleDirectory("/Movies/Subtitles/Show.Name.S01E02.srt")
            ),
            candidates
        )
    }

    @Test
    fun supportedSubtitlePathIgnoresQueryAndFragment() {
        assertEquals(true, SubtitleSidecarMatcher.isSupportedSubtitlePath("file:///Movies/Demo.Ass?token=1"))
        assertEquals(true, SubtitleSidecarMatcher.isSupportedSubtitlePath("file:///Movies/Demo.vtt#track"))
        assertEquals(false, SubtitleSidecarMatcher.isSupportedSubtitlePath("content://media/external/video/media/42"))
    }
}
