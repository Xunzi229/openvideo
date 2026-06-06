package com.example.openvideo.ui.series

import android.content.Context
import com.example.openvideo.R
import kotlin.math.roundToInt

enum class SeriesEpisodeWatchState {
    UNWATCHED,
    IN_PROGRESS,
    COMPLETED
}

data class SeriesEpisodeWatchStatus(
    val state: SeriesEpisodeWatchState,
    val progressPercent: Int? = null
)

data class SeriesEpisodeWatchStatusLabels(
    val unwatched: String,
    val completed: String,
    val progressPercent: (Int) -> String,
    val missingFile: String = "Missing file"
) {
    companion object {
        fun from(context: Context): SeriesEpisodeWatchStatusLabels =
            SeriesEpisodeWatchStatusLabels(
                unwatched = context.getString(R.string.series_episode_unwatched),
                completed = context.getString(R.string.history_continue_completed),
                progressPercent = { percent ->
                    context.getString(R.string.history_continue_progress_percent, percent)
                },
                missingFile = context.getString(R.string.history_continue_missing_file)
            )
    }
}

object SeriesEpisodeWatchStatusPolicy {
    private const val COMPLETED_WINDOW_MS = 10_000L

    fun status(historyLastPositionMs: Long?, durationMs: Long): SeriesEpisodeWatchStatus {
        val position = historyLastPositionMs?.takeIf { it > 0L }
            ?: return SeriesEpisodeWatchStatus(SeriesEpisodeWatchState.UNWATCHED)
        if (durationMs <= 0L) {
            return SeriesEpisodeWatchStatus(SeriesEpisodeWatchState.COMPLETED)
        }
        if (position >= (durationMs - COMPLETED_WINDOW_MS).coerceAtLeast(0L)) {
            return SeriesEpisodeWatchStatus(SeriesEpisodeWatchState.COMPLETED)
        }
        val percent = ((position.toDouble() / durationMs.toDouble()).coerceIn(0.0, 1.0) * 100)
            .roundToInt()
        return SeriesEpisodeWatchStatus(
            state = SeriesEpisodeWatchState.IN_PROGRESS,
            progressPercent = percent
        )
    }

    fun label(
        status: SeriesEpisodeWatchStatus,
        labels: SeriesEpisodeWatchStatusLabels,
        isAvailable: Boolean = true
    ): String {
        if (!isAvailable) return labels.missingFile
        return when (status.state) {
        SeriesEpisodeWatchState.UNWATCHED -> labels.unwatched
        SeriesEpisodeWatchState.COMPLETED -> labels.completed
        SeriesEpisodeWatchState.IN_PROGRESS -> labels.progressPercent(status.progressPercent ?: 0)
        }
    }
}
