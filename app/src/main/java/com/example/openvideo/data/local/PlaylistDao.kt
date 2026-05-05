package com.example.openvideo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId ORDER BY position")
    fun getVideos(playlistId: Long): Flow<List<PlaylistVideoEntity>>

    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId ORDER BY position")
    suspend fun getVideosOnce(playlistId: Long): List<PlaylistVideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: PlaylistVideoEntity)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideo(playlistId: Long, videoId: Long)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId")
    suspend fun clearVideos(playlistId: Long)

    @Query("UPDATE playlist_videos SET position = :position WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun updatePosition(playlistId: Long, videoId: Long, position: Int)

    @Query("SELECT COUNT(*) FROM playlist_videos WHERE playlistId = :playlistId")
    suspend fun getVideoCount(playlistId: Long): Int
}
