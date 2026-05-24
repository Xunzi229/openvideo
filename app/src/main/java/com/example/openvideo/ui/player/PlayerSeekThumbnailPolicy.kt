package com.example.openvideo.ui.player

/**
 * Seek 缩略图预览策略（纯函数）。
 */
object PlayerSeekThumbnailPolicy {

    /**
     * 根据 SeekBar 的进度计算缩略图对应的时间戳（毫秒）。
     */
    fun thumbnailPositionMs(seekProgress: Int, max: Int, durationMs: Long): Long {
        return PlayerTimeline.positionFromSeekBar(seekProgress, max, durationMs)
    }

    /**
     * 抽帧调用的节流间隔时间（毫秒）。
     */
    fun throttleIntervalMs(): Long = 200L

    /**
     * 是否应跳过缩略图预览（例如暂不支持的网络协议）。
     */
    fun shouldSkipThumbnail(uriScheme: String?): Boolean {
        if (uriScheme == null) return true
        val scheme = uriScheme.lowercase()
        return scheme != "file" && scheme != "content"
    }
}
