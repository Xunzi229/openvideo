package com.example.openvideo.core.player

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

internal class PlayerMediaExportController(
    private val context: Context
) {
    fun takeScreenshot(videoView: android.view.View, callback: (Boolean, String?) -> Unit) {
        when (videoView) {
            is TextureView -> {
                val bitmap = videoView.bitmap ?: run {
                    callback(false, null)
                    return
                }
                saveScreenshot(bitmap, callback)
                bitmap.recycle()
            }
            is SurfaceView -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    callback(false, null)
                    return
                }
                val bitmap = Bitmap.createBitmap(videoView.width, videoView.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(
                    videoView,
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            saveScreenshot(bitmap, callback)
                        } else {
                            callback(false, null)
                        }
                        bitmap.recycle()
                    },
                    Handler(Looper.getMainLooper())
                )
            }
            else -> callback(false, null)
        }
    }

    fun exportClip(sourceUri: Uri, startMs: Long, endMs: Long, callback: (Boolean, String?) -> Unit) {
        Thread {
            val result = runCatching {
                if (startMs < 0L || endMs <= startMs) return@runCatching null
                val source = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return@runCatching null
                source.use { descriptor ->
                    val extractor = MediaExtractor()
                    val muxer: MediaMuxer
                    val outputFile = clipOutputFile()
                    try {
                        extractor.setDataSource(descriptor.fileDescriptor)
                        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    } catch (error: Exception) {
                        extractor.release()
                        throw error
                    }

                    val trackMap = mutableMapOf<Int, Int>()
                    for (trackIndex in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(trackIndex)
                        val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                        if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                            extractor.selectTrack(trackIndex)
                            trackMap[trackIndex] = muxer.addTrack(format)
                        }
                    }
                    if (trackMap.isEmpty()) {
                        muxer.release()
                        extractor.release()
                        return@runCatching null
                    }

                    val startUs = startMs * 1000L
                    val endUs = endMs * 1000L
                    val buffer = ByteBuffer.allocate(CLIP_BUFFER_BYTES)
                    val info = MediaCodec.BufferInfo()

                    muxer.start()
                    extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    var muxerStarted = true
                    try {
                        while (true) {
                            val trackIndex = extractor.sampleTrackIndex
                            if (trackIndex < 0) break
                            val sampleTimeUs = extractor.sampleTime
                            if (sampleTimeUs > endUs) break
                            val outputTrack = trackMap[trackIndex]
                            if (outputTrack != null) {
                                buffer.clear()
                                val sampleSize = extractor.readSampleData(buffer, 0)
                                if (sampleSize < 0) break
                                var bufferFlags = 0
                                if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                                    bufferFlags = bufferFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                                }
                                if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                                    bufferFlags = bufferFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                                }
                                info.set(
                                    0,
                                    sampleSize,
                                    (sampleTimeUs - startUs).coerceAtLeast(0L),
                                    bufferFlags
                                )
                                muxer.writeSampleData(outputTrack, buffer, info)
                            }
                            extractor.advance()
                        }
                    } finally {
                        runCatching {
                            if (muxerStarted) {
                                muxer.stop()
                                muxerStarted = false
                            }
                        }
                        muxer.release()
                        extractor.release()
                    }
                    outputFile.absolutePath
                }
            }.getOrNull()

            Handler(Looper.getMainLooper()).post {
                callback(result != null, result)
            }
        }.start()
    }

    private fun saveScreenshot(bitmap: Bitmap, callback: (Boolean, String?) -> Unit) {
        val name = "screenshot_${System.currentTimeMillis()}.jpg"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/OpenVideo")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    values
                ) ?: run {
                    callback(false, null)
                    return
                }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                } ?: run {
                    callback(false, null)
                    return
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                callback(true, uri.toString())
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OpenVideo")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, name)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                callback(true, file.absolutePath)
            }
        } catch (_: Exception) {
            callback(false, null)
        }
    }

    private fun clipOutputFile(): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "OpenVideo")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "clip_${System.currentTimeMillis()}.mp4")
    }

    private companion object {
        const val CLIP_BUFFER_BYTES = 1024 * 1024
    }
}
