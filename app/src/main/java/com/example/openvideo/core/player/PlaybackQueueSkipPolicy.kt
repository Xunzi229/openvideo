package com.example.openvideo.core.player

import com.example.openvideo.core.prefs.LoopMode

/**
 * 通知栏 / MediaSession / 锁屏媒体卡共用的队列切歌索引策略。
 */
object PlaybackQueueSkipPolicy {

    fun nextIndex(
        currentIndex: Int,
        queueSize: Int,
        loopMode: LoopMode = LoopMode.OFF
    ): Int? {
        if (queueSize <= 1 || currentIndex !in 0 until queueSize) return null
        val next = currentIndex + 1
        if (next < queueSize) return next
        return if (loopMode == LoopMode.LIST) 0 else null
    }

    fun previousIndex(
        currentIndex: Int,
        queueSize: Int,
        loopMode: LoopMode = LoopMode.OFF
    ): Int? {
        if (queueSize <= 1 || currentIndex !in 0 until queueSize) return null
        val prev = currentIndex - 1
        if (prev >= 0) return prev
        return if (loopMode == LoopMode.LIST) queueSize - 1 else null
    }
}
