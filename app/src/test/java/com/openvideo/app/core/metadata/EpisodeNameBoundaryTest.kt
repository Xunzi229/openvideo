package com.openvideo.app.core.metadata

import org.junit.Assert.*
import org.junit.Test

class EpisodeNameBoundaryTest {
    @Test fun missingTitlesAndNoiseOnlyNamesAreNotRecognized() {
        for (name in listOf("", " ", "S01E02", "1x02", "\u7b2c2\u96c6", "EP02", "[02]", "[x][02]", "[1080p][02]")) {
            assertNull(name, EpisodeNameParser.parse(name))
        }
        assertNull(EpisodeNameParser.parse("S01E02", "x"))
    }

    @Test fun parentFallbackAndBracketNoiseRetainMeaningfulTitles() {
        assertEquals("My Show", EpisodeNameParser.parse("S01E02", "My.Show.1080p")!!.title)
        val bracket = EpisodeNameParser.parse("[My Show][1080p][x][02]")!!
        assertEquals("My Show", bracket.title)
        assertEquals(2, bracket.episodeStart)
        assertEquals("bracket_episode", bracket.rule)
        val chinese = EpisodeNameParser.parse("\u7b2c2\u96c6", "My Show")!!
        assertEquals(EpisodeMatchConfidence.LOW, chinese.confidence)
        val episode = EpisodeNameParser.parse("EP02", "My Show")!!
        assertEquals(EpisodeMatchConfidence.LOW, episode.confidence)
        assertEquals("My Show", EpisodeNameParser.parse("My.Show.1080p.S01E02")!!.title)
    }
}
