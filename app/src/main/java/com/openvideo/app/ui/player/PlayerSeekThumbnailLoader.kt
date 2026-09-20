package com.openvideo.app.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PlayerSeekThumbnailLoader(private val context: Context) {

    companion object {
        // Native extraction does not stop when its coroutine is cancelled.
        private val extractionMutex = Mutex()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeJob: Job? = null

    fun loadThumbnail(videoUri: Uri, positionMs: Long, onLoaded: (Bitmap?) -> Unit) {
        activeJob?.cancel()

        val cacheKey = PlayerSeekThumbnailPolicy.thumbnailCacheKey(videoUri.toString(), positionMs)
        PlayerSeekThumbnailMemoryCache.get(cacheKey)?.let { cached ->
            onLoaded(cached)
            return
        }

        activeJob = scope.launch {
            delay(PlayerSeekThumbnailPolicy.throttleIntervalMs())

            var previewBitmap: Bitmap? = null
            try {
                withContext(Dispatchers.IO) {
                    extractionMutex.withLock {
                        ensureActive()
                        var retriever: MediaMetadataRetriever? = null
                        try {
                            retriever = MediaMetadataRetriever().apply {
                                setDataSource(context, videoUri)
                            }
                            val timeUs = TimeUnit.MILLISECONDS.toMicros(positionMs)
                            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                retriever.getScaledFrameAtTime(
                                    timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                    PlayerSeekThumbnailPolicy.maxPreviewWidthPx(),
                                    PlayerSeekThumbnailPolicy.maxPreviewHeightPx()
                                )
                            } else {
                                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            }
                            previewBitmap = bitmap?.let(::scaleForPreview)
                        } catch (_: OutOfMemoryError) {
                            PlayerSeekThumbnailMemoryCache.clear()
                        } catch (_: Exception) {
                            // A preview failure must not interrupt playback.
                        } finally {
                            try {
                                retriever?.release()
                            } catch (_: Exception) {
                                // Ignore release failures from platform retriever cleanup.
                            }
                        }
                    }
                }

                if (isActive) {
                    val result = previewBitmap
                    if (result != null) {
                        PlayerSeekThumbnailMemoryCache.put(cacheKey, result)
                    }
                    // Ownership passes to the cache/UI before invoking client code.
                    previewBitmap = null
                    onLoaded(result)
                }
            } finally {
                // Includes cancellation during the IO -> Main dispatcher handoff.
                previewBitmap?.recycle()
            }
        }
    }

    internal fun scaleForPreview(bitmap: Bitmap): Bitmap {
        val target = PlayerSeekThumbnailPolicy.scaledThumbnailSize(bitmap.width, bitmap.height) ?: return bitmap
        if (target.first == bitmap.width && target.second == bitmap.height) return bitmap
        return try {
            Bitmap.createScaledBitmap(bitmap, target.first, target.second, true)
        } finally {
            bitmap.recycle()
        }
    }

    fun release() {
        activeJob?.cancel()
        activeJob = null
        scope.cancel()
    }
}
