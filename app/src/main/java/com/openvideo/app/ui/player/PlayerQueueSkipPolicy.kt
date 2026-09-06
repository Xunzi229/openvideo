package com.openvideo.app.ui.player

import com.openvideo.app.core.player.PlaybackQueueSkipPolicy
import com.openvideo.app.core.prefs.LoopMode

/**
 * 会话队列内手动切换上/下一条（通知栏、MediaSession），不套用片尾自动连播策略。
 */
object PlayerQueueSkipPolicy {

    fun nextIndex(currentIndex: Int, queueSize: Int, loopMode: LoopMode = LoopMode.OFF): Int? =
        PlaybackQueueSkipPolicy.nextIndex(currentIndex, queueSize, loopMode)

    fun previousIndex(currentIndex: Int, queueSize: Int, loopMode: LoopMode = LoopMode.OFF): Int? =
        PlaybackQueueSkipPolicy.previousIndex(currentIndex, queueSize, loopMode)
}
