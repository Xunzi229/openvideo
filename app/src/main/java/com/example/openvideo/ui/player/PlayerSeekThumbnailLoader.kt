package com.example.openvideo.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PlayerSeekThumbnailLoader(private val context: Context) {

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

            val bitmap = withContext(Dispatchers.IO) {
                var retriever: MediaMetadataRetriever? = null
                try {
                    retriever = MediaMetadataRetriever().apply {
                        setDataSource(context, videoUri)
                    }
                    val timeUs = TimeUnit.MILLISECONDS.toMicros(positionMs)
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (_: Exception) {
                    null
                } finally {
                    try {
                        retriever?.release()
                    } catch (_: Exception) {
                        // Ignore release failures from platform retriever cleanup.
                    }
                }
            }

            if (isActive) {
                val previewBitmap = bitmap?.let(::scaleForPreview)
                if (previewBitmap != null) {
                    PlayerSeekThumbnailMemoryCache.put(cacheKey, previewBitmap)
                }
                onLoaded(previewBitmap)
            }
        }
    }

    private fun scaleForPreview(bitmap: Bitmap): Bitmap {
        val target = PlayerSeekThumbnailPolicy.scaledThumbnailSize(bitmap.width, bitmap.height) ?: return bitmap
        if (target.first == bitmap.width && target.second == bitmap.height) return bitmap
        return Bitmap.createScaledBitmap(bitmap, target.first, target.second, true)
    }

    fun release() {
        activeJob?.cancel()
        activeJob = null
        scope.cancel()
    }
}
