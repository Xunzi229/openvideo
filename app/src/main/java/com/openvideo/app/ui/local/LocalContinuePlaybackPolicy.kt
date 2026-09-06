package com.openvideo.app.ui.local

import com.openvideo.app.data.local.HistoryEntity

object LocalContinuePlaybackPolicy {

    fun latestPlayableVideoId(
        history: List<HistoryEntity>,
        visibleVideoIds: Set<Long>,
        visibleVideoPaths: Set<String>
    ): Long? {
        return history
            .sortedByDescending { it.timestamp }
            .firstOrNull { item ->
                item.lastPosition > 0 &&
                    (item.videoId in visibleVideoIds || item.path in visibleVideoPaths)
            }
            ?.videoId
    }
}
