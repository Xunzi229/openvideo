package com.example.openvideo.data.scanner

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.home.MediaLibraryPermissionPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed class VideoDeleteResult {
    data class Deleted(val uris: Set<Uri>) : VideoDeleteResult()
    data class RequiresUserAction(val pendingIntent: PendingIntent) : VideoDeleteResult()
    object Failed : VideoDeleteResult()
}

@Singleton
class VideoScanner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val cacheMutex = Mutex()
    @Volatile
    private var videoCache: Map<Long, VideoItem> = emptyMap()

    fun scanVideos(): Flow<VideoScanOutcome> = callbackFlow {
        val refreshRequests = Channel<Unit>(Channel.CONFLATED)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshRequests.trySend(Unit)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                refreshRequests.trySend(Unit)
            }
        }

        context.contentResolver.registerContentObserver(videoCollectionUri(), true, observer)

        val scanJob = launch(Dispatchers.IO) {
            emitScanResults(this@callbackFlow)
            refreshRequests.receiveAsFlow().collectLatest {
                delay(MediaStoreRefreshPolicy.debounceDelayMs())
                emitScanResults(this@callbackFlow)
            }
        }

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
            refreshRequests.close()
            scanJob.cancel()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun emitScanResults(channel: ProducerScope<VideoScanOutcome>) {
        if (!hasVideoReadPermission()) {
            clearVideoCache()
            channel.send(VideoScanOutcome.PermissionDenied)
            return
        }
        try {
            val videos = refreshVideos { count ->
                channel.send(VideoScanOutcome.Progress(count))
            }
            channel.send(VideoScanOutcome.Success(videos))
        } catch (_: SecurityException) {
            clearVideoCache()
            channel.send(VideoScanOutcome.PermissionDenied)
        } catch (error: Exception) {
            channel.send(VideoScanOutcome.Error(error.message ?: "MediaStore scan failed"))
        }
    }

    private suspend fun clearVideoCache() {
        cacheMutex.withLock { videoCache = emptyMap() }
    }

    private suspend fun replaceVideoCache(videos: List<VideoItem>) {
        cacheMutex.withLock { videoCache = videos.associateBy { it.id } }
    }

    private suspend fun readVideoCache(): Map<Long, VideoItem> =
        cacheMutex.withLock { videoCache }

    private suspend fun readVideoCacheSize(): Int =
        cacheMutex.withLock { videoCache.size }

    private suspend fun refreshVideos(onProgress: (suspend (Int) -> Unit)? = null): List<VideoItem> {
        val cachedCount = readVideoCacheSize()
        val reportProgress = if (MediaStoreRefreshPolicy.shouldReportFullScanProgress(cachedCount)) {
            onProgress
        } else {
            null
        }
        if (!MediaStoreRefreshPolicy.shouldUseIncrementalRefresh(cachedCount)) {
            return queryVideos(onProgress = onProgress).also { videos ->
                replaceVideoCache(videos)
            }
        }
        val currentIndex = queryVideoIndex()
        val previousIndex = readVideoCache().mapValues { (_, video) -> MediaStoreIndexEntry.fromVideo(video) }
        val diff = MediaStoreDiffPolicy.diff(previousIndex, currentIndex)
        if (MediaStoreRefreshPolicy.shouldFallbackToFullScan(cachedCount, diff)) {
            return queryVideos(onProgress = reportProgress).also { videos ->
                replaceVideoCache(videos)
            }
        }
        if (diff.mutationCount == 0) {
            return readVideoCache().values.sortedByDescending { it.dateAdded }
        }
        val idsToFetch = diff.addedIds + diff.changedIds
        val fetched = if (idsToFetch.isEmpty()) {
            emptyMap()
        } else {
            queryVideosByIds(idsToFetch).associateBy { it.id }
        }
        val merged = MediaStoreDiffPolicy.mergeCachedVideos(readVideoCache(), diff, fetched)
        replaceVideoCache(merged)
        return merged
    }

    private fun hasVideoReadPermission(): Boolean =
        MediaLibraryPermissionPolicy.hasReadAccess(
            isPermissionGranted = { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        )

    private fun queryVideoIndex(): Map<Long, MediaStoreIndexEntry> {
        val index = linkedMapOf<Long, MediaStoreIndexEntry>()
        val collection = videoCollectionUri()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED
        )
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                index[id] = MediaStoreIndexEntry(
                    id = id,
                    displayName = cursor.getString(nameCol) ?: "Unknown",
                    dateAdded = cursor.getLong(dateCol),
                    duration = cursor.getLong(durationCol),
                    size = cursor.getLong(sizeCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol)
                )
            }
        }
        return index
    }

    private suspend fun queryVideos(onProgress: (suspend (Int) -> Unit)? = null): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val collection = videoCollectionUri()
        val projection = videoProjection()
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        var lastEmittedCount = 0
        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            while (cursor.moveToNext()) {
                readVideoItem(cursor, collection)?.let { video ->
                    videos.add(video)
                    val scannedCount = videos.size
                    if (onProgress != null &&
                        MediaStoreRefreshPolicy.shouldEmitScanProgress(scannedCount, lastEmittedCount)
                    ) {
                        onProgress(scannedCount)
                        lastEmittedCount = scannedCount
                    }
                }
            }
        }
        if (onProgress != null && videos.size > lastEmittedCount) {
            onProgress(videos.size)
        }
        return videos
    }

    private fun queryVideosByIds(ids: Set<Long>): List<VideoItem> {
        if (ids.isEmpty()) return emptyList()
        val collection = videoCollectionUri()
        return ids.chunked(SQLITE_MAX_VARIABLES).flatMap { batch ->
            queryVideosByIdsBatch(batch.toSet(), collection)
        }
    }

    private fun queryVideosByIdsBatch(ids: Set<Long>, collection: Uri): List<VideoItem> {
        if (ids.isEmpty()) return emptyList()
        val videos = mutableListOf<VideoItem>()
        val projection = videoProjection()
        val placeholders = ids.joinToString(",") { "?" }
        val selection = "${MediaStore.Video.Media._ID} IN ($placeholders)"
        val selectionArgs = ids.map { it.toString() }.toTypedArray()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                readVideoItem(cursor, collection)?.let { videos.add(it) }
            }
        }
        return videos
    }

    private fun videoProjection(): Array<String> = buildList {
        add(MediaStore.Video.Media._ID)
        add(MediaStore.Video.Media.DISPLAY_NAME)
        add(MediaStore.Video.Media.DATA)
        add(MediaStore.Video.Media.DURATION)
        add(MediaStore.Video.Media.SIZE)
        add(MediaStore.Video.Media.WIDTH)
        add(MediaStore.Video.Media.HEIGHT)
        add(MediaStore.Video.Media.DATE_ADDED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.Video.Media.ORIENTATION)
        }
    }.toTypedArray()

    private fun readVideoItem(cursor: android.database.Cursor, collection: Uri = videoCollectionUri()): VideoItem? {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val orientationCol = cursor.getColumnIndex(MediaStore.Video.Media.ORIENTATION)

        val id = cursor.getLong(idCol)
        val name = cursor.getString(nameCol) ?: "Unknown"
        val path = cursor.getString(dataCol) ?: ""
        val duration = cursor.getLong(durationCol)
        val size = cursor.getLong(sizeCol)
        val rawWidth = cursor.getInt(widthCol)
        val rawHeight = cursor.getInt(heightCol)
        val orientation = if (orientationCol >= 0) cursor.getInt(orientationCol) else 0
        val (width, height) = MediaStoreVideoDimensionsPolicy.displayDimensions(
            width = rawWidth,
            height = rawHeight,
            orientationDegrees = orientation
        )
        val dateAdded = cursor.getLong(dateCol)

        val contentUri = ContentUris.withAppendedId(collection, id)
        val thumbnailUri = ContentUris.withAppendedId(collection, id)
        return VideoItem(
            id = id,
            title = name,
            path = path,
            uri = contentUri,
            duration = duration,
            size = size,
            width = width,
            height = height,
            dateAdded = dateAdded,
            thumbnailUri = thumbnailUri
        )
    }

    private fun videoCollectionUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    }

    fun deleteVideo(uri: Uri): Boolean {
        val deleted = deleteVideoWithResult(uri) is VideoDeleteResult.Deleted
        if (deleted) {
            removeCachedVideo(uri)
        }
        return deleted
    }

    fun deleteVideos(uris: List<Uri>): VideoDeleteResult {
        if (uris.isEmpty()) return VideoDeleteResult.Deleted(emptySet())

        createDeleteRequest(uris)?.let { request ->
            return VideoDeleteResult.RequiresUserAction(request)
        }

        val deleted = mutableSetOf<Uri>()
        for (uri in uris) {
            when (val result = deleteVideoWithResult(uri)) {
                is VideoDeleteResult.Deleted -> deleted += result.uris
                is VideoDeleteResult.RequiresUserAction -> return result
                VideoDeleteResult.Failed -> Unit
            }
        }

        deleted.forEach { removeCachedVideo(it) }
        return if (deleted.isNotEmpty()) {
            VideoDeleteResult.Deleted(deleted)
        } else {
            VideoDeleteResult.Failed
        }
    }

    @Synchronized
    private fun removeCachedVideo(uri: Uri) {
        val id = ContentUris.parseId(uri)
        if (videoCache.isEmpty()) return
        videoCache = videoCache.filterKeys { key -> key != id }
    }

    private fun deleteVideoWithResult(uri: Uri): VideoDeleteResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deleteVideoWithScopedStorageFallback(uri)
        } else {
            deleteVideoLegacy(uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteVideoWithScopedStorageFallback(uri: Uri): VideoDeleteResult {
        return try {
            if (context.contentResolver.delete(uri, null, null) > 0) {
                VideoDeleteResult.Deleted(setOf(uri))
            } else {
                VideoDeleteResult.Failed
            }
        } catch (e: RecoverableSecurityException) {
            VideoDeleteResult.RequiresUserAction(e.userAction.actionIntent)
        } catch (e: Exception) {
            VideoDeleteResult.Failed
        }
    }

    private fun deleteVideoLegacy(uri: Uri): VideoDeleteResult {
        return try {
            if (context.contentResolver.delete(uri, null, null) > 0) {
                VideoDeleteResult.Deleted(setOf(uri))
            } else {
                VideoDeleteResult.Failed
            }
        } catch (e: Exception) {
            VideoDeleteResult.Failed
        }
    }

    private companion object {
        private const val SQLITE_MAX_VARIABLES = 200
    }

    fun createDeleteRequest(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        } else {
            null
        }
    }

}
