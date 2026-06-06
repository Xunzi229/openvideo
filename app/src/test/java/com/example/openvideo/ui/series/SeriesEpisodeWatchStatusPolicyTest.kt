package com.example.openvideo.ui.series

import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesEpisodeWatchStatusPolicyTest {

    private val labels = SeriesEpisodeWatchStatusLabels(
        unwatched = "Unwatched",
        completed = "Completed",
        progressPercent = { percent -> "$percent%" }
    )

    @Test
    fun noHistoryMapsToUnwatched() {
        val status = SeriesEpisodeWatchStatusPolicy.status(
            historyLastPositionMs = null,
            durationMs = 120_000L
        )

        assertEquals(SeriesEpisodeWatchState.UNWATCHED, status.state)
        assertEquals(null, status.progressPercent)
        assertEquals("Unwatched", SeriesEpisodeWatchStatusPolicy.label(status, labels))
    }

    @Test
    fun partialHistoryMapsToRoundedProgressPercent() {
        val status = SeriesEpisodeWatchStatusPolicy.status(
            historyLastPositionMs = 61_000L,
            durationMs = 120_000L
        )

        assertEquals(SeriesEpisodeWatchState.IN_PROGRESS, status.state)
        assertEquals(51, status.progressPercent)
        assertEquals("51%", SeriesEpisodeWatchStatusPolicy.label(status, labels))
    }

    @Test
    fun positionNearEndMapsToCompleted() {
        val status = SeriesEpisodeWatchStatusPolicy.status(
            historyLastPositionMs = 115_000L,
            durationMs = 120_000L
        )

        assertEquals(SeriesEpisodeWatchState.COMPLETED, status.state)
        assertEquals(null, status.progressPercent)
        assertEquals("Completed", SeriesEpisodeWatchStatusPolicy.label(status, labels))
    }

    @Test
    fun positiveHistoryWithUnknownDurationMapsToCompleted() {
        val status = SeriesEpisodeWatchStatusPolicy.status(
            historyLastPositionMs = 1_000L,
            durationMs = 0L
        )

        assertEquals(SeriesEpisodeWatchState.COMPLETED, status.state)
    }
}
