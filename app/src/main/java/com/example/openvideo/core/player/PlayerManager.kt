package com.example.openvideo.core.player

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.openvideo.core.prefs.AspectRatio
import dagger.hilt.android.qualifiers.ApplicationContext
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

enum class DecodeMode { SOFT, HARD }
enum class RenderMode { SURFACE, TEXTURE }

@Singleton
@OptIn(UnstableApi::class)
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    var player: ExoPlayer? = null
        private set

    private var trackSelector: DefaultTrackSelector? = null
    private var audioDiagnostics = PlayerAudioDiagnostics()

    var decodeMode = DecodeMode.HARD
    var renderMode = RenderMode.SURFACE
    var aspectRatio = AspectRatio.FIT

    fun initialize(mediaUri: Uri? = null): ExoPlayer {
        audioDiagnostics = PlayerAudioDiagnostics(
            ffmpegExtensionAvailable = isFfmpegExtensionAvailable()
        )
        trackSelector = DefaultTrackSelector(context)
        val bufferingProfile = PlayerBufferingPolicy.profileFor(mediaUri?.toString().orEmpty())
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferingProfile.minBufferMs,
                bufferingProfile.maxBufferMs,
                bufferingProfile.bufferForPlaybackMs,
                bufferingProfile.bufferForPlaybackAfterRebufferMs
            )
            .build()
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector!!)
            .setLoadControl(loadControl)
            .build()
            .also { it.addAnalyticsListener(audioDiagnosticsListener()) }

        this.player = player
        return player
    }

    fun release() {
        clearEffects()
        releaseLoudnessEnhancer()
        releaseEqualizer()
        player?.release()
        player = null
        trackSelector = null
    }

    fun setMediaUri(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player?.let {
            it.setMediaItem(mediaItem)
            it.playWhenReady = true
            it.prepare()
        }
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekForward(ms: Long = 10_000) {
        player?.let {
            it.seekTo((it.currentPosition + ms).coerceAtMost(it.duration))
        }
    }

    fun seekBackward(ms: Long = 10_000) {
        player?.let {
            it.seekTo((it.currentPosition - ms).coerceAtLeast(0))
        }
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun setSpeed(speed: Float, pitch: Float = 1.0f) {
        player?.let {
            val current = it.playbackParameters
            if (current.speed == speed && current.pitch == pitch) return
            it.playbackParameters = PlaybackParameters(speed, pitch)
        }
    }

    fun setRepeatMode(repeatMode: Int) {
        player?.repeatMode = repeatMode
    }

    fun setVolumeBoost(enabled: Boolean) {
        player?.volume = 1.0f
        if (enabled) {
            enableLoudnessEnhancer()
        } else {
            releaseLoudnessEnhancer()
        }
    }

    fun setMuted(muted: Boolean) {
        player?.volume = if (muted) 0f else 1f
    }

    fun currentAudioTracks(): List<PlayerAudioTrackInfo> {
        val player = player ?: return emptyList()
        return player.currentTracks.groups
            .mapIndexedNotNull { groupIndex, group ->
                if (group.type != C.TRACK_TYPE_AUDIO) return@mapIndexedNotNull null
                (0 until group.length).map { trackIndex ->
                    val format = group.getTrackFormat(trackIndex)
                    PlayerAudioTrackInfo(
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        mimeType = format.sampleMimeType.orEmpty(),
                        language = format.language,
                        channelCount = format.channelCount,
                        sampleRate = format.sampleRate,
                        bitrate = format.bitrate,
                        selected = group.isTrackSelected(trackIndex),
                        supported = group.isTrackSupported(trackIndex)
                    )
                }
            }
            .flatten()
    }

    fun currentAudioDiagnostics(): PlayerAudioDiagnostics = audioDiagnostics

    fun selectAudioTrack(groupIndex: Int, trackIndex: Int) {
        val player = player ?: return
        val group = player.currentTracks.groups.getOrNull(groupIndex) ?: return
        if (group.type != C.TRACK_TYPE_AUDIO || trackIndex !in 0 until group.length) return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
            .build()
        setMuted(false)
    }

    fun disableAudioTrack() {
        val player = player ?: return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
        setMuted(true)
    }

    private fun audioDiagnosticsListener(): AnalyticsListener =
        object : AnalyticsListener {
            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                audioDiagnostics = audioDiagnostics.copy(
                    lastDecoderName = decoderName,
                    lastPlaybackError = null
                )
            }

            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                audioDiagnostics = audioDiagnostics.copy(
                    lastInputMimeType = format.sampleMimeType,
                    lastInputLanguage = format.language,
                    lastInputChannelCount = format.channelCount,
                    lastInputSampleRate = format.sampleRate,
                    lastInputNeedsSoftwareFallback = format.sampleMimeType.needsSoftwareAudioFallback()
                )
            }

            override fun onPlayerError(
                eventTime: AnalyticsListener.EventTime,
                error: PlaybackException
            ) {
                audioDiagnostics = audioDiagnostics.copy(
                    lastPlaybackError = "${error.errorCodeName}: ${error.message.orEmpty()}".trim()
                )
            }
        }

    private fun isFfmpegExtensionAvailable(): Boolean {
        return FFMPEG_LIBRARY_CLASS_NAMES.any { className ->
            runCatching {
                val libraryClass = Class.forName(className)
                val isAvailable = runCatching {
                    libraryClass.getMethod("isAvailable").invoke(null) as? Boolean
                }.getOrNull()
                isAvailable ?: true
            }.getOrDefault(false)
        }
    }

    private fun String?.needsSoftwareAudioFallback(): Boolean {
        val normalized = this?.lowercase() ?: return false
        return normalized == "audio/vnd.dts" ||
            normalized == "audio/vnd.dts.hd" ||
            normalized == "audio/vnd.dts.uhd" ||
            normalized == "audio/x-dts" ||
            normalized == "audio/true-hd" ||
            normalized == "audio/mlp" ||
            normalized.contains("dts") ||
            normalized.contains("dca") ||
            normalized.contains("truehd") ||
            normalized.contains("mlp")
    }

    fun applyDecodeMode(mode: DecodeMode) {
        decodeMode = mode
        // Software vs hardware decode is device-dependent; ExoPlayer selects decoders automatically.
    }

    fun getAspectRatioValue(): Float {
        return when (aspectRatio) {
            AspectRatio.FIT,
            AspectRatio.FILL,
            AspectRatio.CROP,
            AspectRatio.STRETCH -> 0f
            AspectRatio.RATIO_4_3 -> 4f / 3f
            AspectRatio.RATIO_16_9 -> 16f / 9f
        }
    }

    val isPlaying: Boolean get() = player?.isPlaying == true
    val currentPosition: Long get() = player?.currentPosition ?: 0
    val duration: Long get() = player?.duration ?: 0
    val playbackState: Int get() = player?.playbackState ?: Player.STATE_IDLE

    fun addListener(listener: Player.Listener) {
        player?.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        player?.removeListener(listener)
    }

    // P1: Video Filters
    private val activeEffects = mutableListOf<androidx.media3.common.Effect>()
    private var lastVideoAdjustments: VideoAdjustments? = null

    fun setBrightness(value: Float) {
        lastVideoAdjustments = null
        activeEffects.removeAll { it is Brightness }
        activeEffects.add(Brightness(value))
        applyEffects()
    }

    fun setContrast(value: Float) {
        lastVideoAdjustments = null
        activeEffects.removeAll { it is Contrast }
        activeEffects.add(Contrast(value))
        applyEffects()
    }

    fun applyVideoAdjustments(
        brightness: Float,
        contrast: Float,
        saturation: Float
    ) {
        val nextAdjustments = VideoAdjustments(brightness, contrast, saturation)
        if (lastVideoAdjustments == nextAdjustments) return
        lastVideoAdjustments = nextAdjustments
        activeEffects.removeAll { it is Brightness || it is Contrast || it is RgbMatrix }
        if (brightness != 0f) {
            activeEffects.add(Brightness(brightness))
        }
        if (contrast != 0f) {
            activeEffects.add(Contrast(contrast))
        }
        if (saturation != 0f) {
            activeEffects.add(saturationMatrix(saturation))
        }
        applyEffects()
    }

    private fun saturationMatrix(value: Float): RgbMatrix {
        val scale = (1f + value).coerceAtLeast(0f)
        val inverse = 1f - scale
        val red = 0.213f * inverse
        val green = 0.715f * inverse
        val blue = 0.072f * inverse
        val matrix = floatArrayOf(
            red + scale, green, blue, 0f,
            red, green + scale, blue, 0f,
            red, green, blue + scale, 0f,
            0f, 0f, 0f, 1f
        )
        return object : RgbMatrix {
            override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix
        }
    }

    fun setRotation(degrees: Float) {
        lastVideoAdjustments = null
        activeEffects.removeAll { it is ScaleAndRotateTransformation }
        activeEffects.add(ScaleAndRotateTransformation.Builder().setRotationDegrees(degrees).build())
        applyEffects()
    }

    fun clearEffects() {
        lastVideoAdjustments = null
        activeEffects.clear()
        player?.setVideoEffects(emptyList())
    }

    private fun applyEffects() {
        player?.setVideoEffects(activeEffects.toList())
    }

    private data class VideoAdjustments(
        val brightness: Float,
        val contrast: Float,
        val saturation: Float
    )

    // P1: Equalizer
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    fun initEqualizer(audioSessionId: Int) {
        releaseEqualizer()
        try {
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true
        } catch (e: Exception) {
            equalizer = null
        }
    }

    fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }

    fun setEqualizerPreset(preset: Short) {
        equalizer?.usePreset(preset)
    }

    fun getEqualizerPresets(): List<String> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
    }

    fun setEqualizerBand(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
    }

    private fun enableLoudnessEnhancer() {
        val audioSessionId = player?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

        releaseLoudnessEnhancer()
        loudnessEnhancer = runCatching {
            LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(VOLUME_BOOST_MILLIBELS)
                enabled = true
            }
        }.getOrNull()
    }

    private fun releaseLoudnessEnhancer() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }

    // P1: Screenshot
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
        const val VOLUME_BOOST_MILLIBELS = 600
        const val CLIP_BUFFER_BYTES = 1024 * 1024
        val FFMPEG_LIBRARY_CLASS_NAMES = arrayOf(
            "androidx.media3.decoder.ffmpeg.FfmpegLibrary",
            "org.jellyfin.media3.ext.ffmpeg.FfmpegLibrary"
        )
    }
}
