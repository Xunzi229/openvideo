package com.example.openvideo.core.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeNameParserTest {

    @Test
    fun parsesSeasonEpisodeNameWithCleanTitle() {
        val match = EpisodeNameParser.parse("Show.Name.S01E02.1080p.WEB-DL.x264.mkv")

        assertNotNull(match)
        checkNotNull(match)
        assertEquals("Show Name", match.title)
        assertEquals(1, match.season)
        assertEquals(2, match.episodeStart)
        assertEquals(null, match.episodeEnd)
        assertEquals(EpisodeMatchConfidence.HIGH, match.confidence)
        assertEquals("season_episode", match.rule)
    }

    @Test
    fun parsesOneByEpisodeName() {
        val match = EpisodeNameParser.parse("Show.Name.1x02.HDTV.mkv")

        assertNotNull(match)
        checkNotNull(match)
        assertEquals("Show Name", match.title)
        assertEquals(1, match.season)
        assertEquals(2, match.episodeStart)
        assertEquals(EpisodeMatchConfidence.HIGH, match.confidence)
        assertEquals("one_by_episode", match.rule)
    }

    @Test
    fun parsesSeasonEpisodeRange() {
        val match = EpisodeNameParser.parse("Show.Name.S01E01-E02.mkv")

        assertNotNull(match)
        checkNotNull(match)
        assertEquals("Show Name", match.title)
        assertEquals(1, match.season)
        assertEquals(1, match.episodeStart)
        assertEquals(2, match.episodeEnd)
        assertEquals(EpisodeMatchConfidence.HIGH, match.confidence)
        assertEquals("season_episode", match.rule)
    }

    @Test
    fun parsesChineseEpisodeMarker() {
        val match = EpisodeNameParser.parse("\u67d0\u5267 \u7b2c02\u96c6 1080P.mp4")

        assertNotNull(match)
        checkNotNull(match)
        assertEquals("\u67d0\u5267", match.title)
        assertEquals(null, match.season)
        assertEquals(2, match.episodeStart)
        assertEquals(EpisodeMatchConfidence.MEDIUM, match.confidence)
        assertEquals("chinese_episode", match.rule)
    }

    @Test
    fun parsesBracketedAnimeEpisodeName() {
        val match = EpisodeNameParser.parse("[Group][Anime.Name][03][1080P][JPSC].mkv")

        assertNotNull(match)
        checkNotNull(match)
        assertEquals("Anime Name", match.title)
        assertEquals(null, match.season)
        assertEquals(3, match.episodeStart)
        assertEquals(EpisodeMatchConfidence.MEDIUM, match.confidence)
        assertEquals("bracket_episode", match.rule)
    }

    @Test
    fun lowersConfidenceWhenUsingParentFolderFallback() {
        val match = EpisodeNameParser.parse("EP12.mkv", parentFolderName = "Show.Name")

        assertNotNull(match)
        checkNotNull(match)
        assertEquals("Show Name", match.title)
        assertEquals(null, match.season)
        assertEquals(12, match.episodeStart)
        assertEquals(EpisodeMatchConfidence.LOW, match.confidence)
        assertEquals("episode_only", match.rule)
    }

    @Test
    fun ignoresMovieYearWithoutEpisodeToken() {
        assertNull(EpisodeNameParser.parse("Movie.Name.2024.1080p.BluRay.x264.mkv"))
    }

    @Test
    fun ignoresPureNumericFileName() {
        assertNull(EpisodeNameParser.parse("03.mkv"))
    }

    @Test
    fun ignoresReversedEpisodeRange() {
        assertNull(EpisodeNameParser.parse("Show.Name.S01E03-E02.mkv"))
    }
}
