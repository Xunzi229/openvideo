package com.example.openvideo.ui.history

import com.example.openvideo.data.local.HistoryEntity

object HistoryCleanupPolicy {

    fun videoIdsToRemove(
        history: List<HistoryEntity>,
        scannedVideoIds: Set<Long>,
        scannedPaths: Set<String>,
        localFileExists: (String) -> Boolean
    ): List<Long> {
        return history.mapNotNull { entity ->
            val candidatePath = filesystemPath(entity.path)
            val fileExists = candidatePath.isNotBlank() && localFileExists(candidatePath)
            val inScanById = entity.videoId in scannedVideoIds
            val inScanByPath = normalizePath(entity.path) in scannedPaths
            if (!fileExists && !inScanById && !inScanByPath) entity.videoId else null
        }
    }

    private fun filesystemPath(path: String): String = when {
        path.startsWith("file://") -> path.removePrefix("file://")
        path.startsWith("content://") -> ""
        else -> path
    }

    private fun normalizePath(path: String): String =
        path.trim().replace('\\', '/').trimEnd('/')
}
