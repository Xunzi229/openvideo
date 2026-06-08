package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSubtitleInfoUiSourceTest {

    @Test
    fun subtitleSettingsSheetShowsCurrentSubtitleInfoSummary() {
        val sheet = sourceText("PlayerSubtitleSettingsSheet.kt")
        val viewModel = sourceText("PlayerViewModel.kt")
        val layout = layoutText("activity_player_subtitle_settings.xml")

        assertTrue(layout.contains("@+id/tv_subtitle_info_summary"))
        assertTrue(layout.contains("@string/player_settings_subtitle_info_title"))
        assertTrue(sheet.contains("viewModel.currentSubtitleInfo()"))
        assertTrue(sheet.contains("PlayerSubtitleInfoUiPolicy.summaryText("))
        assertTrue(sheet.contains("R.string.player_settings_subtitle_info_empty"))
        assertTrue(viewModel.contains("fun currentSubtitleInfo()"))
        assertTrue(viewModel.contains("SubtitleInfoPolicy.summarize("))
        assertTrue(viewModel.contains("encoding = playerPrefs.subtitleEncoding"))
    }

    private fun sourceText(name: String): String =
        String(Files.readAllBytes(rootFile(Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", name))))

    private fun layoutText(name: String): String =
        String(Files.readAllBytes(rootFile(Paths.get("src", "main", "res", "layout", name))))

    private fun rootFile(relativePath: Path): Path =
        sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
}
