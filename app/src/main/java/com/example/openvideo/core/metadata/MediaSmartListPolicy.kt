package com.example.openvideo.core.metadata

enum class MediaSmartListType {
    RECENTLY_ADDED,
    IN_PROGRESS,
    COMPLETED,
    LARGE_FILES,
    UHD,
    HDR,
    WITH_SUBTITLES
}

data class MediaSmartListItem(
    val id: Long,
    val title: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val lastPositionMs: Long? = null,
    val hasExternalSubtitle: Boolean = false,
    val isHdr: Boolean = false
)

object MediaSmartListPolicy {
    private const val DEFAULT_LIMIT = 20
    private const val DEFAULT_LARGE_FILE_THRESHOLD_BYTES = 1_073_741_824L
    private const val COMPLETED_WINDOW_MS = 10_000L

    fun itemsFor(
        type: MediaSmartListType,
        items: List<MediaSmartListItem>,
        limit: Int = DEFAULT_LIMIT,
        largeFileThresholdBytes: Long = DEFAULT_LARGE_FILE_THRESHOLD_BYTES
    ): List<MediaSmartListItem> {
        val sorted = when (type) {
            MediaSmartListType.RECENTLY_ADDED -> items.sortedByDescending { it.dateAdded }
            MediaSmartListType.IN_PROGRESS -> items
                .filter { it.isInProgress() }
                .sortedByDescending { it.lastPositionMs ?: 0L }
            MediaSmartListType.COMPLETED -> items
                .filter { it.isCompleted() }
                .sortedByDescending { it.dateAdded }
            MediaSmartListType.LARGE_FILES -> items
                .filter { it.sizeBytes >= largeFileThresholdBytes }
                .sortedByDescending { it.sizeBytes }
            MediaSmartListType.UHD -> items
                .filter { it.width >= 3840 || it.height >= 2160 }
                .sortedByDescending { it.dateAdded }
            MediaSmartListType.HDR -> items
                .filter { it.isHdr }
                .sortedByDescending { it.dateAdded }
            MediaSmartListType.WITH_SUBTITLES -> items
                .filter { it.hasExternalSubtitle }
                .sortedByDescending { it.dateAdded }
        }
        return sorted.take(limit.coerceAtLeast(0))
    }

    private fun MediaSmartListItem.isInProgress(): Boolean {
        val position = lastPositionMs?.takeIf { it > 0L } ?: return false
        if (durationMs <= 0L) return false
        return position < (durationMs - COMPLETED_WINDOW_MS).coerceAtLeast(0L)
    }

    private fun MediaSmartListItem.isCompleted(): Boolean {
        val position = lastPositionMs?.takeIf { it > 0L } ?: return false
        if (durationMs <= 0L) return true
        return position >= (durationMs - COMPLETED_WINDOW_MS).coerceAtLeast(0L)
    }
}
