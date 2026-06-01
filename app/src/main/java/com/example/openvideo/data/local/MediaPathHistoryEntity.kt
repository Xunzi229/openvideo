package com.example.openvideo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "media_path_history",
    primaryKeys = ["identityId", "normalizedPathKey"],
    foreignKeys = [
        ForeignKey(
            entity = MediaIdentityEntity::class,
            parentColumns = ["identityId"],
            childColumns = ["identityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["identityId"]),
        Index(value = ["normalizedPathKey"])
    ]
)
data class MediaPathHistoryEntity(
    val identityId: Long,
    val path: String,
    val normalizedPathKey: String,
    val seenAt: Long,
    val exists: Boolean
)
