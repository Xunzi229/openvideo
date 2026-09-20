package com.openvideo.app.core.player

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
import java.util.concurrent.atomic.AtomicBoolean

internal class PlayerMediaExportController(
    private val context: Context
) {
    private val screenshotInProgress = AtomicBoolean(false)
    private val clipInProgress = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun takeScreenshot(videoView: android.view.View, callback: (Boolean, String?) -> Unit) {
        if (!screenshotInProgress.compareAndSet(false, true)) {
            callback(false, null)
            return
        }
        var pendingBitmap: Bitmap? = null
        try {
            when (videoView) {
                is TextureView -> {
                    val bitmap = videoView.bitmap
                    if (bitmap == null) {
                        screenshotInProgress.set(false)
                        callback(false, null)
                    } else saveScreenshot(bitmap, callback)
                }
                is SurfaceView -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || videoView.width <= 0 || videoView.height <= 0) {
                        screenshotInProgress.set(false)
                        callback(false, null)
                        return
                    }
                    val bitmap = Bitmap.createBitmap(videoView.width, videoView.height, Bitmap.Config.ARGB_8888)
                    pendingBitmap = bitmap
                    PixelCopy.request(videoView, bitmap, { result ->
                        if (result == PixelCopy.SUCCESS) saveScreenshot(bitmap, callback)
                        else {
                            bitmap.recycle()
                            screenshotInProgress.set(false)
                            callback(false, null)
                        }
                    }, mainHandler)
                    pendingBitmap = null
                }
                else -> { screenshotInProgress.set(false); callback(false, null) }
            }
        } catch (_: Exception) {
            pendingBitmap?.recycle()
            screenshotInProgress.set(false)
            callback(false, null)
        } catch (_: OutOfMemoryError) {
            pendingBitmap?.recycle()
            screenshotInProgress.set(false)
            callback(false, null)
        }
    }

    fun exportClip(sourceUri: Uri, startMs: Long, endMs: Long, callback: (Boolean, String?) -> Unit) {
        if (!clipInProgress.compareAndSet(false, true)) {
            callback(false, null)
            return
        }
        Thread {
            val result = runCatching {
                require(startMs >= 0L && endMs > startMs && endMs <= Long.MAX_VALUE / 1000L)
                val source = context.contentResolver.openFileDescriptor(sourceUri, "r")
                    ?: error("Cannot open clip source")
                source.use { descriptor ->
                    val outputFile = clipOutputFile()
                    MediaExportFile.write(outputFile) {
                        val extractor = MediaExtractor()
                        var muxer: MediaMuxer? = null
                        try {
                            extractor.setDataSource(descriptor.fileDescriptor)
                            val writer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                            muxer = writer
                            val trackMap = mutableMapOf<Int, Int>()
                            var bufferBytes = CLIP_BUFFER_BYTES
                            for (trackIndex in 0 until extractor.trackCount) {
                                val format = extractor.getTrackFormat(trackIndex)
                                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                                    extractor.selectTrack(trackIndex)
                                    trackMap[trackIndex] = writer.addTrack(format)
                                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                                        bufferBytes = maxOf(bufferBytes, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                                    }
                                }
                            }
                            require(trackMap.isNotEmpty()) { "No exportable tracks" }
                            require(bufferBytes <= MAX_CLIP_BUFFER_BYTES) { "Sample exceeds export limit" }
                            var buffer = ByteBuffer.allocate(bufferBytes)
                            val info = MediaCodec.BufferInfo()
                            val startUs = startMs * 1000L
                            val endUs = endMs * 1000L
                            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                            val actualStartUs = extractor.sampleTime
                            require(actualStartUs >= 0L && actualStartUs < endUs) { "No samples in clip" }
                            writer.start()
                            var writtenSamples = 0
                            while (true) {
                                val trackIndex = extractor.sampleTrackIndex
                                if (trackIndex < 0) break
                                val sampleTimeUs = extractor.sampleTime
                                if (sampleTimeUs > endUs) break
                                val outputTrack = trackMap[trackIndex]
                                if (outputTrack != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        val sampleSize = extractor.sampleSize
                                        require(sampleSize <= MAX_CLIP_BUFFER_BYTES) { "Sample exceeds export limit" }
                                        if (sampleSize > buffer.capacity()) buffer = ByteBuffer.allocate(sampleSize.toInt())
                                    }
                                    buffer.clear()
                                    val sampleSize = extractor.readSampleData(buffer, 0)
                                    if (sampleSize < 0) break
                                    var flags = 0
                                    if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                                        flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                                    }
                                    if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                                        flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                                    }
                                    info.set(0, sampleSize, ClipTimestampPolicy.relativeTimeUs(sampleTimeUs, actualStartUs), flags)
                                    writer.writeSampleData(outputTrack, buffer, info)
                                    writtenSamples++
                                }
                                extractor.advance()
                            }
                            require(writtenSamples > 0) { "Empty clip" }
                            // A failed finalization is a failed export, never a successful path.
                            writer.stop()
                        } finally {
                            try { muxer?.release() } finally { extractor.release() }
                        }
                    }
                }
            }.getOrNull()
            clipInProgress.set(false)
            mainHandler.post { callback(result != null, result) }
        }.start()
    }

    private fun saveScreenshot(bitmap: Bitmap, callback: (Boolean, String?) -> Unit) {
        Thread {
            val result = runCatching { writeScreenshot(bitmap) }.getOrNull()
            bitmap.recycle()
            screenshotInProgress.set(false)
            mainHandler.post { callback(result != null, result) }
        }.start()
    }

    private fun writeScreenshot(bitmap: Bitmap): String {
        val name = "screenshot_${System.currentTimeMillis()}.jpg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/OpenVideo")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
            ) ?: error("Cannot create screenshot")
            var published = false
            try {
                val output = context.contentResolver.openOutputStream(uri) ?: error("Cannot open screenshot")
                output.use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                check(context.contentResolver.update(uri, values, null, null) > 0)
                published = true
                return uri.toString()
            } finally {
                if (!published) context.contentResolver.delete(uri, null, null)
            }
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OpenVideo")
        dir.mkdirs()
        val file = File(dir, name)
        return MediaExportFile.write(file) {
            FileOutputStream(file).use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
        }
    }

    private fun clipOutputFile(): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "OpenVideo")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "clip_${System.currentTimeMillis()}.mp4")
    }

    private companion object {
        const val CLIP_BUFFER_BYTES = 1024 * 1024
        const val MAX_CLIP_BUFFER_BYTES = 32 * 1024 * 1024
    }
}
