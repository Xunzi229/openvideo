package com.example.openvideo.ui.player

/**
 * 首帧超时检测的纯策略。
 *
 * 当 ExoPlayer 进入 `STATE_READY` 但长时间没有渲染出首帧时，可能是软解切换 / 编解码退化 /
 * 解码硬件异常等问题。此策略决定：
 * - 收到 `STATE_READY` 后应等待多长时间再判定为超时（[scheduleDelayMs]）。
 * - 收到首帧或视频源没有视频轨道时应取消等待（[onFirstFrameRendered]、[onReadyWithoutVideoTrack]）。
 *
 * 纯函数 + 一份 `Long` 阈值，便于在 JVM 单测里覆盖各种状态。
 */
object PlayerFirstFrameTimeoutPolicy {

    /** 首帧从 `STATE_READY` 起计的默认等待阈值（毫秒）。 */
    const val DEFAULT_TIMEOUT_MS = 8_000L

    /**
     * 判断当前是否需要排程一次"首帧超时"检查。返回 null 表示无需排程。
     */
    fun scheduleDelayMs(
        hasVideoTrack: Boolean,
        firstFrameRendered: Boolean,
        alreadyTimedOut: Boolean,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Long? {
        if (!hasVideoTrack) return null
        if (firstFrameRendered) return null
        if (alreadyTimedOut) return null
        return timeoutMs.coerceAtLeast(0L)
    }

    /**
     * 计算 [PlayerStartupTrace] 中 `prepare_ready` -> `first_frame_rendered` 实际等待时长，
     * 超过 [timeoutMs] 则视为首帧迟滞，用于诊断日志的判定。
     */
    fun isFirstFrameLate(
        prepareReadyElapsedMs: Long?,
        firstFrameElapsedMs: Long?,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Boolean {
        if (prepareReadyElapsedMs == null) return false
        if (firstFrameElapsedMs == null) return true
        return firstFrameElapsedMs - prepareReadyElapsedMs >= timeoutMs
    }
}
