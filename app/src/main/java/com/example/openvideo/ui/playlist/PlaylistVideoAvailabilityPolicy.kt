package com.example.openvideo.ui.playlist

import com.example.openvideo.core.media.LocalMediaUriPolicy
import com.example.openvideo.data.local.PlaylistVideoEntity

object PlaylistVideoAvailabilityPolicy {

    fun isAvailable(videoPath: String): Boolean =
        LocalMediaUriPolicy.isPlayable(videoPath)

    fun filterPlayable(entities: List<PlaylistVideoEntity>): List<PlaylistVideoEntity> =
        entities.filter { isAvailable(it.videoPath) }

    fun staleVideoIds(entities: List<PlaylistVideoEntity>): List<Long> =
        entities.filterNot { isAvailable(it.videoPath) }.map { it.videoId }
}
