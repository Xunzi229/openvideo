package com.example.openvideo.core.metadata

import java.io.File
import java.util.Locale

object LocalArtworkCandidateScanner {
    private val supportedExtensions = setOf("jpg", "jpeg", "png", "webp")

    fun candidatesNear(videoPath: String): List<String> {
        if (videoPath.startsWith("content://", ignoreCase = true)) return emptyList()
        val videoFile = File(videoPath)
        val parent = videoFile.parentFile ?: return emptyList()
        if (!parent.isDirectory) return emptyList()

        return parent.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile }
            .filter { file ->
                file.extension.lowercase(Locale.ROOT) in supportedExtensions
            }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .map { it.path }
            .toList()
    }
}
