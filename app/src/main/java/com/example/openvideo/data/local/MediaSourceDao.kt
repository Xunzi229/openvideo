package com.example.openvideo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaSourceDao {

    @Query("SELECT * FROM media_sources ORDER BY lastUsedAt DESC, updatedAt DESC")
    fun getAll(): Flow<List<MediaSourceEntity>>

    @Query("SELECT * FROM media_sources WHERE sourceId = :sourceId")
    suspend fun getById(sourceId: Long): MediaSourceEntity?

    @Query("SELECT * FROM media_sources WHERE type = :type AND normalizedUrl = :normalizedUrl")
    suspend fun getByTypeAndUrl(type: String, normalizedUrl: String): MediaSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(source: MediaSourceEntity): Long

    @Update
    suspend fun update(source: MediaSourceEntity)

    @Query("DELETE FROM media_sources WHERE sourceId = :sourceId")
    suspend fun delete(sourceId: Long)

    @Query("UPDATE media_sources SET lastUsedAt = :lastUsedAt, updatedAt = :lastUsedAt WHERE sourceId = :sourceId")
    suspend fun markUsed(sourceId: Long, lastUsedAt: Long)
}
