package com.example.openvideo.data.repository

import android.app.PendingIntent
import com.example.openvideo.core.mediaid.MediaFingerprint
import com.example.openvideo.core.mediaid.MediaFingerprintPolicy
import com.example.openvideo.core.mediaid.MediaIdentityCandidate
import com.example.openvideo.core.mediaid.MediaIdentityMatchDecision
import com.example.openvideo.core.mediaid.MediaIdentityMatcher
import com.example.openvideo.data.local.FavoriteDao
import com.example.openvideo.data.local.FavoriteEntity
import com.example.openvideo.data.local.HistoryDao
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.local.MediaIdentityDao
import com.example.openvideo.data.local.MediaIdentityEntity
import com.example.openvideo.data.local.MediaPathHistoryEntity
import com.example.openvideo.data.local.PlaylistDao
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.scanner.VideoDeleteResult
import com.example.openvideo.data.scanner.VideoScanOutcome
import com.example.openvideo.data.scanner.VideoScanner
import com.example.openvideo.ui.history.HistoryCleanupPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoScanner: VideoScanner,
    private val historyDao: HistoryDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val mediaIdentityDao: MediaIdentityDao
) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sharedScanResults = videoScanner.scanVideos()
        .onEach { outcome ->
            if (outcome is VideoScanOutcome.Success) {
                syncMediaIdentities(outcome.videos)
            }
        }
        .shareIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            replay = 1
        )

    fun scanLocalVideos(): Flow<VideoScanOutcome> = sharedScanResults

    private suspend fun syncMediaIdentities(videos: List<VideoItem>) {
        val now = System.currentTimeMillis()
        videos.forEach { video ->
            val fingerprint = MediaFingerprintPolicy.fromFields(
                title = video.title,
                pathOrUri = video.path,
                sizeBytes = video.size,
                durationMs = video.duration,
                width = video.width,
                height = video.height,
                timestamp = video.dateAdded * MILLIS_PER_SECOND
            ) ?: return@forEach

            val existing = resolveMediaIdentity(video, fingerprint)
            val entity = when (existing) {
                is MediaIdentityMatchDecision.Matched -> {
                    val previous = mediaIdentityDao.getByIdentityId(existing.identityId) ?: return@forEach
                    previous.copy(
                        currentVideoId = video.id,
                        title = video.title,
                        currentPath = video.path,
                        normalizedPathKey = fingerprint.normalizedPathKey,
                        normalizedTitleKey = fingerprint.normalizedTitleKey,
                        sizeBytes = fingerprint.sizeBytes,
                        durationMs = fingerprint.durationMs,
                        width = fingerprint.width,
                        height = fingerprint.height,
                        modifiedTime = fingerprint.timestamp,
                        lastSeen = now
                    )
                }
                is MediaIdentityMatchDecision.Conflict -> null
                MediaIdentityMatchDecision.NoMatch -> MediaIdentityEntity(
                    currentVideoId = video.id,
                    title = video.title,
                    currentPath = video.path,
                    normalizedPathKey = fingerprint.normalizedPathKey,
                    normalizedTitleKey = fingerprint.normalizedTitleKey,
                    sizeBytes = fingerprint.sizeBytes,
                    durationMs = fingerprint.durationMs,
                    width = fingerprint.width,
                    height = fingerprint.height,
                    modifiedTime = fingerprint.timestamp,
                    firstSeen = now,
                    lastSeen = now
                )
            } ?: return@forEach

            val identityId = mediaIdentityDao.upsertIdentity(entity)
            mediaIdentityDao.upsertPathHistory(
                MediaPathHistoryEntity(
                    identityId = identityId,
                    path = video.path,
                    normalizedPathKey = fingerprint.normalizedPathKey,
                    seenAt = now,
                    exists = true
                )
            )
        }
    }

    private suspend fun resolveMediaIdentity(
        video: VideoItem,
        fingerprint: MediaFingerprint
    ): MediaIdentityMatchDecision {
        val candidates = sequenceOf(
            mediaIdentityDao.getByCurrentVideoId(video.id),
            mediaIdentityDao.getByNormalizedPathKey(fingerprint.normalizedPathKey)
        )
            .plus(
                mediaIdentityDao.findFingerprintCandidates(
                    sizeBytes = fingerprint.sizeBytes,
                    durationMs = fingerprint.durationMs,
                    width = fingerprint.width,
                    height = fingerprint.height
                )
            )
            .filterNotNull()
            .distinctBy { it.identityId }
            .map { it.toIdentityCandidate() }
            .toList()

        return MediaIdentityMatcher.match(
            currentVideoId = video.id,
            current = fingerprint,
            candidates = candidates
        )
    }

    private fun MediaIdentityEntity.toIdentityCandidate(): MediaIdentityCandidate =
        MediaIdentityCandidate(
            identityId = identityId,
            currentVideoId = currentVideoId,
            fingerprint = MediaFingerprint(
                normalizedPathKey = normalizedPathKey,
                normalizedTitleKey = normalizedTitleKey,
                sizeBytes = sizeBytes,
                durationMs = durationMs,
                width = width,
                height = height,
                timestamp = modifiedTime
            )
        )

    // History
    fun getHistory(): Flow<List<HistoryEntity>> = historyDao.getAll()

    suspend fun saveHistory(
        video: VideoItem,
        position: Long,
        speed: Float,
        aspectRatioKey: String,
        contentFrameKey: String,
        externalSubtitleUri: String,
        subtitlesEnabled: Boolean,
        audioTrackGroupIndex: Int,
        audioTrackIndex: Int,
        audioMuted: Boolean
    ) {
        historyDao.upsert(
            HistoryEntity(
                videoId = video.id,
                title = video.title,
                path = video.path,
                duration = video.duration,
                lastPosition = position,
                timestamp = System.currentTimeMillis(),
                speed = speed,
                aspectRatioKey = aspectRatioKey,
                contentFrameKey = contentFrameKey,
                externalSubtitleUri = externalSubtitleUri,
                subtitlesEnabled = subtitlesEnabled,
                audioTrackGroupIndex = audioTrackGroupIndex,
                audioTrackIndex = audioTrackIndex,
                audioMuted = audioMuted
            )
        )
    }

    suspend fun getHistory(videoId: Long): HistoryEntity? = historyDao.getByVideoId(videoId)

    suspend fun clearHistory() = historyDao.deleteAll()

    suspend fun pruneStaleHistory(scannedVideos: List<VideoItem>) {
        val history = historyDao.getAll().first()
        if (history.isEmpty()) return
        val scannedVideoIds = scannedVideos.map { it.id }.toSet()
        val scannedPaths = scannedVideos.map { it.path.trim().replace('\\', '/').trimEnd('/') }.toSet()
        val staleIds = HistoryCleanupPolicy.videoIdsToRemove(
            history = history,
            scannedVideoIds = scannedVideoIds,
            scannedPaths = scannedPaths,
            localFileExists = { path -> File(path).exists() }
        )
        staleIds.forEach { historyDao.delete(it) }
    }

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
        const val MILLIS_PER_SECOND = 1_000L
    }
}
