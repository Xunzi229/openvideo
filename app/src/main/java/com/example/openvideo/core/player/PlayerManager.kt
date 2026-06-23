package com.example.openvideo.core.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.openvideo.core.prefs.AspectRatio
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class DecodeMode { SOFT, HARD }
enum class RenderMode { SURFACE, TEXTURE }

@Singleton
@OptIn(UnstableApi::class)
class PlayerManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    var player: ExoPlayer? = null
        private set

    private var trackSelector: DefaultTrackSelector? = null
    private var audioDiagnostics = PlayerAudioDiagnostics()
    private val videoEffects = PlayerVideoEffectsController { player }
    private val audioEffects = PlayerAudioEffectsController { player }
    private val audioTracks = PlayerAudioTrackController(
        playerProvider = { player },
        onMuteChanged = ::setMuted
    )
    private val mediaExport = PlayerMediaExportController(context)

    var decodeMode = DecodeMode.HARD
    var renderMode = RenderMode.SURFACE
    var aspectRatio = AspectRatio.FIT

    fun initialize(mediaUri: Uri? = null): ExoPlayer {
        // Singleton：Activity / PlaybackService 共用；再次 initialize 必须先 release，否则会 orphan 旧 ExoPlayer 继续出声。
        release()
        audioDiagnostics = PlayerAudioDiagnostics(
            ffmpegExtensionAvailable = PlayerAudioExtensionAvailability.isFfmpegExtensionAvailable()
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
            .setMediaCodecSelector(codecSelectorFor(decodeMode))

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
        videoEffects.clearEffects()
        audioEffects.releaseAll()
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
        audioEffects.setVolumeBoost(enabled)
    }

    fun setMuted(muted: Boolean) {
        player?.volume = if (muted) 0f else 1f
    }

    fun currentAudioTracks(): List<PlayerAudioTrackInfo> {
        return audioTracks.currentAudioTracks()
    }

    fun currentAudioDiagnostics(): PlayerAudioDiagnostics = audioDiagnostics

    fun selectAudioTrack(groupIndex: Int, trackIndex: Int) {
        audioTracks.selectAudioTrack(groupIndex, trackIndex)
    }

    fun disableAudioTrack() {
        audioTracks.disableAudioTrack()
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
    }

    private fun codecSelectorFor(mode: DecodeMode): MediaCodecSelector =
        when (mode) {
            DecodeMode.SOFT -> MediaCodecSelector.PREFER_SOFTWARE
            DecodeMode.HARD -> MediaCodecSelector.DEFAULT
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
    fun setBrightness(value: Float) {
        videoEffects.setBrightness(value)
    }

    fun setContrast(value: Float) {
        videoEffects.setContrast(value)
    }

    fun applyVideoAdjustments(
        brightness: Float,
        contrast: Float,
        saturation: Float
    ) {
        videoEffects.applyVideoAdjustments(brightness, contrast, saturation)
    }

    fun setRotation(degrees: Float) {
        videoEffects.setRotation(degrees)
    }

    fun clearEffects() {
        videoEffects.clearEffects()
    }

    // P1: Equalizer
    fun initEqualizer(audioSessionId: Int) {
        audioEffects.initEqualizer(audioSessionId)
    }

    fun releaseEqualizer() {
        audioEffects.releaseEqualizer()
    }

    fun setEqualizerPreset(preset: Short) {
        audioEffects.setEqualizerPreset(preset)
    }

    fun getEqualizerPresets(): List<String> {
        return audioEffects.getEqualizerPresets()
    }

    fun setEqualizerBand(band: Short, level: Short) {
        audioEffects.setEqualizerBand(band, level)
    }

    // P1: Screenshot
    fun takeScreenshot(videoView: android.view.View, callback: (Boolean, String?) -> Unit) {
        mediaExport.takeScreenshot(videoView, callback)
    }

    fun exportClip(sourceUri: Uri, startMs: Long, endMs: Long, callback: (Boolean, String?) -> Unit) {
        mediaExport.exportClip(sourceUri, startMs, endMs, callback)
    }

}
