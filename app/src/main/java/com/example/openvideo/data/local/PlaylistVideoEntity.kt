package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
    ],
    indices = [Index(value = ["mediaIdentityId"])]
)
data class PlaylistVideoEntity(
    val playlistId: Long,
    val videoId: Long,
    val mediaIdentityId: Long? = null,
    val videoTitle: String,
    val videoPath: String,
    val videoDuration: Long,
    val position: Int = 0
)
