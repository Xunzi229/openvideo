package com.example.openvideo.data.repository

import android.app.PendingIntent
import com.example.openvideo.data.local.FavoriteDao
import com.example.openvideo.data.local.FavoriteEntity
import com.example.openvideo.data.local.HistoryDao
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.local.PlaylistDao
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.scanner.VideoDeleteResult
import com.example.openvideo.data.scanner.VideoScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoScanner: VideoScanner,
    private val historyDao: HistoryDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao
) {

    fun scanLocalVideos(): Flow<List<VideoItem>> = videoScanner.scanVideos()

    // History
    fun getHistory(): Flow<List<HistoryEntity>> = historyDao.getAll()

    suspend fun saveHistory(video: VideoItem, position: Long) {
        historyDao.upsert(
            HistoryEntity(
                videoId = video.id,
                title = video.title,
                path = video.path,
                duration = video.duration,
                lastPosition = position,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun getHistory(videoId: Long): HistoryEntity? = historyDao.getByVideoId(videoId)

    suspend fun clearHistory() = historyDao.deleteAll()

    // Favorites
    fun getFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAll()

    suspend fun toggleFavorite(video: VideoItem) {
        if (favoriteDao.isFavorite(video.id)) {
            favoriteDao.delete(video.id)
        } else {
            favoriteDao.insert(
                FavoriteEntity(
                    videoId = video.id,
                    title = video.title,
                    path = video.path,
                    duration = video.duration,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun isFavorite(videoId: Long): Boolean = favoriteDao.isFavorite(videoId)

    suspend fun addToQuickPlaylist(video: VideoItem): Long {
        val playlist = playlistDao.getAll().first().firstOrNull { it.name == QUICK_PLAYLIST_NAME }
        val playlistId = playlist?.id ?: playlistDao.insert(PlaylistEntity(name = QUICK_PLAYLIST_NAME))
        val position = playlistDao.getVideoCount(playlistId)
        playlistDao.insertVideo(
            PlaylistVideoEntity(
                playlistId = playlistId,
                videoId = video.id,
                videoTitle = video.title,
                videoPath = video.path,
                videoDuration = video.duration,
                position = position
            )
        )
        return playlistId
    }

    fun deleteVideo(video: VideoItem): Boolean {
        return videoScanner.deleteVideo(video.uri)
    }

    fun deleteVideos(videos: List<VideoItem>): VideoDeleteResult {
        return videoScanner.deleteVideos(videos.map { it.uri })
    }

    fun createDeleteRequest(videos: List<VideoItem>): PendingIntent? {
        return videoScanner.createDeleteRequest(videos.map { it.uri })
    }

    private companion object {
        const val QUICK_PLAYLIST_NAME = "Quick Playlist"
    }
}
