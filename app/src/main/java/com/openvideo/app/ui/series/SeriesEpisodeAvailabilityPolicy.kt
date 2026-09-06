package com.openvideo.app.ui.series

import com.openvideo.app.core.media.LocalMediaUriPolicy

object SeriesEpisodeAvailabilityPolicy {
    fun isAvailable(videoPath: String): Boolean =
        LocalMediaUriPolicy.isPlayable(videoPath)
}
