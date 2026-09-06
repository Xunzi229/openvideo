package com.openvideo.app.ui.playlist

import com.openvideo.app.core.media.LocalMediaUriPolicy
import com.openvideo.app.data.local.PlaylistVideoEntity

object PlaylistVideoAvailabilityPolicy {

    fun isAvailable(videoPath: String): Boolean =
        LocalMediaUriPolicy.isPlayable(videoPath)

    fun filterPlayable(entities: List<PlaylistVideoEntity>): List<PlaylistVideoEntity> =
        entities.filter { isAvailable(it.videoPath) }

    fun staleVideoIds(entities: List<PlaylistVideoEntity>): List<Long> =
        entities.filterNot { isAvailable(it.videoPath) }.map { it.videoId }
}
