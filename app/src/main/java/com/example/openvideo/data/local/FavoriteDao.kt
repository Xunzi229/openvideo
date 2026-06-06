package com.example.openvideo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Query(
        """
        SELECT
            COALESCE(media_identity.currentVideoId, favorites.videoId) AS videoId,
            favorites.mediaIdentityId AS mediaIdentityId,
            COALESCE(media_identity.title, favorites.title) AS title,
            COALESCE(media_identity.currentPath, favorites.path) AS path,
            COALESCE(media_identity.durationMs, favorites.duration) AS duration,
            favorites.timestamp AS timestamp
        FROM favorites
        LEFT JOIN media_identity ON favorites.mediaIdentityId = media_identity.identityId
        ORDER BY favorites.timestamp DESC
        """
    )
    fun getAllWithIdentityFallback(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE videoId = :videoId")
    suspend fun delete(videoId: Long)

    @Query("DELETE FROM favorites WHERE mediaIdentityId = :mediaIdentityId")
    suspend fun deleteByMediaIdentityId(mediaIdentityId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoId = :videoId)")
    suspend fun isFavorite(videoId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaIdentityId = :mediaIdentityId)")
    suspend fun isFavoriteByMediaIdentityId(mediaIdentityId: Long): Boolean
}
