package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "playlist_videos",
    primaryKeys = ["playlistId", "videoId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistVideoEntity(
    val playlistId: Long,
    val videoId: Long,
    val videoTitle: String,
    val videoPath: String,
    val videoDuration: Long,
    val position: Int = 0
)
