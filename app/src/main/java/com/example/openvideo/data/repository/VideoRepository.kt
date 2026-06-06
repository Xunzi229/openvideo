package com.example.openvideo.data.repository

import android.app.PendingIntent
import com.example.openvideo.core.metadata.EpisodeMatchConfidence
import com.example.openvideo.core.metadata.EpisodeNameParser
import com.example.openvideo.core.metadata.LocalArtworkCandidateScanner
import com.example.openvideo.core.metadata.LocalArtworkFinder
import com.example.openvideo.core.mediaid.MediaFingerprint
import com.example.openvideo.core.mediaid.MediaFingerprintPolicy
import com.example.openvideo.core.mediaid.MediaIdentityCandidate
import com.example.openvideo.core.mediaid.MediaIdentityMatchDecision
import com.example.openvideo.core.mediaid.MediaIdentityMatcher
import com.example.openvideo.core.mediaid.MediaPathNormalizer
import com.example.openvideo.core.network.NetworkRecentUrlPolicy
import com.example.openvideo.core.network.WebDavConnectionPolicy
import com.example.openvideo.core.prefs.WebDavCredentialStore
import com.example.openvideo.data.local.EpisodeEntity
import com.example.openvideo.data.local.FavoriteDao
import com.example.openvideo.data.local.FavoriteEntity
import com.example.openvideo.data.local.HistoryDao
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.local.MediaIdentityDao
import com.example.openvideo.data.local.MediaIdentityEntity
import com.example.openvideo.data.local.MediaPathHistoryEntity
import com.example.openvideo.data.local.MediaSourceDao
import com.example.openvideo.data.local.MediaSourceEntity
import com.example.openvideo.data.local.NetworkRecentItemDao
import com.example.openvideo.data.local.NetworkRecentItemEntity
import com.example.openvideo.data.local.PlaylistDao
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.data.local.SeriesEntity
import com.example.openvideo.data.local.SeriesEpisodeDao
import com.example.openvideo.data.local.SeriesEpisodePlaybackEntity
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
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoScanner: VideoScanner,
    private val historyDao: HistoryDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val mediaIdentityDao: MediaIdentityDao,
    private val mediaSourceDao: MediaSourceDao,
    private val seriesEpisodeDao: SeriesEpisodeDao,
    private val networkRecentItemDao: NetworkRecentItemDao,
    private val webDavCredentialStore: WebDavCredentialStore
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

    fun getAllSeries(): Flow<List<SeriesEntity>> = seriesEpisodeDao.getAllSeries()

    fun getEpisodesForSeries(seriesId: Long): Flow<List<EpisodeEntity>> =
        seriesEpisodeDao.getEpisodesForSeries(seriesId)

    fun getPlayableEpisodesForSeries(seriesId: Long): Flow<List<SeriesEpisodePlaybackEntity>> =
        seriesEpisodeDao.getPlayableEpisodesForSeries(seriesId)

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
                    fileExists = true
                )
            )
            syncSeriesEpisode(video = video, identityId = identityId, now = now)
        }
    }

    private suspend fun syncSeriesEpisode(video: VideoItem, identityId: Long, now: Long) {
        val normalizedPath = MediaPathNormalizer.normalize(video.path) ?: return
        val match = EpisodeNameParser.parse(
            fileName = normalizedPath.fileName,
            parentFolderName = parentFolderName(normalizedPath.displayPath)
        ) ?: return
        if (match.confidence == EpisodeMatchConfidence.LOW) return
        val folderPath = normalizedPath.parentKey
        val normalizedTitleKey = match.title.lowercase(Locale.ROOT)
        val existingSeries = seriesEpisodeDao.getSeriesByKey(
            normalizedTitleKey = normalizedTitleKey,
            folderPath = folderPath
        )
        val discoveredPosterPath = LocalArtworkFinder.find(
            videoPath = video.path,
            candidatePaths = LocalArtworkCandidateScanner.candidatesNear(video.path)
        )
        val seriesId = seriesEpisodeDao.insertSeries(
            existingSeries?.copy(
                title = match.title,
                posterPath = resolveSeriesPosterPath(
                    existingPosterPath = existingSeries?.posterPath,
                    discoveredPosterPath = discoveredPosterPath
                ),
                updatedAt = now
            ) ?: SeriesEntity(
                title = match.title,
                normalizedTitleKey = normalizedTitleKey,
                folderPath = folderPath,
                posterPath = resolveSeriesPosterPath(
                    existingPosterPath = existingSeries?.posterPath,
                    discoveredPosterPath = discoveredPosterPath
                ),
                createdAt = now,
                updatedAt = now
            )
        )
        val existingEpisode = seriesEpisodeDao.getEpisodeByIdentityId(identityId)
        seriesEpisodeDao.upsertEpisode(
            EpisodeEntity(
                episodeId = existingEpisode?.episodeId ?: 0L,
                seriesId = seriesId,
                identityId = identityId,
                season = match.season,
                episodeStart = match.episodeStart,
                episodeEnd = match.episodeEnd,
                episodeTitle = "",
                confidence = match.confidence.name,
                rule = match.rule,
                createdAt = existingEpisode?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    private fun resolveSeriesPosterPath(existingPosterPath: String?, discoveredPosterPath: String?): String? {
        if (!discoveredPosterPath.isNullOrBlank()) return discoveredPosterPath
        return existingPosterPath
            ?.takeIf { it.startsWith("content://", ignoreCase = true) || File(it).exists() }
    }

    private fun parentFolderName(displayPath: String): String? =
        displayPath.substringBeforeLast('/', missingDelimiterValue = "")
            .substringAfterLast('/')
            .takeIf { it.isNotBlank() }

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

    private suspend fun resolveMediaIdentityId(video: VideoItem): Long? {
        val fingerprint = MediaFingerprintPolicy.fromFields(
            title = video.title,
            pathOrUri = video.path,
            sizeBytes = video.size,
            durationMs = video.duration,
            width = video.width,
            height = video.height,
            timestamp = video.dateAdded * MILLIS_PER_SECOND
        ) ?: return null

        return when (val decision = resolveMediaIdentity(video, fingerprint)) {
            is MediaIdentityMatchDecision.Matched -> decision.identityId
            is MediaIdentityMatchDecision.Conflict -> null
            MediaIdentityMatchDecision.NoMatch -> null
        }
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

    fun getMediaSources(): Flow<List<MediaSourceEntity>> = mediaSourceDao.getAll()

    suspend fun getMediaSource(sourceId: Long): MediaSourceEntity? = mediaSourceDao.getById(sourceId)

    fun getWebDavCredentials(sourceId: Long): WebDavCredentialStore.Credentials? =
        webDavCredentialStore.read(sourceId)

    suspend fun deleteMediaSource(sourceId: Long) {
        webDavCredentialStore.delete(sourceId)
        mediaSourceDao.delete(sourceId)
    }

    fun getNetworkRecentUrls(): Flow<List<NetworkRecentItemEntity>> = networkRecentItemDao.getAll()

    suspend fun recordNetworkRecentUrl(normalizedUrl: String, title: String) {
        val now = System.currentTimeMillis()
        val displayUrl = NetworkRecentUrlPolicy.displayUrlFor(normalizedUrl)
        val displayTitle = title.ifBlank { NetworkRecentUrlPolicy.titleFor(normalizedUrl) }
        val existing = networkRecentItemDao.getByNormalizedUrl(normalizedUrl)
        networkRecentItemDao.upsert(
            existing?.copy(
                uri = normalizedUrl,
                displayUrl = displayUrl,
                title = displayTitle,
                lastPlayedAt = now,
                updatedAt = now
            ) ?: NetworkRecentItemEntity(
                uri = normalizedUrl,
                normalizedUrl = normalizedUrl,
                displayUrl = displayUrl,
                title = displayTitle,
                lastPlayedAt = now
            )
        )
        val existingSource = mediaSourceDao.getByTypeAndUrl(type = "url", normalizedUrl = normalizedUrl)
        mediaSourceDao.upsert(
            existingSource?.copy(
                name = displayTitle,
                url = normalizedUrl,
                displayUrl = displayUrl,
                lastUsedAt = now,
                updatedAt = now
            ) ?: MediaSourceEntity(
                type = "url",
                name = displayTitle,
                url = normalizedUrl,
                normalizedUrl = normalizedUrl,
                displayUrl = displayUrl,
                lastUsedAt = now,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun addWebDavSource(
        baseUrl: String,
        name: String,
        username: String,
        password: String
    ): Long {
        val validation = WebDavConnectionPolicy.validateBaseUrl(baseUrl)
        require(validation is WebDavConnectionPolicy.Validation.Valid) { "Invalid WebDAV base URL" }
        val credentials = WebDavConnectionPolicy.validateCredentials(username, password)
        require(credentials is WebDavConnectionPolicy.CredentialValidation.Valid) { "Invalid WebDAV credentials" }

        val now = System.currentTimeMillis()
        val normalizedBaseUrl = validation.normalizedBaseUrl
        val displayName = name.trim().ifBlank { WebDavConnectionPolicy.displayNameFor(normalizedBaseUrl) }
        val existing = mediaSourceDao.getByTypeAndUrl(type = MEDIA_SOURCE_TYPE_WEBDAV, normalizedUrl = normalizedBaseUrl)
        val sourceId = mediaSourceDao.upsert(
            existing?.copy(
                name = displayName,
                url = normalizedBaseUrl,
                normalizedUrl = normalizedBaseUrl,
                displayUrl = normalizedBaseUrl,
                lastUsedAt = now,
                updatedAt = now
            ) ?: MediaSourceEntity(
                type = MEDIA_SOURCE_TYPE_WEBDAV,
                name = displayName,
                url = normalizedBaseUrl,
                normalizedUrl = normalizedBaseUrl,
                displayUrl = normalizedBaseUrl,
                lastUsedAt = now,
                createdAt = now,
                updatedAt = now
            )
        )
        val resolvedSourceId = if (sourceId > 0L) sourceId else existing?.sourceId ?: sourceId
        webDavCredentialStore.save(
            sourceId = resolvedSourceId,
            username = username,
            password = password
        )
        return resolvedSourceId
    }

    suspend fun clearNetworkRecentUrls() = networkRecentItemDao.deleteAll()

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
                mediaIdentityId = resolveMediaIdentityId(video),
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

    suspend fun getHistory(videoId: Long): HistoryEntity? =
        historyDao.getByVideoId(videoId) ?: mediaIdentityDao.getByCurrentVideoId(videoId)
            ?.let { identity -> historyDao.getByMediaIdentityId(identity.identityId) }

    suspend fun clearHistory() = historyDao.deleteAll()

    suspend fun pruneStaleHistory(scannedVideos: List<VideoItem>) {
        val history = historyDao.getAll().first()
        if (history.isEmpty()) return
        val scannedVideoIds = scannedVideos.map { it.id }.toSet()
        val scannedPaths = scannedVideos.map { it.path.trim().replace('\\', '/').trimEnd('/') }.toSet()
        val activeMediaIdentityIds = activeHistoryIdentityIds(
            history = history,
            scannedVideoIds = scannedVideoIds
        )
        val staleIds = HistoryCleanupPolicy.videoIdsToRemove(
            history = history,
            scannedVideoIds = scannedVideoIds,
            scannedPaths = scannedPaths,
            activeMediaIdentityIds = activeMediaIdentityIds,
            localFileExists = { path -> File(path).exists() }
        )
        staleIds.forEach { historyDao.delete(it) }
    }

    private suspend fun activeHistoryIdentityIds(
        history: List<HistoryEntity>,
        scannedVideoIds: Set<Long>
    ): Set<Long> {
        val identityIds = history.mapNotNull { it.mediaIdentityId }.distinct()
        if (identityIds.isEmpty()) return emptySet()
        return mediaIdentityDao.getByIdentityIds(identityIds)
            .filter { it.currentVideoId in scannedVideoIds }
            .map { it.identityId }
            .toSet()
    }

    // Favorites
    fun getFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllWithIdentityFallback()

    suspend fun toggleFavorite(video: VideoItem) {
        val identityId = resolveMediaIdentityId(video)
        if (favoriteDao.isFavorite(video.id)) {
            favoriteDao.delete(video.id)
        } else if (identityId != null && favoriteDao.isFavoriteByMediaIdentityId(identityId)) {
            favoriteDao.deleteByMediaIdentityId(identityId)
        } else {
            favoriteDao.insert(
                FavoriteEntity(
                    videoId = video.id,
                    mediaIdentityId = identityId,
                    title = video.title,
                    path = video.path,
                    duration = video.duration,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun isFavorite(videoId: Long): Boolean =
        favoriteDao.isFavorite(videoId) || mediaIdentityDao.getByCurrentVideoId(videoId)
            ?.let { identity -> favoriteDao.isFavoriteByMediaIdentityId(identity.identityId) } == true

    suspend fun addToQuickPlaylist(video: VideoItem): Long {
        val playlist = playlistDao.getAll().first().firstOrNull { it.name == QUICK_PLAYLIST_NAME }
        val playlistId = playlist?.id ?: playlistDao.insert(PlaylistEntity(name = QUICK_PLAYLIST_NAME))
        val position = playlistDao.getVideoCount(playlistId)
        playlistDao.insertVideo(
            PlaylistVideoEntity(
                playlistId = playlistId,
                videoId = video.id,
                mediaIdentityId = resolveMediaIdentityId(video),
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
        const val MEDIA_SOURCE_TYPE_WEBDAV = "webdav"
    }
}
