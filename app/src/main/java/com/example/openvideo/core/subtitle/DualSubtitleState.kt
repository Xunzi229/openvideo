package com.example.openvideo.core.subtitle

data class PrimarySubtitle(
    val items: List<SubtitleItem> = emptyList(),
    val enabled: Boolean = true
)

data class SecondarySubtitle(
    val items: List<SubtitleItem> = emptyList(),
    val enabled: Boolean = false
)

data class DualSubtitleText(
    val primary: String,
    val secondary: String,
    val primaryStyle: SubtitleCueStyle? = null,
    val secondaryStyle: SubtitleCueStyle? = null
)

data class DualSubtitleState(
    val primary: PrimarySubtitle = PrimarySubtitle(),
    val secondary: SecondarySubtitle = SecondarySubtitle()
) {
    fun textAt(positionMs: Long, delayMs: Long = 0): DualSubtitleText? {
        val adjustedPosition = positionMs + delayMs
        val primaryItem = if (primary.enabled) primary.items.itemAt(adjustedPosition) else null
        val secondaryItem = if (secondary.enabled) secondary.items.itemAt(adjustedPosition) else null
        val primaryText = primaryItem?.text.orEmpty()
        val secondaryText = secondaryItem?.text.orEmpty()
        if (primaryText.isEmpty() && secondaryText.isEmpty()) return null
        return DualSubtitleText(
            primary = primaryText,
            secondary = secondaryText,
            primaryStyle = primaryItem?.style,
            secondaryStyle = secondaryItem?.style
        )
    }

    private fun List<SubtitleItem>.itemAt(positionMs: Long): SubtitleItem? =
        firstOrNull { positionMs in it.startTimeMs..it.endTimeMs }
}
