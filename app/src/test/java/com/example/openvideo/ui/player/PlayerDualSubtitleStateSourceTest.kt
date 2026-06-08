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

    @Test
    fun playerViewModelCanSetAndToggleSecondarySubtitleTrack() {
        val source = String(Files.readAllBytes(playerViewModelSource()))

        assertTrue(source.contains("import com.example.openvideo.core.subtitle.SecondarySubtitle"))
        assertTrue(source.contains("fun setSecondarySubtitles(subtitles: List<SubtitleItem>, enabled: Boolean = true)"))
        assertTrue(source.contains("fun setSecondarySubtitlesEnabled(enabled: Boolean)"))
        assertTrue(source.contains("secondary = SecondarySubtitle(items = subtitles, enabled = enabled)"))
    }

    @Test
    fun subtitleSettingsSheetExposesSecondarySubtitleEntryAndToggle() {
        val sheet = String(Files.readAllBytes(playerSubtitleSettingsSheetSource()))
        val layout = String(Files.readAllBytes(playerSubtitleSettingsLayoutSource()))

        assertTrue(layout.contains("@+id/btn_load_secondary_subtitle"))
        assertTrue(layout.contains("@+id/switch_secondary_subtitle_enabled"))
        assertTrue(sheet.contains("pickSecondarySubtitleLauncher"))
        assertTrue(sheet.contains("viewModel.loadSecondarySubtitles("))
        assertTrue(sheet.contains("viewModel.setSecondarySubtitlesEnabled(checked)"))
    }

    private fun playerViewModelSource() =
        sequenceOf(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerViewModel.kt"),
            Paths.get("app", "src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerViewModel.kt")
        ).first(Files::exists)

    private fun playerSubtitleSettingsSheetSource() =
        sequenceOf(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerSubtitleSettingsSheet.kt"),
            Paths.get("app", "src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerSubtitleSettingsSheet.kt")
        ).first(Files::exists)

    private fun playerSubtitleSettingsLayoutSource() =
        sequenceOf(
            Paths.get("src", "main", "res", "layout", "activity_player_subtitle_settings.xml"),
            Paths.get("app", "src", "main", "res", "layout", "activity_player_subtitle_settings.xml")
        ).first(Files::exists)
}
