package com.example.openvideo.data.repository

import com.example.openvideo.data.local.FavoriteDao
import com.example.openvideo.data.local.FavoriteEntity
import com.example.openvideo.data.local.HistoryDao
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.scanner.VideoScanner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoScanner: VideoScanner,
    private val historyDao: HistoryDao,
    private val favoriteDao: FavoriteDao
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
}
