package com.example.openvideo.core.player

internal object PlayerAudioExtensionAvailability {
    fun isFfmpegExtensionAvailable(): Boolean =
        FFMPEG_LIBRARY_CLASS_NAMES.any { className ->
            runCatching {
                val libraryClass = Class.forName(className)
                val isAvailable = runCatching {
                    libraryClass.getMethod("isAvailable").invoke(null) as? Boolean
                }.getOrNull()
                isAvailable ?: true
            }.getOrDefault(false)
        }

    private val FFMPEG_LIBRARY_CLASS_NAMES = arrayOf(
        "androidx.media3.decoder.ffmpeg.FfmpegLibrary",
        "org.jellyfin.media3.ext.ffmpeg.FfmpegLibrary"
    )
}
