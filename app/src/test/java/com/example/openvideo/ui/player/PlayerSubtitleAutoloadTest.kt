package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSubtitleAutoloadTest {

    @Test
    fun doesNotTreatContentVideoUriAsSubtitleFile() {
        assertFalse(PlayerSubtitleAutoload.canLoadAsSubtitleUri("content://media/external/video/media/42"))
    }

    @Test
    fun allowsExplicitSubtitleDocuments() {
        assertTrue(PlayerSubtitleAutoload.canLoadAsSubtitleUri("file:///storage/emulated/0/Movies/demo.srt"))
        assertTrue(PlayerSubtitleAutoload.canLoadAsSubtitleUri("file:///storage/emulated/0/Movies/demo.ass"))
        assertTrue(PlayerSubtitleAutoload.canLoadAsSubtitleUri("file:///storage/emulated/0/Movies/demo.vtt"))
    }
}
