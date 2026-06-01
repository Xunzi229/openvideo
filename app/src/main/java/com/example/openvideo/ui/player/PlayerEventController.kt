package com.example.openvideo.ui.player

import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.example.openvideo.core.diagnostics.CrashLogger
import com.example.openvideo.core.prefs.PlayerPrefs

class PlayerEventController(
    private val activity: AppCompatActivity,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val startupTrace: PlayerStartupTrace,
    private val playbackWasBufferingProvider: () -> Boolean,
    private val onPlaybackWasBufferingChanged: (Boolean) -> Unit,
    private val onUpdatePlayPauseIcon: (isPlaying: Boolean, playWhenReady: Boolean) -> Unit,
    private val onStartPlaybackServiceIfNeeded: (Boolean) -> Unit,
    private val onUnlockPlayerForPause: () -> Unit,
    private val onApplyPlaybackTickSeek: (positionMs: Long, durationMs: Long) -> Unit,
    private val onHideFirstFrameForAudioOnly: () -> Unit,
    private val onHideErrorHud: () -> Unit,
    private val onPrepareReady: () -> Unit,
    private val onPlaybackEnded: () -> Unit,
    private val onSetVolumeBoost: (Boolean) -> Unit,
    private val onFirstFrameRendered: () -> Unit,
    private val onShowPlayerError: (PlaybackException) -> Unit,
    private val onApplyVideoOrientation: (width: Int, height: Int, pixelWidthHeightRatio: Float, unappliedRotationDegrees: Int) -> Unit,
    private val onApplyContentAspectRatio: (width: Int, height: Int, pixelWidthHeightRatio: Float, unappliedRotationDegrees: Int) -> Unit,
    private val onApplyContentFrameTransform: (width: Int, height: Int, pixelWidthHeightRatio: Float, unappliedRotationDegrees: Int) -> Unit
) {
    private var playerListener: Player.Listener? = null
    private var startupAnalyticsListener: AnalyticsListener? = null

    fun attach() {
        playerListener?.let { viewModel.player?.removeListener(it) }
        val listener = createPlayerListener()
        playerListener = listener
        viewModel.player?.addListener(listener)
        attachStartupAnalyticsListener()
    }

    fun detach() {
        playerListener?.let { viewModel.player?.removeListener(it) }
        playerListener = null
        startupAnalyticsListener?.let { viewModel.player?.removeAnalyticsListener(it) }
        startupAnalyticsListener = null
    }

    private fun createPlayerListener(): Player.Listener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val playWhenReady = viewModel.player?.playWhenReady == true
                onUpdatePlayPauseIcon(isPlaying, playWhenReady)
                onStartPlaybackServiceIfNeeded(isPlaying)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) onUnlockPlayerForPause()
                onUpdatePlayPauseIcon(
                    viewModel.player?.isPlaying == true,
                    playWhenReady
                )
                onStartPlaybackServiceIfNeeded(playWhenReady && viewModel.player?.isPlaying == true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val readyTraceUpdate = PlayerPlaybackReadyTracePolicy.onPlaybackStateChanged(
                    playbackState = playbackState,
                    wasBuffering = playbackWasBufferingProvider(),
                    hasRecordedPrepareReady = startupTrace.hasRecorded(PlayerStartupTrace.Events.PREPARE_READY)
                )
                onPlaybackWasBufferingChanged(readyTraceUpdate.nextWasBuffering)
                when (readyTraceUpdate.readyTraceEvent) {
                    PlayerPlaybackReadyTracePolicy.ReadyTraceEvent.RECOVERED_AFTER_BUFFERING ->
                        startupTrace.record(PlayerStartupTrace.Events.READY_AFTER_BUFFERING)
                    PlayerPlaybackReadyTracePolicy.ReadyTraceEvent.FIRST_PREPARE_READY,
                    null -> Unit
                }

                if (playbackState == Player.STATE_READY) {
                    val state = viewModel.uiState.value
                    onApplyPlaybackTickSeek(state.currentPosition, state.duration)
                    onHideFirstFrameForAudioOnly()
                    onHideErrorHud()
                    onPrepareReady()
                } else if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded()
                }
            }

            @OptIn(UnstableApi::class)
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (PlayerVolumeBoostApplyPolicy.shouldReapplyOnAudioSessionChange(playerPrefs.volumeBoost)) {
                    onSetVolumeBoost(true)
                }
            }

            override fun onRenderedFirstFrame() {
                onFirstFrameRendered()
            }

            override fun onPlayerError(error: PlaybackException) {
                val diagnostics = PlayerErrorDiagnostics.build(
                    context = activity,
                    video = viewModel.currentVideoItemForDiagnostics(),
                    player = viewModel.player
                )
                CrashLogger.logPlayerError(activity, error, diagnostics)
                onShowPlayerError(error)
            }

            @Suppress("DEPRECATION")
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                onApplyVideoOrientation(
                    videoSize.width,
                    videoSize.height,
                    videoSize.pixelWidthHeightRatio,
                    videoSize.unappliedRotationDegrees
                )
                onApplyContentAspectRatio(
                    videoSize.width,
                    videoSize.height,
                    videoSize.pixelWidthHeightRatio,
                    videoSize.unappliedRotationDegrees
                )
                onApplyContentFrameTransform(
                    videoSize.width,
                    videoSize.height,
                    videoSize.pixelWidthHeightRatio,
                    videoSize.unappliedRotationDegrees
                )
            }
        }

    @OptIn(UnstableApi::class)
    private fun attachStartupAnalyticsListener() {
        if (startupAnalyticsListener != null) return
        val listener = object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                PlayerDecoderEventPolicy.videoDecoderEvents(decoderName)
                    .forEach { startupTrace.recordOnce(it) }
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                PlayerDecoderEventPolicy.audioDecoderEvents(decoderName)
                    .forEach { startupTrace.recordOnce(it) }
            }

            override fun onVideoCodecError(
                eventTime: AnalyticsListener.EventTime,
                videoCodecError: Exception
            ) {
                PlayerDecoderEventPolicy.videoCodecErrorEvents(videoCodecError.javaClass.name)
                    .forEach { startupTrace.recordOnce(it) }
            }

            override fun onAudioCodecError(
                eventTime: AnalyticsListener.EventTime,
                audioCodecError: Exception
            ) {
                PlayerDecoderEventPolicy.audioCodecErrorEvents(audioCodecError.javaClass.name)
                    .forEach { startupTrace.recordOnce(it) }
            }
        }
        viewModel.player?.addAnalyticsListener(listener)
        startupAnalyticsListener = listener
    }
}
