package com.example.openvideo.ui.player

import com.example.openvideo.core.subtitle.SubtitleSidecarMatcher

object PlayerSubtitleAutoload {
    fun canLoadAsSubtitleUri(uriString: String): Boolean =
        SubtitleSidecarMatcher.isSupportedSubtitlePath(uriString)

    fun rankSidecarCandidates(
        videoBaseName: String,
        candidateFileNames: List<String>
    ): List<String> =
        SubtitleSidecarMatcher.matchSameDirectory(
            videoBaseName = videoBaseName,
            candidatePaths = candidateFileNames
        ).map { it.path }
}
