package com.example.openvideo.core.metadata

import com.example.openvideo.core.mediaid.MediaPathNormalizer
import java.util.Locale

object LocalArtworkFinder {
    private val supportedExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val namedArtworkPriority = listOf("poster", "folder", "cover")

    fun find(videoPath: String, candidatePaths: List<String>): String? {
        val video = MediaPathNormalizer.normalize(videoPath) ?: return null
        val videoBaseName = video.fileName.substringBeforeLast('.', missingDelimiterValue = video.fileName)
            .lowercase(Locale.ROOT)

        val candidates = candidatePaths.mapNotNull { path ->
            val normalized = MediaPathNormalizer.normalize(path) ?: return@mapNotNull null
            if (normalized.parentKey != video.parentKey) return@mapNotNull null

            val fileName = normalized.fileName
            val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
                .lowercase(Locale.ROOT)
            if (extension !in supportedExtensions) return@mapNotNull null

            val baseName = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
                .lowercase(Locale.ROOT)
            ArtworkCandidate(
                originalPath = normalized.original,
                baseName = baseName
            )
        }

        namedArtworkPriority.forEach { preferredName ->
            candidates.firstOrNull { it.baseName == preferredName }?.let { return it.originalPath }
        }

        return candidates.firstOrNull { it.baseName == videoBaseName }?.originalPath
    }

    private data class ArtworkCandidate(
        val originalPath: String,
        val baseName: String
    )
}
