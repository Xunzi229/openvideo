package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaIdentityEntity::class,
            parentColumns = ["identityId"],
            childColumns = ["identityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["seriesId"]),
        Index(value = ["identityId"], unique = true),
        Index(value = ["seriesId", "season", "episodeStart"], name = "index_episodes_series_order")
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val episodeId: Long = 0,
    val seriesId: Long,
    val identityId: Long,
    val season: Int? = null,
    val episodeStart: Int,
    val episodeEnd: Int? = null,
    val episodeTitle: String = "",
    val confidence: String,
    val rule: String,
    val createdAt: Long,
    val updatedAt: Long
)
