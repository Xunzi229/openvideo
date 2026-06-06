package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "network_recent_items",
    indices = [
        Index(value = ["normalizedUrl"], unique = true),
        Index(value = ["lastPlayedAt"])
    ]
)
data class NetworkRecentItemEntity(
    @PrimaryKey(autoGenerate = true) val recentId: Long = 0,
    val sourceId: Long? = null,
    val uri: String,
    val normalizedUrl: String,
    val displayUrl: String,
    val title: String,
    val durationMs: Long = 0L,
    val lastPlayedAt: Long,
    val createdAt: Long = lastPlayedAt,
    val updatedAt: Long = lastPlayedAt
)
