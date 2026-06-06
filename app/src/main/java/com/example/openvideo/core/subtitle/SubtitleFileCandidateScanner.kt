package com.example.openvideo.core.subtitle

import java.io.File

object SubtitleFileCandidateScanner {
    private val subtitleDirectoryNames = listOf("Subs", "Subtitles", "\u5b57\u5e55")

    data class Item(
        val path: String,
        val inSubtitleDirectory: Boolean
    )

    fun candidatesNear(videoPath: String): List<Item> {
        if (videoPath.isBlank() || videoPath.startsWith("content://")) return emptyList()

        val videoFile = File(videoPath)
        if (!videoFile.exists()) return emptyList()

        val parent = videoFile.parentFile ?: return emptyList()
        val sameDirectory = parent.listFiles()
            ?.filter { it.isFile && SubtitleSidecarMatcher.isSupportedSubtitlePath(it.absolutePath) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { Item(it.absolutePath, inSubtitleDirectory = false) }
            .orEmpty()

        val subtitleDirectory = subtitleDirectoryNames.flatMap { name ->
            val directory = parent.resolve(name)
            if (!directory.isDirectory) {
                emptyList()
            } else {
                directory.listFiles()
                    ?.filter { it.isFile && SubtitleSidecarMatcher.isSupportedSubtitlePath(it.absolutePath) }
                    ?.sortedBy { it.name.lowercase() }
                    ?.map { Item(it.absolutePath, inSubtitleDirectory = true) }
                    .orEmpty()
            }
        }

        return sameDirectory + subtitleDirectory
    }
}
