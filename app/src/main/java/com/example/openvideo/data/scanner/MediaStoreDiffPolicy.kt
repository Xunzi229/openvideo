package com.example.openvideo.data.scanner

import com.example.openvideo.data.model.VideoItem

data class MediaStoreDiff(
    val removedIds: Set<Long>,
    val addedIds: Set<Long>,
    val changedIds: Set<Long>
) {
    val mutationCount: Int get() = removedIds.size + addedIds.size + changedIds.size
}

object MediaStoreDiffPolicy {

    fun diff(
        previous: Map<Long, MediaStoreIndexEntry>,
        current: Map<Long, MediaStoreIndexEntry>
    ): MediaStoreDiff {
        val previousIds = previous.keys
        val currentIds = current.keys
        val removedIds = previousIds - currentIds
        val addedIds = currentIds - previousIds
        val changedIds = currentIds.intersect(previousIds).filterTo(mutableSetOf()) { id ->
            previous.getValue(id) != current.getValue(id)
        }
        return MediaStoreDiff(
            removedIds = removedIds,
            addedIds = addedIds,
            changedIds = changedIds
        )
    }

    fun mergeCachedVideos(
        cache: Map<Long, VideoItem>,
        diff: MediaStoreDiff,
        updatedVideos: Map<Long, VideoItem>
    ): List<VideoItem> {
        val merged = cache.toMutableMap()
        diff.removedIds.forEach { merged.remove(it) }
        updatedVideos.forEach { (id, video) -> merged[id] = video }
        return merged.values.sortedByDescending { it.dateAdded }
    }
}
