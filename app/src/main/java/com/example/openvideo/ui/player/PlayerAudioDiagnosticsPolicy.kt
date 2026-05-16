package com.example.openvideo.ui.player

import com.example.openvideo.core.player.PlayerAudioDiagnostics
import com.example.openvideo.core.player.PlayerAudioTrackInfo
import java.util.Locale
import kotlin.math.absoluteValue

object PlayerAudioDiagnosticsPolicy {

    fun trackSummary(
        track: PlayerAudioTrackInfo,
        streamLabel: String,
        unsupportedLabel: String
    ): String = buildList {
        add(streamLabel)
        add(codecLabel(track.mimeType))
        track.language.languageLabel()?.let(::add)
        channelLabel(track.channelCount)?.let(::add)
        sampleRateLabel(track.sampleRate)?.let(::add)
        bitrateLabel(track.bitrate)?.let(::add)
        if (!track.supported) add(unsupportedLabel)
    }.joinToString(" / ")

    fun quickTrackSummary(
        track: PlayerAudioTrackInfo,
        streamLabel: String
    ): String = buildList {
        add(streamLabel)
        add(codecLabel(track.mimeType))
        track.language.languageLabel()?.let(::add)
        channelLabel(track.channelCount)?.let(::add)
        sampleRateLabel(track.sampleRate)?.let(::add)
        bitrateLabel(track.bitrate)?.let(::add)
    }.joinToString(" / ")

    fun runtimeInputSummary(
        diagnostics: PlayerAudioDiagnostics,
        softwareFallbackLabel: String
    ): String? = buildList {
        diagnostics.lastInputMimeType?.takeIf { it.isNotBlank() }?.let { add(codecLabel(it)) }
        diagnostics.lastInputLanguage.languageLabel()?.let(::add)
        channelLabel(diagnostics.lastInputChannelCount)?.let(::add)
        sampleRateLabel(diagnostics.lastInputSampleRate)?.let(::add)
        if (diagnostics.lastInputNeedsSoftwareFallback) add(softwareFallbackLabel)
    }.takeIf { it.isNotEmpty() }?.joinToString(" / ")

    fun compatibilityMessage(
        diagnostics: PlayerAudioDiagnostics,
        fallbackActiveLabel: String,
        fallbackNeededLabel: String
    ): String? {
        if (!diagnostics.needsSoftwareAudioFallback) return null
        return if (diagnostics.isUsingFfmpegDecoder) fallbackActiveLabel else fallbackNeededLabel
    }

    fun codecLabel(mimeType: String): String = when (mimeType.lowercase(Locale.US)) {
        "audio/mp4a-latm" -> "AAC"
        "audio/ac3" -> "AC-3"
        "audio/eac3" -> "E-AC-3"
        "audio/vnd.dts" -> "DTS/DCA"
        "audio/vnd.dts.hd" -> "DTS-HD"
        "audio/vnd.dts.uhd" -> "DTS-UHD"
        "audio/x-dts" -> "DTS/DCA"
        "audio/true-hd" -> "Dolby TrueHD"
        "audio/mlp" -> "MLP"
        else -> mimeType.ifBlank { "Audio" }
    }

    fun channelLabel(count: Int): String? = when {
        count <= 0 -> null
        count == 1 -> "Mono"
        count == 2 -> "Stereo"
        count == 6 -> "5.1"
        count == 8 -> "7.1"
        else -> "$count ch"
    }

    fun sampleRateLabel(sampleRate: Int): String? =
        sampleRate.takeIf { it > 0 }?.let { "$it Hz" }

    fun bitrateLabel(bitsPerSecond: Int): String? {
        if (bitsPerSecond <= 0) return null
        val abs = bitsPerSecond.absoluteValue
        return if (abs >= 1_000_000) {
            String.format(Locale.US, "%.1f Mbps", bitsPerSecond / 1_000_000f)
        } else {
            "${bitsPerSecond / 1000} kbps"
        }
    }

    private fun String?.languageLabel(): String? =
        this?.takeIf { it.isNotBlank() && it != "und" }
}
