package com.example.openvideo.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * 异步抽帧缩略图加载器，具备生命周期释放和节流控制能力。
 */
class PlayerSeekThumbnailLoader(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeJob: Job? = null

    /**
     * 异步加载指定视频 [videoUri] 在 [positionMs] 毫秒处的缩略图帧。
     *
     * 节流策略：新请求会立即取消之前的排队/加载任务，并延迟指定时间执行，避免频繁抽帧导致系统卡顿。
     */
    fun loadThumbnail(videoUri: Uri, positionMs: Long, onLoaded: (Bitmap?) -> Unit) {
        // 取消当前处于延迟或加载中的任务
        activeJob?.cancel()

        activeJob = scope.launch {
            // 节流延迟，防止用户连续拖动进度条时发起大量重复抽帧
            delay(PlayerSeekThumbnailPolicy.throttleIntervalMs())

            val bitmap = withContext(Dispatchers.IO) {
                var retriever: MediaMetadataRetriever? = null
                try {
                    retriever = MediaMetadataRetriever().apply {
                        setDataSource(context, videoUri)
                    }
                    val timeUs = TimeUnit.MILLISECONDS.toMicros(positionMs)
                    // OPTION_CLOSEST_SYNC 通常最快，适合拖动预览的实时性要求
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (_: Exception) {
                    null
                } finally {
                    try {
                        retriever?.release()
                    } catch (_: Exception) {}
                }
            }

            if (isActive) {
                onLoaded(bitmap)
            }
        }
    }

    /**
     * 释放加载器，取消所有未决的任务以防止内存泄漏。
     */
    fun release() {
        scope.cancel()
    }
}
