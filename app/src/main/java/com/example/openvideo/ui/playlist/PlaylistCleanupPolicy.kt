package com.example.openvideo.ui.playlist

import com.example.openvideo.core.mediaid.MediaPathNormalizer
import com.example.openvideo.data.local.PlaylistVideoEntity

data class PlaylistCleanupPlan(
    val missingVideoIds: List<Long>,
    val duplicateVideoIds: List<Long>
) {
    val removableVideoIds: List<Long> = (missingVideoIds + duplicateVideoIds).distinct()
}

object PlaylistCleanupPolicy {

    fun plan(videos: List<PlaylistVideoEntity>): PlaylistCleanupPlan {
        val sorted = videos.sortedBy { it.position }
        val missingVideoIds = PlaylistVideoAvailabilityPolicy.staleVideoIds(sorted)
        val seenKeys = mutableSetOf<String>()
        val duplicateVideoIds = mutableListOf<Long>()

        sorted.forEach { video ->
            val key = duplicateKey(video) ?: return@forEach
            if (!seenKeys.add(key)) {
                duplicateVideoIds += video.videoId
            }
        }

        return PlaylistCleanupPlan(
            missingVideoIds = missingVideoIds,
            duplicateVideoIds = duplicateVideoIds
        )
    }

    private fun duplicateKey(video: PlaylistVideoEntity): String? {
        video.mediaIdentityId?.let { return "identity:$it" }
        val normalized = MediaPathNormalizer.normalize(video.videoPath) ?: return null
        return "path:${normalized.comparisonKey}"
    }
}
