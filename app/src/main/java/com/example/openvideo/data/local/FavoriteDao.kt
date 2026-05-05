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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE videoId = :videoId")
    suspend fun delete(videoId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoId = :videoId)")
    suspend fun isFavorite(videoId: Long): Boolean
}
