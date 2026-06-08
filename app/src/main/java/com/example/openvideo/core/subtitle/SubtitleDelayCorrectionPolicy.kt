package com.example.openvideo.core.subtitle

data class SubtitleDelayCorrectionPlan(
    val deltaMs: Int,
    val items: List<SubtitleItem>,
    val changedLineCount: Int,
    val suggestedCopyName: String,
    val overwritesOriginal: Boolean = false
)

object SubtitleDelayCorrectionPolicy {

    fun planShiftedCopy(
        items: List<SubtitleItem>,
        deltaMs: Int,
        sourceName: String
    ): SubtitleDelayCorrectionPlan =
        SubtitleDelayCorrectionPlan(
            deltaMs = deltaMs,
            items = items.map { item ->
                item.copy(
                    startTimeMs = (item.startTimeMs + deltaMs).coerceAtLeast(0L),
                    endTimeMs = (item.endTimeMs + deltaMs).coerceAtLeast(0L)
                )
            },
            changedLineCount = items.size,
            suggestedCopyName = suggestedCopyName(sourceName, deltaMs)
        )

    private fun suggestedCopyName(sourceName: String, deltaMs: Int): String {
        val cleanName = sourceName.trim().substringAfterLast('/').substringAfterLast('\\')
        val dotIndex = cleanName.lastIndexOf('.')
        val baseName = cleanName
            .takeIf { it.isNotBlank() && dotIndex > 0 }
            ?.substring(0, dotIndex)
            ?: DEFAULT_BASE_NAME
        val extension = cleanName
            .takeIf { dotIndex > 0 && dotIndex < cleanName.lastIndex }
            ?.substring(dotIndex + 1)
            ?: DEFAULT_EXTENSION
        val sign = if (deltaMs >= 0) "+" else ""
        return "$baseName.shift$sign${deltaMs}ms.$extension"
    }

    private const val DEFAULT_BASE_NAME = "subtitle"
    private const val DEFAULT_EXTENSION = "srt"
}
