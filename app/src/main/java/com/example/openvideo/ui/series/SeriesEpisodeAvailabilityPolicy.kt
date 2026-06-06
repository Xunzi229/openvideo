package com.example.openvideo.ui.series

import com.example.openvideo.core.media.LocalMediaUriPolicy

object SeriesEpisodeAvailabilityPolicy {
    fun isAvailable(videoPath: String): Boolean =
        LocalMediaUriPolicy.isPlayable(videoPath)
}
