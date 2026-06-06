package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["mediaIdentityId"])]
)
data class FavoriteEntity(
    @PrimaryKey val videoId: Long,
    val mediaIdentityId: Long? = null,
    val title: String,
    val path: String,
    val duration: Long,
    val timestamp: Long   // epoch ms when favorited
)
