package com.example.openvideo.ui.series

import com.example.openvideo.data.local.EpisodeEntity
import com.example.openvideo.data.local.SeriesEpisodePlaybackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class SeriesEpisodeUiStateTest {

    @Test
    fun mapsSeasonEpisodeToStableNumberLabelAndDisplayTitle() {
        val state = uiStateFrom(
            episode(
                episodeId = 10L,
                seriesId = 20L,
                identityId = 30L,
                season = 1,
                episodeStart = 2,
                episodeTitle = "Pilot"
            )
        )

        assertEquals(10L, longProperty(state, "episodeId"))
        assertEquals(20L, longProperty(state, "seriesId"))
        assertEquals(30L, longProperty(state, "identityId"))
        assertEquals("S01E02", stringProperty(state, "numberLabel"))
        assertEquals("Pilot", stringProperty(state, "displayTitle"))
        assertEquals("HIGH", stringProperty(state, "confidence"))
        assertEquals("season_episode", stringProperty(state, "rule"))
    }

    @Test
    fun mapsEpisodeRangeWithoutSeasonToEpisodeRangeLabel() {
        val state = uiStateFrom(
            episode(
                season = null,
                episodeStart = 12,
                episodeEnd = 13,
                episodeTitle = ""
            )
        )

        assertEquals("EP12-13", stringProperty(state, "numberLabel"))
        assertEquals("EP12-13", stringProperty(state, "displayTitle"))
    }

    @Test
    fun mapsSeasonEpisodeRangeToCompactSeasonRangeLabel() {
        val state = uiStateFrom(
            episode(
                season = 2,
                episodeStart = 3,
                episodeEnd = 4,
                episodeTitle = "Finale"
            )
        )

        assertEquals("S02E03-E04", stringProperty(state, "numberLabel"))
        assertEquals("Finale", stringProperty(state, "displayTitle"))
    }

    @Test
    fun playableEntityMarksContentUrisAvailableAndMissingLocalFilesUnavailable() {
        val contentUriState = SeriesEpisodeUiState.from(
            playableEpisode(videoPath = "content://media/external/video/media/42")
        )
        val missingLocalState = SeriesEpisodeUiState.from(
            playableEpisode(videoPath = "/missing-series-${System.nanoTime()}.mp4")
        )

        assertEquals(true, booleanProperty(contentUriState, "isAvailable"))
        assertEquals(false, booleanProperty(missingLocalState, "isAvailable"))
    }

    private fun uiStateFrom(entity: EpisodeEntity): Any {
        val stateClass = try {
            Class.forName("com.example.openvideo.ui.series.SeriesEpisodeUiState")
        } catch (e: ClassNotFoundException) {
            fail("SeriesEpisodeUiState must exist before the detail screen can render episodes")
            throw e
        }
        val companion = stateClass.getDeclaredField("Companion").get(null)
        val method = companion.javaClass.getDeclaredMethod("from", EpisodeEntity::class.java)
        return method.invoke(companion, entity)
    }

    private fun stringProperty(instance: Any, property: String): String =
        instance.javaClass.getMethod(getter(property)).invoke(instance) as String

    private fun longProperty(instance: Any, property: String): Long =
        instance.javaClass.getMethod(getter(property)).invoke(instance) as Long

    private fun booleanProperty(instance: Any, property: String): Boolean =
        try {
            val methodName = if (property.startsWith("is")) property else getter(property)
            instance.javaClass.getMethod(methodName).invoke(instance) as Boolean
        } catch (e: NoSuchMethodException) {
            fail("SeriesEpisodeUiState must expose $property for missing-file degradation")
            throw e
        }

    private fun getter(property: String): String =
        "get" + property.substring(0, 1).uppercase() + property.substring(1)

    private fun episode(
        episodeId: Long = 1L,
        seriesId: Long = 2L,
        identityId: Long = 3L,
        season: Int? = null,
        episodeStart: Int = 1,
        episodeEnd: Int? = null,
        episodeTitle: String = "",
        confidence: String = "HIGH",
        rule: String = "season_episode"
    ): EpisodeEntity = EpisodeEntity(
        episodeId = episodeId,
        seriesId = seriesId,
        identityId = identityId,
        season = season,
        episodeStart = episodeStart,
        episodeEnd = episodeEnd,
        episodeTitle = episodeTitle,
        confidence = confidence,
        rule = rule,
        createdAt = 100L,
        updatedAt = 200L
    )

    private fun playableEpisode(videoPath: String): SeriesEpisodePlaybackEntity =
        SeriesEpisodePlaybackEntity(
            episodeId = 1L,
            seriesId = 2L,
            identityId = 3L,
            season = 1,
            episodeStart = 1,
            episodeEnd = null,
            episodeTitle = "",
            confidence = "HIGH",
            rule = "season_episode",
            videoId = 4L,
            videoTitle = "Episode",
            videoPath = videoPath,
            videoDuration = 120_000L,
            videoSize = 42L,
            videoWidth = 1920,
            videoHeight = 1080,
            videoDateAdded = 1_000L,
            historyLastPositionMs = null
        )
}
