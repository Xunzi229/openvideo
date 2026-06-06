package com.example.openvideo.core.subtitle

data class SubtitleInfo(
    val sourceLabel: String,
    val encodingLabel: String,
    val lineCount: Int,
    val firstStartTimeMs: Long,
    val lastEndTimeMs: Long,
    val durationMs: Long,
    val styledLineCount: Int,
    val hasStyledCues: Boolean,
    val isEmpty: Boolean,
    val status: SubtitleInfoStatus
)

enum class SubtitleInfoStatus {
    Loaded,
    Empty
}

object SubtitleInfoPolicy {

    fun summarize(
        items: List<SubtitleItem>,
        sourceLabel: String,
        encoding: String
    ): SubtitleInfo {
        val sortedItems = items.sortedWith(compareBy<SubtitleItem> { it.startTimeMs }.thenBy { it.endTimeMs })
        val firstStartTimeMs = sortedItems.firstOrNull()?.startTimeMs ?: 0L
        val lastEndTimeMs = sortedItems.lastOrNull()?.endTimeMs ?: 0L
        val styledLineCount = items.count { it.style != null }

        return SubtitleInfo(
            sourceLabel = sourceLabel.trim().takeIf { it.isNotEmpty() } ?: UNKNOWN_SUBTITLE_LABEL,
            encodingLabel = encodingLabel(encoding),
            lineCount = items.size,
            firstStartTimeMs = firstStartTimeMs,
            lastEndTimeMs = lastEndTimeMs,
            durationMs = (lastEndTimeMs - firstStartTimeMs).coerceAtLeast(0L),
            styledLineCount = styledLineCount,
            hasStyledCues = styledLineCount > 0,
            isEmpty = items.isEmpty(),
            status = if (items.isEmpty()) SubtitleInfoStatus.Empty else SubtitleInfoStatus.Loaded
        )
    }

    private fun encodingLabel(encoding: String): String =
        if (encoding.equals("auto", ignoreCase = true)) AUTO_ENCODING_LABEL else encoding

    private const val UNKNOWN_SUBTITLE_LABEL = "Unknown subtitle"
    private const val AUTO_ENCODING_LABEL = "Auto detect"
}
