package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_sources",
    indices = [
        Index(value = ["type"]),
        Index(value = ["type", "normalizedUrl"], unique = true)
    ]
)
data class MediaSourceEntity(
    @PrimaryKey(autoGenerate = true) val sourceId: Long = 0,
    val type: String,
    val name: String,
    val url: String,
    val normalizedUrl: String,
    val displayUrl: String,
    val lastUsedAt: Long = 0L,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
