package com.example.openvideo.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.openvideo.R
import com.example.openvideo.core.player.PlayerAudioDiagnostics
import com.example.openvideo.core.player.PlayerAudioTrackInfo
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.PlayerPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.round

class PlayerSettingsInfoController(
    private val context: Context,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val scope: CoroutineScope,
    private val isInfoPageVisible: () -> Boolean,
    private val onInfoChanged: () -> Unit,
    private val formatTime: (Long) -> String,
    private val playbackSpeedLabelFor: (Float) -> String,
    private val aspectLabel: (AspectRatio) -> String
) {
    private var cachedMediaInfo: PlayerMediaInfo? = null
    private var cachedMediaInfoSource: String? = null
    private var isMediaInfoLoading = false

    fun loadMediaInfoAsync() {
        val source = viewModel.currentVideoSource()
        if (source.isBlank()) return
        if (cachedMediaInfoSource == source && (cachedMediaInfo != null || isMediaInfoLoading)) return

        isMediaInfoLoading = true
        scope.launch {
            val mediaInfo = withContext(Dispatchers.IO) {
                PlayerMediaInfoReader.read(context, viewModel.currentVideoSource())
            }
            cachedMediaInfo = mediaInfo
            cachedMediaInfoSource = source
            isMediaInfoLoading = false
            if (isInfoPageVisible()) onInfoChanged()
        }
    }

    fun videoInfoRows(): List<Pair<String, String>> {
        val state = viewModel.uiState.value
        val mediaInfo = cachedMediaInfo
        val rows = mutableListOf(
            context.getString(R.string.player_settings_info_title) to
                state.title.ifBlank { context.getString(R.string.app_name) },
            context.getString(R.string.player_settings_info_position) to formatTime(playerManager.currentPosition),
            context.getString(R.string.player_settings_info_resolution) to videoResolutionLabel(mediaInfo),
            context.getString(R.string.player_settings_info_speed) to playbackSpeedLabelFor(state.speed),
            context.getString(R.string.player_settings_info_aspect) to aspectLabel(playerPrefs.aspectRatio),
            context.getString(R.string.player_settings_info_source) to
                viewModel.currentVideoSource().ifBlank { context.getString(R.string.player_settings_value_none) }
        )
        mediaInfo
            ?.mediaInfoRows(context, formatTime)
            ?.let { rows += it }
        if (mediaInfo == null && isMediaInfoLoading) {
            rows += context.getString(R.string.player_settings_info_container) to
                context.getString(R.string.player_settings_info_loading)
        }
        viewModel.selectedAudioTrack()?.let { track ->
            rows += context.getString(R.string.player_settings_info_current_audio_track) to
                PlayerAudioDiagnosticsPolicy.trackSummary(
                    track = track,
                    streamLabel = context.getString(R.string.player_settings_info_stream, track.groupIndex + 1),
                    unsupportedLabel = context.getString(R.string.player_settings_audio_track_unsupported)
                )
            rows += context.getString(R.string.player_settings_info_audio_decoder) to audioDecoderLabel(track)
        } ?: run {
            rows += context.getString(R.string.player_settings_info_current_audio_track) to
                context.getString(
                    if (playerPrefs.audioMuted) R.string.player_sheet_disable
                    else R.string.player_settings_audio_track_none
                )
        }
        rows += audioDiagnosticRows(viewModel.audioDiagnostics())
        if (rows.none { it.first == context.getString(R.string.player_settings_info_duration) }) {
            rows += context.getString(R.string.player_settings_info_duration) to formatTime(playerManager.duration)
        }
        return rows
    }

    fun audioTrackLabel(track: PlayerAudioTrackInfo): String =
        PlayerAudioDiagnosticsPolicy.trackSummary(
            track = track,
            streamLabel = context.getString(R.string.player_settings_info_stream, track.groupIndex + 1),
            unsupportedLabel = context.getString(R.string.player_settings_audio_track_unsupported)
        )

    fun copyVideoInfoToClipboard() {
        val text = videoInfoRows().joinToString(separator = "\n") { "${it.first}: ${it.second}" }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("openvideo_video_info", text))
        Toast.makeText(context, context.getString(R.string.player_settings_info_copied), Toast.LENGTH_SHORT).show()
    }

    private fun audioDiagnosticRows(diagnostics: PlayerAudioDiagnostics): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        rows += context.getString(R.string.player_settings_info_ffmpeg_extension) to context.getString(
            if (diagnostics.ffmpegExtensionAvailable) {
                R.string.player_settings_info_ffmpeg_available
            } else {
                R.string.player_settings_info_ffmpeg_unavailable
            }
        )
        diagnostics.lastDecoderName?.takeIf { it.isNotBlank() }?.let { decoder ->
            rows += context.getString(R.string.player_settings_info_audio_decoder) to decoder
        }
        PlayerAudioDiagnosticsPolicy.runtimeInputSummary(
            diagnostics = diagnostics,
            softwareFallbackLabel = context.getString(R.string.player_settings_info_audio_fallback_needed)
        )?.let { label ->
            rows += context.getString(R.string.player_settings_info_audio_input_format) to label
        }
        PlayerAudioDiagnosticsPolicy.compatibilityMessage(
            diagnostics = diagnostics,
            fallbackActiveLabel = context.getString(R.string.player_settings_info_audio_fallback_active),
            fallbackNeededLabel = context.getString(R.string.player_settings_info_audio_fallback_needed)
        )?.let { message ->
            rows += context.getString(R.string.player_settings_info_audio_compatibility) to message
        }
        diagnostics.lastPlaybackError?.takeIf { it.isNotBlank() }?.let { error ->
            rows += context.getString(R.string.player_settings_info_playback_error) to error
        }
        return rows
    }

    private fun audioDecoderLabel(track: PlayerAudioTrackInfo): String = when {
        !track.supported -> context.getString(R.string.player_settings_audio_decoder_unsupported)
        track.requiresSoftwareAudioFallback -> context.getString(R.string.player_settings_audio_decoder_ffmpeg)
        else -> context.getString(R.string.player_settings_audio_decoder_system)
    }

    private fun audioCodecLabel(mimeType: String): String = when (mimeType.lowercase(Locale.US)) {
        "audio/mp4a-latm" -> "AAC"
        "audio/ac3" -> "AC-3"
        "audio/eac3" -> "E-AC-3"
        "audio/vnd.dts" -> "DTS/DCA"
        "audio/vnd.dts.hd" -> "DTS-HD"
        "audio/vnd.dts.uhd" -> "DTS-UHD"
        "audio/x-dts" -> "DTS/DCA"
        "audio/true-hd" -> "Dolby TrueHD"
        "audio/mlp" -> "MLP"
        else -> mimeType.ifBlank { context.getString(R.string.player_settings_info_type_audio) }
    }

    private fun audioChannelLabel(count: Int): String = when (count) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> "$count ch"
    }

    @OptIn(UnstableApi::class)
    private fun videoResolutionLabel(mediaInfo: PlayerMediaInfo?): String {
        val vs = viewModel.player?.videoSize
        if (vs != null) {
            val h = vs.height
            val w = vs.width
            if (w > 0 && h > 0) {
                val displayW = round(w * vs.pixelWidthHeightRatio.toDouble()).toInt().coerceAtLeast(1)
                return "${displayW}x$h"
            }
        }
        mediaInfo?.tracks
            ?.firstOrNull { track ->
                track.type == PlayerMediaTrack.Type.VIDEO &&
                    track.width != null &&
                    track.height != null
            }
            ?.let { track -> return "${track.width}x${track.height}" }
        return context.getString(R.string.player_settings_value_none)
    }

    @Suppress("unused")
    private fun legacyAudioSummaryParts(track: PlayerAudioTrackInfo): String {
        val parts = mutableListOf<String>()
        parts += context.getString(R.string.player_settings_info_stream, track.groupIndex + 1)
        parts += audioCodecLabel(track.mimeType)
        track.language?.takeIf { it.isNotBlank() && it != "und" }?.let { parts += it }
        if (track.channelCount > 0) parts += audioChannelLabel(track.channelCount)
        if (track.sampleRate > 0) parts += "${track.sampleRate} Hz"
        if (!track.supported) parts += context.getString(R.string.player_settings_audio_track_unsupported)
        return parts.joinToString(" 路 ")
    }
}
