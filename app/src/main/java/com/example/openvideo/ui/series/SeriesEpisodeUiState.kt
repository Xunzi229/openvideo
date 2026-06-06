package com.example.openvideo.ui.series

import com.example.openvideo.core.media.LocalMediaUriPolicy
import com.example.openvideo.data.local.EpisodeEntity
import com.example.openvideo.data.local.SeriesEpisodePlaybackEntity
import com.example.openvideo.data.model.VideoItem

data class SeriesEpisodeUiState(
    val episodeId: Long,
    val seriesId: Long,
    val identityId: Long,
    val numberLabel: String,
    val displayTitle: String,
    val confidence: String,
    val rule: String,
    val videoId: Long,
    val videoTitle: String,
    val videoPath: String,
    val videoDuration: Long,
    val videoSize: Long,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoDateAdded: Long,
    val isAvailable: Boolean,
    val watchStatus: SeriesEpisodeWatchStatus
) {
    fun toVideoItem(): VideoItem =
        VideoItem(
            id = videoId,
            title = videoTitle.takeIf { it.isNotBlank() } ?: displayTitle,
            path = videoPath,
            uri = LocalMediaUriPolicy.playbackUri(videoPath),
            duration = videoDuration,
            size = videoSize,
            width = videoWidth,
            height = videoHeight,
            dateAdded = videoDateAdded,
            thumbnailUri = null
        )

    companion object {
        fun from(entity: EpisodeEntity): SeriesEpisodeUiState {
            val numberLabel = numberLabel(
                season = entity.season,
                episodeStart = entity.episodeStart,
                episodeEnd = entity.episodeEnd
            )
            return SeriesEpisodeUiState(
                episodeId = entity.episodeId,
                seriesId = entity.seriesId,
                identityId = entity.identityId,
                numberLabel = numberLabel,
                displayTitle = entity.episodeTitle.takeIf { it.isNotBlank() } ?: numberLabel,
                confidence = entity.confidence,
                rule = entity.rule,
                videoId = 0L,
                videoTitle = "",
                videoPath = "",
                videoDuration = 0L,
                videoSize = 0L,
                videoWidth = 0,
                videoHeight = 0,
                videoDateAdded = 0L,
                isAvailable = false,
                watchStatus = SeriesEpisodeWatchStatusPolicy.status(
                    historyLastPositionMs = null,
                    durationMs = 0L
                )
            )
        }

        fun from(entity: SeriesEpisodePlaybackEntity): SeriesEpisodeUiState {
            val numberLabel = numberLabel(
                season = entity.season,
                episodeStart = entity.episodeStart,
                episodeEnd = entity.episodeEnd
            )
            return SeriesEpisodeUiState(
                episodeId = entity.episodeId,
                seriesId = entity.seriesId,
                identityId = entity.identityId,
                numberLabel = numberLabel,
                displayTitle = entity.episodeTitle.takeIf { it.isNotBlank() } ?: numberLabel,
                confidence = entity.confidence,
                rule = entity.rule,
                videoId = entity.videoId,
                videoTitle = entity.videoTitle,
                videoPath = entity.videoPath,
                videoDuration = entity.videoDuration,
                videoSize = entity.videoSize,
                videoWidth = entity.videoWidth,
                videoHeight = entity.videoHeight,
                videoDateAdded = entity.videoDateAdded,
                isAvailable = SeriesEpisodeAvailabilityPolicy.isAvailable(entity.videoPath),
                watchStatus = SeriesEpisodeWatchStatusPolicy.status(
                    historyLastPositionMs = entity.historyLastPositionMs,
                    durationMs = entity.videoDuration
                )
            )
        }

        private fun numberLabel(season: Int?, episodeStart: Int, episodeEnd: Int?): String {
            val start = episodeStart.twoDigits()
            return if (season != null) {
                val prefix = "S${season.twoDigits()}E$start"
                episodeEnd?.let { "$prefix-E${it.twoDigits()}" } ?: prefix
            } else {
                val prefix = "EP$start"
                episodeEnd?.let { "$prefix-${it.twoDigits()}" } ?: prefix
            }
        }

        private fun Int.twoDigits(): String = toString().padStart(2, '0')
    }
}
