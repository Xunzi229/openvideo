package com.example.openvideo.ui.local

import com.example.openvideo.data.model.VideoItem

data class VideoFolderSummary(
    val key: String,
    val name: String,
    val videoCount: Int,
    val isPinned: Boolean = false
)

data class VideoFolder(
    val key: String,
    val name: String,
    val videos: List<VideoItem>,
    val isPinned: Boolean = false
) {
    val videoCount: Int get() = videos.size
}

object VideoFolderGrouper {
    const val UNKNOWN_FOLDER_KEY = "__unknown__"
    const val UNKNOWN_FOLDER_NAME = "未知目录"

    fun groupPaths(paths: List<String>): List<VideoFolderSummary> {
        return paths
            .groupBy { folderKey(it) }
            .map { (key, items) ->
                VideoFolderSummary(
                    key = key,
                    name = folderName(key),
                    videoCount = items.size
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun groupVideos(videos: List<VideoItem>): List<VideoFolder> {
        return videos
            .groupBy { folderKey(it.path) }
            .map { (key, items) ->
                VideoFolder(
                    key = key,
                    name = folderName(key),
                    videos = items.sortedByDescending { it.dateAdded }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun folderKey(path: String): String {
        if (path.isBlank() || path.startsWith("content://")) return UNKNOWN_FOLDER_KEY
        val normalized = path.replace('\\', '/').trimEnd('/')
        val separatorIndex = normalized.lastIndexOf('/')
        if (separatorIndex <= 0) return UNKNOWN_FOLDER_KEY
        return normalized.substring(0, separatorIndex)
    }

    fun folderName(key: String): String {
        if (key == UNKNOWN_FOLDER_KEY) return UNKNOWN_FOLDER_NAME
        return key.substringAfterLast('/').takeIf { it.isNotBlank() } ?: UNKNOWN_FOLDER_NAME
    }
}
