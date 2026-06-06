package com.example.openvideo.ui.history

import com.example.openvideo.data.local.HistoryEntity

object HistoryCleanupPolicy {

    fun videoIdsToRemove(
        history: List<HistoryEntity>,
        scannedVideoIds: Set<Long>,
        scannedPaths: Set<String>,
        activeMediaIdentityIds: Set<Long> = emptySet(),
        localFileExists: (String) -> Boolean
    ): List<Long> {
        return history.mapNotNull { entity ->
            val candidatePath = filesystemPath(entity.path)
            val fileExists = candidatePath.isNotBlank() && localFileExists(candidatePath)
            val inScanById = entity.videoId in scannedVideoIds
            val inScanByPath = normalizePath(entity.path) in scannedPaths
            val inScanByIdentity = entity.mediaIdentityId in activeMediaIdentityIds
            if (!fileExists && !inScanById && !inScanByPath && !inScanByIdentity) entity.videoId else null
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
