package com.example.openvideo.ui.player

data class PlayerSubtitlePresentation(
    val visible: Boolean,
    val text: String
)

object PlayerSubtitlePresentationPolicy {
    fun present(
        subtitlesEnabled: Boolean,
        subtitleText: String
    ): PlayerSubtitlePresentation {
        if (!subtitlesEnabled || subtitleText.isBlank()) {
            return PlayerSubtitlePresentation(
                visible = false,
                text = ""
            )
        }
        return PlayerSubtitlePresentation(
            visible = true,
            text = subtitleText
        )
    }
}
