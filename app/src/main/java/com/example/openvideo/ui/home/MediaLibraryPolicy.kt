package com.example.openvideo.ui.home

import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.local.VideoFolderGrouper

enum class MediaLibraryEmptyState {
    LOADING,
    NONE,
    NO_MEDIA,
    FILTERED_BY_PRIVACY,
    FILTERED_BY_QUERY_OR_FOLDER
}

data class MediaScanSignature(
    val entries: List<Pair<String, Long>>
) {
    companion object {
        fun fromVideos(videos: List<VideoItem>): MediaScanSignature =
            fromPaths(videos.map { it.path to it.dateAdded })

        fun fromPaths(paths: List<Pair<String, Long>>): MediaScanSignature =
            MediaScanSignature(
                paths
                    .map { (path, dateAdded) -> normalizePath(path) to dateAdded }
                    .sortedBy { it.first }
            )
    }
}

object MediaLibraryPolicy {

    fun visibleVideos(
        videos: List<VideoItem>,
        hiddenFolders: List<String>,
        folderKey: String? = null
    ): List<VideoItem> =
        videos.filter { video ->
            !isHiddenPath(video.path, hiddenFolders) &&
                (folderKey == null || VideoFolderGrouper.folderKey(video.path) == folderKey)
        }

    fun visiblePaths(
        paths: List<String>,
        hiddenFolders: List<String>,
        folderKey: String? = null
    ): List<String> =
        paths.filter { path ->
            !isHiddenPath(path, hiddenFolders) &&
                (folderKey == null || VideoFolderGrouper.folderKey(path) == folderKey)
        }

    fun hiddenFilteredCount(paths: List<String>, hiddenFolders: List<String>): Int =
        paths.count { isHiddenPath(it, hiddenFolders) }

    fun isHiddenPath(path: String, hiddenFolders: List<String>): Boolean {
        val normalizedPath = normalizePath(path)
        if (normalizedPath.isBlank() || normalizedPath.startsWith("content://")) return false
        return hiddenFolders
            .map(::normalizePath)
            .filter { it.isNotBlank() && !it.startsWith("content://") }
            .any { hidden ->
                normalizedPath == hidden || normalizedPath.startsWith("$hidden/")
            }
    }

    fun shouldPublishScan(previous: MediaScanSignature?, next: MediaScanSignature): Boolean =
        previous != next

    fun validFolderKey(selectedFolderKey: String?, folderKeys: List<String>): String? {
        if (selectedFolderKey == null) return null
        return selectedFolderKey.takeIf { it in folderKeys }
    }

    fun shouldExposeStoredFallback(
        path: String,
        hiddenFolders: List<String>,
        localFileExists: (String) -> Boolean
    ): Boolean {
        if (isHiddenPath(path, hiddenFolders)) return false
        if (path.startsWith("content://")) return true

        val candidatePath = when {
            path.startsWith("file://") -> path.removePrefix("file://")
            else -> path
        }
        return candidatePath.isNotBlank() && localFileExists(candidatePath)
    }

    fun emptyState(
        isLoading: Boolean,
        scannedCount: Int,
        visibleCount: Int,
        hiddenFilteredCount: Int = 0
    ): MediaLibraryEmptyState {
        if (isLoading) return MediaLibraryEmptyState.LOADING
        if (visibleCount > 0) return MediaLibraryEmptyState.NONE
        if (scannedCount == 0) return MediaLibraryEmptyState.NO_MEDIA
        return if (hiddenFilteredCount >= scannedCount) {
            MediaLibraryEmptyState.FILTERED_BY_PRIVACY
        } else {
            MediaLibraryEmptyState.FILTERED_BY_QUERY_OR_FOLDER
        }
    }
}

private fun normalizePath(path: String): String =
    path.trim().replace('\\', '/').trimEnd('/')
