package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_identity",
    indices = [
        Index(value = ["currentVideoId"], unique = true),
        Index(value = ["normalizedPathKey"], unique = true),
        Index(
            value = ["sizeBytes", "durationMs", "width", "height"],
            name = "index_media_identity_fingerprint"
        )
    ]
)
data class MediaIdentityEntity(
    @PrimaryKey(autoGenerate = true) val identityId: Long = 0,
    val currentVideoId: Long,
    val title: String,
    val currentPath: String,
    val normalizedPathKey: String,
    val normalizedTitleKey: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val modifiedTime: Long,
    val firstSeen: Long,
    val lastSeen: Long
)
