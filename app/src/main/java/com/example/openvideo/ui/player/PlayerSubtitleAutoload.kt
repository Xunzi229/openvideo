package com.example.openvideo.ui.player

object PlayerSubtitleAutoload {
    private val subtitleExtensions = setOf("srt", "ass", "ssa", "vtt")

    fun canLoadAsSubtitleUri(uriString: String): Boolean {
        val lower = uriString.substringBefore('?').lowercase()
        return subtitleExtensions.any { lower.endsWith(".$it") }
    }
}
