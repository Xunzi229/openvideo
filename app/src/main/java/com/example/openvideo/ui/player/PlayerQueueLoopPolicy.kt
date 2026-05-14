package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.LoopMode

object PlayerQueueLoopPolicy {

    fun nextIndexAfterEnded(
        currentIndex: Int,
        queueSize: Int,
        autoPlayNext: Boolean,
        loopMode: LoopMode
    ): Int? {
        if (!autoPlayNext) return null
        if (queueSize <= 1 || currentIndex !in 0 until queueSize) return null
        if (loopMode != LoopMode.LIST) return null

        val nextIndex = currentIndex + 1
        if (nextIndex < queueSize) return nextIndex

        return 0
    }
}
