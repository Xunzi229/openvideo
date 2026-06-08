package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerDualSubtitleStyleSourceTest {

    @Test
    fun playerPrefsExposeIndependentSecondarySubtitleStyleKeys() {
        val source = rootText("app", "src", "main", "java", "com", "example", "openvideo", "core", "prefs", "PlayerPrefs.kt")

        assertTrue(source.contains("var secondarySubtitleSize: Int"))
        assertTrue(source.contains("var secondarySubtitleColor: Int"))
        assertTrue(source.contains("var secondarySubtitleBgStyle: SubtitleBgStyle"))
        assertTrue(source.contains("var secondarySubtitlePosition: Float"))
        assertTrue(source.contains("KEY_SECONDARY_SUBTITLE_SIZE"))
        assertTrue(source.contains("KEY_SECONDARY_SUBTITLE_COLOR"))
        assertTrue(source.contains("KEY_SECONDARY_SUBTITLE_BG"))
        assertTrue(source.contains("KEY_SECONDARY_SUBTITLE_POSITION"))
        assertTrue(source.contains("KEY_SECONDARY_SUBTITLE_SIZE,"))
        assertTrue(source.contains("KEY_SECONDARY_SUBTITLE_COLOR,"))
        assertTrue(source.contains("KEY_SECONDARY_SUBTITLE_BG,"))
        assertTrue(source.contains("KEY_SECONDARY_SUBTITLE_POSITION,"))
    }

    @Test
    fun displayControllerAppliesPrimaryAndSecondarySubtitleStyleSeparately() {
        val source = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerDisplayController.kt"
        )

        assertFalse(source.contains("listOf(subtitleProvider(), secondarySubtitleProvider()).forEach"))
        assertTrue(source.contains("sizeSp = playerPrefs.subtitleSize"))
        assertTrue(source.contains("sizeSp = playerPrefs.secondarySubtitleSize"))
        assertTrue(source.contains("playerPrefs.subtitleColor"))
        assertTrue(source.contains("playerPrefs.secondarySubtitleColor"))
        assertTrue(source.contains("bgStyle = playerPrefs.subtitleBgStyle"))
        assertTrue(source.contains("bgStyle = playerPrefs.secondarySubtitleBgStyle"))
        assertTrue(source.contains("position = playerPrefs.subtitlePosition"))
        assertTrue(source.contains("position = playerPrefs.secondarySubtitlePosition"))
    }

    @Test
    fun playbackTickUsesSecondaryStyleDefaultsForSecondaryAssCueFallback() {
        val source = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerPlaybackTickController.kt"
        )

        assertTrue(source.contains("defaultTextSizeSp = playerPrefs.subtitleSize"))
        assertTrue(source.contains("defaultTextColor = playerPrefs.subtitleColor"))
        assertTrue(source.contains("defaultTextSizeSp = playerPrefs.secondarySubtitleSize"))
        assertTrue(source.contains("defaultTextColor = playerPrefs.secondarySubtitleColor"))
    }

    @Test
    fun subtitleSettingsSheetExposesSecondarySubtitleStyleControls() {
        val layout = rootText("app", "src", "main", "res", "layout", "activity_player_subtitle_settings.xml")
        val sheet = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerSubtitleSettingsSheet.kt"
        )

        assertTrue(layout.contains("@+id/tv_secondary_subtitle_preview"))
        assertTrue(layout.contains("@+id/sb_secondary_subtitle_size"))
        assertTrue(layout.contains("@+id/tv_secondary_subtitle_size_value"))
        assertTrue(layout.contains("@+id/secondary_subtitle_color_swatch_row"))
        assertTrue(layout.contains("@+id/tv_secondary_subtitle_bg_value"))
        assertTrue(layout.contains("@+id/sb_secondary_subtitle_position"))
        assertTrue(sheet.contains("playerPrefs.secondarySubtitleSize"))
        assertTrue(sheet.contains("playerPrefs.secondarySubtitleColor"))
        assertTrue(sheet.contains("playerPrefs.secondarySubtitleBgStyle"))
        assertTrue(sheet.contains("playerPrefs.secondarySubtitlePosition"))
    }

    @Test
    fun settingsBackupIncludesSecondarySubtitleStyleFields() {
        val schema = rootText("app", "src", "main", "java", "com", "example", "openvideo", "core", "prefs", "SettingsBackupSchema.kt")
        val exporter = rootText("app", "src", "main", "java", "com", "example", "openvideo", "core", "prefs", "SettingsBackupExporter.kt")
        val importer = rootText("app", "src", "main", "java", "com", "example", "openvideo", "core", "prefs", "SettingsBackupImporter.kt")
        val allowlist = rootText("app", "src", "main", "java", "com", "example", "openvideo", "core", "prefs", "SettingsBackupAllowlistPolicy.kt")

        listOf(
            "secondarySubtitleSize",
            "secondarySubtitleColor",
            "secondarySubtitleBgStyle",
            "secondarySubtitlePosition"
        ).forEach { field ->
            assertTrue(schema.contains("val $field"))
            assertTrue(schema.contains("\"$field\""))
            assertTrue(exporter.contains("$field = playerPrefs.$field"))
            assertTrue(importer.contains("section.$field"))
            assertTrue(allowlist.contains("\"$field\""))
        }
    }

    @Test
    fun phaseRoadmapMarksSecondarySubtitleStyleSliceDone() {
        val roadmap = rootText("docs", "roadmap", "phases", "phase-4-subtitles-content", "README.md")
        val masterRoadmap = rootText("docs", "roadmap", "ROADMAP.md")

        assertTrue(roadmap.contains("P4-DUAL-004"))
        assertTrue(roadmap.contains("主副字幕样式独立"))
        assertTrue(masterRoadmap.contains("Phase 4 / P4-DUAL-004 主副字幕样式独立"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative))
                    .firstOrNull(Files::exists) ?: relative
            }
}
