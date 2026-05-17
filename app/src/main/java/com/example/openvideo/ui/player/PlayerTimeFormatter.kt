package com.example.openvideo.ui.player

/**
 * 把毫秒时间格式化为 `HH:MM:SS` / `MM:SS` 的纯策略。
 *
 * - 负数视作 0；
 * - 不足 1 小时只输出 `MM:SS`；
 * - 大于等于 1 小时输出 `H:MM:SS`，小时位不补零（与现有播放器 UI 习惯保持一致）。
 *
 * 纯函数，便于在 JVM 单测里覆盖边界。
 */
object PlayerTimeFormatter {

    fun format(timeMs: Long): String {
        val totalSec = (timeMs.coerceAtLeast(0L)) / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
