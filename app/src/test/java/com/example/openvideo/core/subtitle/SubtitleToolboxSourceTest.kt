package com.example.openvideo.core.subtitle

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SubtitleToolboxSourceTest {

    @Test
    fun roadmapDocumentsSubtitleToolboxInfoFoundation() {
        val phaseRoadmap = rootText("docs", "roadmap", "phases", "phase-4-subtitles-content", "README.md")
        val masterRoadmap = rootText("docs", "roadmap", "ROADMAP.md")

        assertTrue(phaseRoadmap.contains("P4-TOOLS-001"))
        assertTrue(phaseRoadmap.contains("SubtitleInfoPolicy"))
        assertTrue(phaseRoadmap.contains("lineCount"))
        assertTrue(phaseRoadmap.contains("time range"))
        assertTrue(phaseRoadmap.contains("styledLineCount"))
        assertTrue(phaseRoadmap.contains("UI 未包含"))
        assertTrue(phaseRoadmap.contains("另存 UTF-8"))
        assertTrue(phaseRoadmap.contains("延迟批量校正"))

        assertTrue(masterRoadmap.contains("Phase 4 / P4-TOOLS-001"))
        assertTrue(masterRoadmap.contains("SubtitleInfoPolicy"))
    }

    @Test
    fun subtitleToolboxInfoFoundationDoesNotDuplicateExistingSettingsUi() {
        val policy = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "subtitle",
            "SubtitleInfoPolicy.kt"
        )
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

        assertTrue(policy.contains("object SubtitleInfoPolicy"))
        assertTrue(policy.contains("data class SubtitleInfo"))
        assertTrue(sheet.contains("playerPrefs.subtitleDelayMs"))
        assertTrue(sheet.contains("playerPrefs.subtitleEncoding"))
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
