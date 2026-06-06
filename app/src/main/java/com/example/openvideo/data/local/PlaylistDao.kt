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

    @Query(
        """
        SELECT
            playlist_videos.playlistId AS playlistId,
            COALESCE(media_identity.currentVideoId, playlist_videos.videoId) AS videoId,
            playlist_videos.mediaIdentityId AS mediaIdentityId,
            COALESCE(media_identity.title, playlist_videos.videoTitle) AS videoTitle,
            COALESCE(media_identity.currentPath, playlist_videos.videoPath) AS videoPath,
            COALESCE(media_identity.durationMs, playlist_videos.videoDuration) AS videoDuration,
            playlist_videos.position AS position
        FROM playlist_videos
        LEFT JOIN media_identity ON playlist_videos.mediaIdentityId = media_identity.identityId
        WHERE playlist_videos.playlistId = :playlistId
        ORDER BY playlist_videos.position
        """
    )
    fun getVideos(playlistId: Long): Flow<List<PlaylistVideoEntity>>

    @Query(
        """
        SELECT
            playlist_videos.playlistId AS playlistId,
            COALESCE(media_identity.currentVideoId, playlist_videos.videoId) AS videoId,
            playlist_videos.mediaIdentityId AS mediaIdentityId,
            COALESCE(media_identity.title, playlist_videos.videoTitle) AS videoTitle,
            COALESCE(media_identity.currentPath, playlist_videos.videoPath) AS videoPath,
            COALESCE(media_identity.durationMs, playlist_videos.videoDuration) AS videoDuration,
            playlist_videos.position AS position
        FROM playlist_videos
        LEFT JOIN media_identity ON playlist_videos.mediaIdentityId = media_identity.identityId
        WHERE playlist_videos.playlistId = :playlistId
        ORDER BY playlist_videos.position
        """
    )
    suspend fun getVideosOnce(playlistId: Long): List<PlaylistVideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: PlaylistVideoEntity)

    @Update
    suspend fun updatePositions(videos: List<PlaylistVideoEntity>)

    @Query(
        """
        DELETE FROM playlist_videos
        WHERE playlistId = :playlistId
            AND (
                videoId = :videoId
                OR mediaIdentityId IN (
                    SELECT identityId FROM media_identity WHERE currentVideoId = :videoId
                )
            )
        """
    )
    suspend fun removeVideo(playlistId: Long, videoId: Long)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId")
    suspend fun clearVideos(playlistId: Long)

    @Query("UPDATE playlist_videos SET position = :position WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun updatePosition(playlistId: Long, videoId: Long, position: Int)

    @Query("SELECT COUNT(*) FROM playlist_videos WHERE playlistId = :playlistId")
    suspend fun getVideoCount(playlistId: Long): Int
}
