package com.example.openvideo.core.player

import java.util.Locale

data class PlayerAudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val mimeType: String,
    val language: String?,
    val channelCount: Int,
    val sampleRate: Int,
    val bitrate: Int,
    val selected: Boolean,
    val supported: Boolean
) {
    val isDtsAudio: Boolean
        get() {
            val normalized = mimeType.lowercase(Locale.US)
            return normalized == "audio/vnd.dts" ||
                normalized == "audio/vnd.dts.hd" ||
                normalized.contains("dts") ||
                normalized.contains("dca")
    }
}

data class PlayerAudioDiagnostics(
    val ffmpegExtensionAvailable: Boolean = false,
    val lastDecoderName: String? = null,
    val lastInputMimeType: String? = null,
    val lastInputLanguage: String? = null,
    val lastInputChannelCount: Int = 0,
    val lastInputSampleRate: Int = 0,
    val lastPlaybackError: String? = null
) {
    val isUsingFfmpegDecoder: Boolean
        get() = lastDecoderName
            ?.lowercase(Locale.US)
            ?.contains("ffmpeg") == true
}
