package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "series",
    indices = [
        Index(value = ["normalizedTitleKey", "folderPath"], unique = true)
    ]
)
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val seriesId: Long = 0,
    val title: String,
    val normalizedTitleKey: String,
    val folderPath: String,
    val posterPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
