package com.example.openvideo.data.local

data class SeriesEpisodePlaybackEntity(
    val episodeId: Long,
    val seriesId: Long,
    val identityId: Long,
    val season: Int?,
    val episodeStart: Int,
    val episodeEnd: Int?,
    val episodeTitle: String,
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
    val historyLastPositionMs: Long?
)
