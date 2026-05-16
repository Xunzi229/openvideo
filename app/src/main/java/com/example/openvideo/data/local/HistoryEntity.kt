package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_history")
data class HistoryEntity(
    @PrimaryKey val videoId: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val lastPosition: Long,   // last playback position in ms
    val timestamp: Long,      // epoch ms when last played
    val speed: Float = 1.0f,
    val aspectRatioKey: String = "fit",
    val externalSubtitleUri: String = "",
    val subtitlesEnabled: Boolean = true,
    val audioTrackGroupIndex: Int = -1,
    val audioTrackIndex: Int = -1,
    val audioMuted: Boolean = false
)
