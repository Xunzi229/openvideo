package com.example.openvideo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM play_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    @Query("DELETE FROM play_history WHERE videoId = :videoId")
    suspend fun delete(videoId: Long)

    @Query("DELETE FROM play_history")
    suspend fun deleteAll()
}
