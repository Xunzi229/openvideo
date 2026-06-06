package com.example.openvideo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkRecentItemDao {

    @Query("SELECT * FROM network_recent_items ORDER BY lastPlayedAt DESC")
    fun getAll(): Flow<List<NetworkRecentItemEntity>>

    @Query("SELECT * FROM network_recent_items WHERE normalizedUrl = :normalizedUrl")
    suspend fun getByNormalizedUrl(normalizedUrl: String): NetworkRecentItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: NetworkRecentItemEntity): Long

    @Query("DELETE FROM network_recent_items WHERE recentId = :recentId")
    suspend fun delete(recentId: Long)

    @Query("DELETE FROM network_recent_items")
    suspend fun deleteAll()
}
