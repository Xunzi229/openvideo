package com.example.openvideo.ui.player

sealed interface PlayerSubtitleLoadRequest {
    data class SidecarFile(val videoPath: String) : PlayerSubtitleLoadRequest
    data class SubtitleUri(val uriString: String) : PlayerSubtitleLoadRequest
    data object None : PlayerSubtitleLoadRequest
}

object PlayerSubtitleLoadPolicy {
    fun resolve(uriString: String, videoPath: String): PlayerSubtitleLoadRequest {
        val scheme = uriString.substringBefore(':', "").lowercase()
        val sidecarPath = when {
            scheme == "file" -> uriString.removePrefix("file://")
            videoPath.isNotBlank() && !videoPath.startsWith("content://") -> videoPath
            else -> ""
        }

        if (sidecarPath.isNotBlank()) {
            return PlayerSubtitleLoadRequest.SidecarFile(videoPath = sidecarPath)
        }

        if (PlayerSubtitleAutoload.canLoadAsSubtitleUri(uriString)) {
            return PlayerSubtitleLoadRequest.SubtitleUri(uriString = uriString)
        }

        return PlayerSubtitleLoadRequest.None
    }
}
