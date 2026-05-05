package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoId: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val timestamp: Long   // epoch ms when favorited
)
