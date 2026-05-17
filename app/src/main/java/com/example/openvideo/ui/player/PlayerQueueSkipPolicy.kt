package com.example.openvideo.ui.player

/**
 * 会话队列内手动切换上/下一条（通知栏、MediaSession），不套用片尾自动连播策略。
 */
object PlayerQueueSkipPolicy {

    fun nextIndex(currentIndex: Int, queueSize: Int): Int? {
        if (queueSize <= 1 || currentIndex !in 0 until queueSize) return null
        val next = currentIndex + 1
        return if (next < queueSize) next else null
    }

    fun previousIndex(currentIndex: Int, queueSize: Int): Int? {
        if (queueSize <= 1 || currentIndex !in 0 until queueSize) return null
        val prev = currentIndex - 1
        return if (prev >= 0) prev else null
    }
}
