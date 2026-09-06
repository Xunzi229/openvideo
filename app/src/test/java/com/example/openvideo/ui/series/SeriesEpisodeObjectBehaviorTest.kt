package com.example.openvideo.ui.series

import android.app.Application
import com.example.openvideo.R
import com.example.openvideo.data.local.EpisodeEntity
import com.example.openvideo.data.local.SeriesEpisodePlaybackEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SeriesEpisodeObjectBehaviorTest {
    @Test fun episodeNumberAndTitleFallbacksHandleSeasonAndRangeIndependently() {
        for ((season, end, number) in listOf(Triple(null, null, "EP03"), Triple(null, 4, "EP03-04"),
            Triple(2, null, "S02E03"), Triple(2, 4, "S02E03-E04"))) {
            val entity = EpisodeEntity(episodeId = 1, seriesId = 2, identityId = 3, season = season,
                episodeStart = 3, episodeEnd = end, confidence = "HIGH", rule = "season", createdAt = 1, updatedAt = 2)
            val state = SeriesEpisodeUiState.from(entity)
            assertEquals(number, state.numberLabel)
            assertEquals(number, state.displayTitle)
            assertEquals(1L, state.episodeId)
            assertEquals(2L, state.seriesId)
            assertEquals(3L, state.identityId)
            assertEquals("HIGH", state.confidence)
            assertEquals("season", state.rule)
            assertFalse(state.isAvailable)
            assertEquals("Named", SeriesEpisodeUiState.from(entity.copy(episodeTitle = "Named")).displayTitle)
        }
    }

    @Test fun playableEpisodeConversionPreservesMetadataAndUsesFallbackTitle() {
        val entity = SeriesEpisodePlaybackEntity(1, 2, 3, null, 1, null, "Episode", "HIGH", "episode",
            42, "Video", "content://media/42", 100_000, 2000, 1920, 1080, 300, 50_000)
        for (title in listOf("Video", "")) {
            val state = SeriesEpisodeUiState.from(entity.copy(videoTitle = title))
            val video = state.toVideoItem()
            assertTrue(state.isAvailable)
            assertEquals(SeriesEpisodeWatchState.IN_PROGRESS, state.watchStatus.state)
            assertEquals(50, state.watchStatus.progressPercent)
            assertEquals(42L, video.id)
            assertEquals(if (title.isEmpty()) "Episode" else title, video.title)
            assertEquals(entity.videoPath, video.path)
            assertEquals(entity.videoPath, video.uri.toString())
            assertEquals(entity.videoDuration, video.duration)
            assertEquals(entity.videoSize, video.size)
            assertEquals(entity.videoWidth, video.width)
            assertEquals(entity.videoHeight, video.height)
            assertEquals(entity.videoDateAdded, video.dateAdded)
            assertNull(video.thumbnailUri)
        }
        assertEquals("EP01", SeriesEpisodeUiState.from(entity.copy(episodeTitle = "")).displayTitle)
    }

    @Test fun watchLabelsUseResourcesAndDefaultMissingProgressToZero() {
        val context = RuntimeEnvironment.getApplication()
        val labels = SeriesEpisodeWatchStatusLabels.from(context)
        assertEquals(context.getString(R.string.series_episode_unwatched), labels.unwatched)
        assertEquals(context.getString(R.string.history_continue_completed), labels.completed)
        assertEquals(context.getString(R.string.history_continue_missing_file), labels.missingFile)
        assertEquals(context.getString(R.string.history_continue_progress_percent, 0),
            SeriesEpisodeWatchStatusPolicy.label(SeriesEpisodeWatchStatus(SeriesEpisodeWatchState.IN_PROGRESS), labels))
        assertEquals(labels.missingFile, SeriesEpisodeWatchStatusPolicy.label(SeriesEpisodeWatchStatus(SeriesEpisodeWatchState.COMPLETED), labels, false))
    }
}
