package com.example.openvideo.core.subtitle

import java.io.File

object SubtitleFileCandidateScanner {
    private val subtitleDirectoryNames = listOf("Subs", "Subtitles", "\u5b57\u5e55")
    private const val MAX_CANDIDATES_PER_DIRECTORY = 40

    data class Item(
        val path: String,
        val inSubtitleDirectory: Boolean
    )

    fun candidatesNear(videoPath: String): List<Item> {
        if (videoPath.isBlank() || videoPath.startsWith("content://")) return emptyList()

        val videoFile = File(videoPath)
        if (!videoFile.exists()) return emptyList()

        val parent = videoFile.parentFile ?: return emptyList()
        val videoBaseName = videoFile.nameWithoutExtension
        val sameDirectory = scanDirectory(parent, videoBaseName, inSubtitleDirectory = false)

        val subtitleDirectory = subtitleDirectoryNames.flatMap { name ->
            val directory = parent.resolve(name)
            if (!directory.isDirectory) {
                emptyList()
            } else {
                scanDirectory(directory, videoBaseName, inSubtitleDirectory = true)
            }
        }

        return sameDirectory + subtitleDirectory
    }

    private fun scanDirectory(
        directory: File,
        videoBaseName: String,
        inSubtitleDirectory: Boolean
    ): List<Item> =
        directory.listFiles { file ->
            file.isFile && SubtitleSidecarMatcher.isSupportedSubtitlePath(file.absolutePath)
        }
            ?.sortedWith(
                compareByDescending<File> {
                    it.nameWithoutExtension.startsWith(videoBaseName, ignoreCase = true)
                }.thenBy { it.name.lowercase() }
            )
            ?.take(MAX_CANDIDATES_PER_DIRECTORY)
            ?.map { Item(it.absolutePath, inSubtitleDirectory = inSubtitleDirectory) }
            .orEmpty()
}
