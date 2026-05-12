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
                normalized == "audio/vnd.dts.uhd" ||
                normalized == "audio/x-dts" ||
                normalized.contains("dts") ||
                normalized.contains("dca")
        }

    val requiresSoftwareAudioFallback: Boolean
        get() {
            val normalized = mimeType.lowercase(Locale.US)
            return isDtsAudio ||
                normalized == "audio/true-hd" ||
                normalized == "audio/mlp" ||
                normalized.contains("truehd") ||
                normalized.contains("mlp")
        }
}

data class PlayerAudioDiagnostics(
    val ffmpegExtensionAvailable: Boolean = false,
    val lastDecoderName: String? = null,
    val lastInputMimeType: String? = null,
    val lastInputLanguage: String? = null,
    val lastInputChannelCount: Int = 0,
    val lastInputSampleRate: Int = 0,
    val lastInputNeedsSoftwareFallback: Boolean = false,
    val lastPlaybackError: String? = null
) {
    val isUsingFfmpegDecoder: Boolean
        get() = lastDecoderName
            ?.lowercase(Locale.US)
            ?.contains("ffmpeg") == true

    val needsSoftwareAudioFallback: Boolean
        get() = lastInputNeedsSoftwareFallback
}
