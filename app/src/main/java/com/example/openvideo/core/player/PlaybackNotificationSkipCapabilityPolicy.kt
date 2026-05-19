package com.example.openvideo.core.player

import com.example.openvideo.core.prefs.LoopMode
import com.example.openvideo.data.model.VideoItem

/**
 * 从会话快照推导通知栏与 MediaSession 是否应暴露上一集/下一集控制。
 */
object PlaybackNotificationSkipCapabilityPolicy {

    data class Capabilities(
        val canSkipToNext: Boolean,
        val canSkipToPrevious: Boolean
    ) {
        val hasAnySkip: Boolean
            get() = canSkipToNext || canSkipToPrevious
    }

    fun fromSnapshot(snapshot: PlaybackNotificationCoordinator.Snapshot?): Capabilities {
        if (snapshot == null) return Capabilities(canSkipToNext = false, canSkipToPrevious = false)
        return fromQueue(snapshot.queue, snapshot.videoId, snapshot.loopMode)
    }

    fun fromQueue(
        queue: List<VideoItem>,
        currentVideoId: Long,
        loopMode: LoopMode
    ): Capabilities {
        val currentIndex = queue.indexOfFirst { it.id == currentVideoId }
        return fromQueueIndex(currentIndex, queue.size, loopMode)
    }

    fun fromQueueIndex(
        currentIndex: Int,
        queueSize: Int,
        loopMode: LoopMode
    ): Capabilities {
        if (queueSize <= 1 || currentIndex !in 0 until queueSize) {
            return Capabilities(canSkipToNext = false, canSkipToPrevious = false)
        }
        return Capabilities(
            canSkipToNext = PlaybackQueueSkipPolicy.nextIndex(
                currentIndex,
                queueSize,
                loopMode
            ) != null,
            canSkipToPrevious = PlaybackQueueSkipPolicy.previousIndex(
                currentIndex,
                queueSize,
                loopMode
            ) != null
        )
    }
}
