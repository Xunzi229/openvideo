package com.example.openvideo.data.scanner

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
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

    private val cacheLock = Any()
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

    private fun clearVideoCache() {
        synchronized(cacheLock) { videoCache = emptyMap() }
    }

    private fun replaceVideoCache(videos: List<VideoItem>) {
        synchronized(cacheLock) { videoCache = videos.associateBy { it.id } }
    }

    private fun readVideoCache(): Map<Long, VideoItem> =
        synchronized(cacheLock) { videoCache }

    private fun readVideoCacheSize(): Int =
        synchronized(cacheLock) { videoCache.size }

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
            mediaStorePathColumn(),
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.ORIENTATION
        )
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndex(mediaStorePathColumn())
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val modifiedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
            val orientationCol = cursor.getColumnIndex(MediaStore.Video.Media.ORIENTATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val size = cursor.getLong(sizeCol)
                if (size <= 0L) continue
                val displayName = cursor.getString(nameCol) ?: "Unknown"
                val contentUri = ContentUris.withAppendedId(collection, id)
                val pathValue = cursor.stringOrEmpty(pathCol)
                val dateAdded = cursor.getLong(dateCol)
                val orientation = cursor.intOrDefault(orientationCol)
                val dimensions = MediaStoreVideoDimensionsPolicy.displayDimensions(
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    orientationDegrees = orientation
                )
                index[id] = MediaStoreIndexEntry(
                    id = id,
                    displayName = displayName,
                    libraryPath = mediaStoreLibraryPath(
                        pathValue = pathValue,
                        displayName = displayName,
                        contentUri = contentUri
                    ),
                    dateAdded = dateAdded,
                    dateModified = cursor.longOrDefault(modifiedCol, dateAdded),
                    duration = cursor.getLong(durationCol),
                    size = size,
                    width = dimensions.first,
                    height = dimensions.second,
                    orientationDegrees = orientation
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
        add(mediaStorePathColumn())
        add(MediaStore.Video.Media.DURATION)
        add(MediaStore.Video.Media.SIZE)
        add(MediaStore.Video.Media.WIDTH)
        add(MediaStore.Video.Media.HEIGHT)
        add(MediaStore.Video.Media.DATE_ADDED)
        add(MediaStore.Video.Media.DATE_MODIFIED)
        add(MediaStore.Video.Media.ORIENTATION)
    }.toTypedArray()

    private fun readVideoItem(cursor: android.database.Cursor, collection: Uri = videoCollectionUri()): VideoItem? {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val pathCol = cursor.getColumnIndex(mediaStorePathColumn())
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val modifiedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
        val orientationCol = cursor.getColumnIndex(MediaStore.Video.Media.ORIENTATION)

        val id = cursor.getLong(idCol)
        val name = cursor.getString(nameCol) ?: "Unknown"
        val pathValue = cursor.stringOrEmpty(pathCol)
        val duration = cursor.getLong(durationCol)
        val size = cursor.getLong(sizeCol)
        if (size <= 0L) return null
        val rawWidth = cursor.getInt(widthCol)
        val rawHeight = cursor.getInt(heightCol)
        val orientation = if (orientationCol >= 0) cursor.getInt(orientationCol) else 0
        val (width, height) = MediaStoreVideoDimensionsPolicy.displayDimensions(
            width = rawWidth,
            height = rawHeight,
            orientationDegrees = orientation
        )
        val dateAdded = cursor.getLong(dateCol)
        val dateModified = cursor.longOrDefault(modifiedCol, dateAdded)

        val contentUri = ContentUris.withAppendedId(collection, id)
        val thumbnailUri = ContentUris.withAppendedId(collection, id)
        val path = MediaStorePathPolicy.playbackSource(
            sdkInt = Build.VERSION.SDK_INT,
            dataPath = pathValue,
            contentUri = contentUri.toString()
        )
        val libraryPath = mediaStoreLibraryPath(pathValue, name, contentUri)
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
            thumbnailUri = thumbnailUri,
            libraryPath = libraryPath,
            dateModified = dateModified,
            orientationDegrees = orientation
        )
    }

    private fun mediaStorePathColumn(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.RELATIVE_PATH
        } else {
            MediaStore.Video.Media.DATA
        }

    private fun mediaStoreLibraryPath(pathValue: String, displayName: String, contentUri: Uri): String =
        MediaStorePathPolicy.libraryPath(
            dataPath = pathValue.takeIf { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q }.orEmpty(),
            relativePath = pathValue.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q }.orEmpty(),
            displayName = displayName,
            externalStorageRoot = Environment.getExternalStorageDirectory().path,
            contentUri = contentUri.toString()
        )

    private fun Cursor.stringOrEmpty(columnIndex: Int): String =
        if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex).orEmpty() else ""

    private fun Cursor.longOrDefault(columnIndex: Int, defaultValue: Long = 0L): Long =
        if (columnIndex >= 0 && !isNull(columnIndex)) getLong(columnIndex) else defaultValue

    private fun Cursor.intOrDefault(columnIndex: Int, defaultValue: Int = 0): Int =
        if (columnIndex >= 0 && !isNull(columnIndex)) getInt(columnIndex) else defaultValue

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

    private fun removeCachedVideo(uri: Uri) {
        val id = ContentUris.parseId(uri)
        synchronized(cacheLock) {
            if (videoCache.isEmpty()) return
            videoCache = videoCache.filterKeys { key -> key != id }
        }
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
