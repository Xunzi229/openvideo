package com.example.openvideo.ui.home

import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.local.VideoFolderGrouper

object MediaLibrarySearchPolicy {

    private const val SHORT_MAX_MS = 5 * 60 * 1000L
    private const val MEDIUM_MAX_MS = 60 * 60 * 1000L
    private const val DAY_SECONDS = 86_400L

    fun matchesQuery(title: String, path: String, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        return title.contains(trimmed, ignoreCase = true) ||
            path.contains(trimmed, ignoreCase = true) ||
            fileName(path).contains(trimmed, ignoreCase = true) ||
            VideoFolderGrouper.folderName(VideoFolderGrouper.folderKey(path))
                .contains(trimmed, ignoreCase = true)
    }

    fun matchesQuery(video: VideoItem, query: String): Boolean =
        matchesQuery(video.title, video.path, query)

    fun matchesAdvanced(
        path: String,
        durationMs: Long,
        dateAddedSec: Long,
        filters: MediaLibraryAdvancedFilters,
        nowEpochSec: Long
    ): Boolean {
        if (!matchesDuration(durationMs, filters.durationFilter)) return false
        if (!matchesFormat(path, filters.formatExtension)) return false
        if (!matchesDate(dateAddedSec, filters.dateFilter, nowEpochSec)) return false
        return true
    }

    fun matchesAdvanced(
        video: VideoItem,
        filters: MediaLibraryAdvancedFilters,
        nowEpochSec: Long
    ): Boolean = matchesAdvanced(
        path = video.path,
        durationMs = video.duration,
        dateAddedSec = video.dateAdded,
        filters = filters,
        nowEpochSec = nowEpochSec
    )

    fun matchesLibrary(
        title: String,
        path: String,
        durationMs: Long,
        dateAddedSec: Long,
        query: String,
        filters: MediaLibraryAdvancedFilters,
        nowEpochSec: Long
    ): Boolean = matchesQuery(title, path, query) &&
        matchesAdvanced(path, durationMs, dateAddedSec, filters, nowEpochSec)

    fun matchesLibrary(
        video: VideoItem,
        query: String,
        filters: MediaLibraryAdvancedFilters,
        nowEpochSec: Long
    ): Boolean = matchesLibrary(
        title = video.title,
        path = video.path,
        durationMs = video.duration,
        dateAddedSec = video.dateAdded,
        query = query,
        filters = filters,
        nowEpochSec = nowEpochSec
    )

    fun fileExtension(path: String): String =
        path.substringAfterLast('.', "").lowercase()

    private fun fileName(path: String): String =
        path.substringAfterLast('/').substringAfterLast('\\')

    private fun matchesDuration(durationMs: Long, filter: DurationFilter): Boolean = when (filter) {
        DurationFilter.ANY -> true
        DurationFilter.SHORT -> durationMs in 1..SHORT_MAX_MS
        DurationFilter.MEDIUM -> durationMs > SHORT_MAX_MS && durationMs <= MEDIUM_MAX_MS
        DurationFilter.LONG -> durationMs > MEDIUM_MAX_MS
    }

    private fun matchesFormat(path: String, extension: String?): Boolean {
        if (extension.isNullOrBlank()) return true
        return fileExtension(path) == extension.lowercase()
    }

    private fun matchesDate(dateAddedSec: Long, filter: DateFilter, nowEpochSec: Long): Boolean {
        if (filter == DateFilter.ANY) return true
        val ageSec = (nowEpochSec - dateAddedSec).coerceAtLeast(0L)
        return when (filter) {
            DateFilter.TODAY -> ageSec <= DAY_SECONDS
            DateFilter.LAST_7_DAYS -> ageSec <= 7 * DAY_SECONDS
            DateFilter.LAST_30_DAYS -> ageSec <= 30 * DAY_SECONDS
            DateFilter.OLDER_THAN_30_DAYS -> ageSec > 30 * DAY_SECONDS
            DateFilter.ANY -> true
        }
    }
}
