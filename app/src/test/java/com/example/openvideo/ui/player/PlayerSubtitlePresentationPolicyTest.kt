package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSubtitlePresentationPolicyTest {

    @Test
    fun hidesSubtitleWhenSubtitlesAreDisabled() {
        val presentation = PlayerSubtitlePresentationPolicy.present(
            subtitlesEnabled = false,
            subtitleText = "hello"
        )

        assertEquals(false, presentation.visible)
        assertEquals("", presentation.text)
    }

    @Test
    fun hidesSubtitleWhenTextIsEmpty() {
        val presentation = PlayerSubtitlePresentationPolicy.present(
            subtitlesEnabled = true,
            subtitleText = ""
        )

        assertEquals(false, presentation.visible)
        assertEquals("", presentation.text)
    }

    @Test
    fun hidesSubtitleWhenTextIsBlank() {
        val presentation = PlayerSubtitlePresentationPolicy.present(
            subtitlesEnabled = true,
            subtitleText = "   "
        )

        assertEquals(false, presentation.visible)
        assertEquals("", presentation.text)
    }

    @Test
    fun showsSubtitleWhenEnabledAndTextExists() {
        val presentation = PlayerSubtitlePresentationPolicy.present(
            subtitlesEnabled = true,
            subtitleText = " hello "
        )

        assertEquals(true, presentation.visible)
        assertEquals(" hello ", presentation.text)
    }
}
