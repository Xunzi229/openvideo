package com.example.openvideo.ui.player

import com.example.openvideo.core.subtitle.SubtitleInfo

object PlayerSubtitleInfoUiPolicy {

    data class Labels(
        val noLoadedSubtitle: String,
        val sourcePrefix: String,
        val linesPrefix: String,
        val timeRangePrefix: String,
        val encodingPrefix: String,
        val styledLinesPrefix: String
    )

    fun summaryText(info: SubtitleInfo, labels: Labels): String {
        if (info.isEmpty) return labels.noLoadedSubtitle
        return listOf(
            "${labels.sourcePrefix} ${info.sourceLabel}",
            "${labels.linesPrefix} ${info.lineCount}",
            "${labels.timeRangePrefix} ${formatTime(info.firstStartTimeMs)} - ${formatTime(info.lastEndTimeMs)}",
            "${labels.encodingPrefix} ${info.encodingLabel}",
            "${labels.styledLinesPrefix} ${info.styledLineCount}"
        ).joinToString(separator = "\n")
    }

    private fun formatTime(timeMs: Long): String {
        val safeMs = timeMs.coerceAtLeast(0L)
        val hours = safeMs / 3_600_000
        val minutes = (safeMs % 3_600_000) / 60_000
        val seconds = (safeMs % 60_000) / 1_000
        val millis = safeMs % 1_000
        return "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, millis)
    }
}
