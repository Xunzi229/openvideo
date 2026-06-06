package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class PlayerDualSubtitleStateSourceTest {

    @Test
    fun playerUiStateExposesDualSubtitleState() {
        val source = String(Files.readAllBytes(playerViewModelSource()))

        assertTrue(source.contains("import com.example.openvideo.core.subtitle.DualSubtitleState"))
        assertTrue(source.contains("val dualSubtitles: DualSubtitleState = DualSubtitleState()"))
        assertTrue(source.contains("dualSubtitles = DualSubtitleState(primary = PrimarySubtitle(items = subtitles))"))
    }

    private fun playerViewModelSource() =
        sequenceOf(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerViewModel.kt"),
            Paths.get("app", "src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerViewModel.kt")
        ).first(Files::exists)
}
