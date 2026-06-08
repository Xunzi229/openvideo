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
        assertTrue(phaseRoadmap.contains("SubtitleDelayCorrectionPolicy"))
        assertTrue(phaseRoadmap.contains("SubtitleUtf8ExportPolicy"))
        assertTrue(phaseRoadmap.contains("SubtitleExportWriter"))
        assertTrue(phaseRoadmap.contains("writeCurrentSubtitleUtf8ExportTo"))
        assertTrue(phaseRoadmap.contains("currentSubtitleInfo"))
        assertTrue(phaseRoadmap.contains("writeCurrentSubtitleDelayCorrectionExportTo"))
        assertTrue(phaseRoadmap.contains("SubtitleCacheCopyPolicy"))
        assertTrue(phaseRoadmap.contains("lineCount"))
        assertTrue(phaseRoadmap.contains("time range"))
        assertTrue(phaseRoadmap.contains("styledLineCount"))
        assertTrue(phaseRoadmap.contains("suggestedCopyName"))
        assertTrue(phaseRoadmap.contains("overwritesOriginal = false"))
        assertTrue(phaseRoadmap.contains("UI"))
        assertTrue(phaseRoadmap.contains("UTF-8"))

        assertTrue(masterRoadmap.contains("Phase 4 / P4-TOOLS-001"))
        assertTrue(masterRoadmap.contains("SubtitleInfoPolicy"))
        assertTrue(masterRoadmap.contains("SubtitleDelayCorrectionPolicy"))
        assertTrue(masterRoadmap.contains("SubtitleUtf8ExportPolicy"))
        assertTrue(masterRoadmap.contains("SubtitleExportWriter"))
        assertTrue(masterRoadmap.contains("UTF-8 SAF 导出入口"))
        assertTrue(masterRoadmap.contains("字幕信息摘要 UI"))
        assertTrue(masterRoadmap.contains("延迟校正 SAF 导出入口"))
        assertTrue(masterRoadmap.contains("字幕缓存副本管理策略"))
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

    @Test
    fun subtitleToolboxDelayCorrectionPlansCopyWithoutFilesystemWrites() {
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
            "SubtitleDelayCorrectionPolicy.kt"
        )

        assertTrue(policy.contains("object SubtitleDelayCorrectionPolicy"))
        assertTrue(policy.contains("data class SubtitleDelayCorrectionPlan"))
        assertTrue(policy.contains("overwritesOriginal: Boolean = false"))
        assertTrue(policy.contains("suggestedCopyName"))
        assertTrue(policy.contains("coerceAtLeast(0L)"))
        assertTrue(!policy.contains("writeText("))
        assertTrue(!policy.contains("outputStream("))
    }

    @Test
    fun subtitleToolboxUtf8ExportPlansSrtCopyWithoutFilesystemWrites() {
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
            "SubtitleUtf8ExportPolicy.kt"
        )

        assertTrue(policy.contains("object SubtitleUtf8ExportPolicy"))
        assertTrue(policy.contains("data class SubtitleUtf8ExportPlan"))
        assertTrue(policy.contains("Charsets.UTF_8"))
        assertTrue(policy.contains("suggestedCopyName"))
        assertTrue(policy.contains("overwritesOriginal: Boolean = false"))
        assertTrue(!policy.contains("writeText("))
        assertTrue(!policy.contains("outputStream("))
    }

    @Test
    fun subtitleToolboxExportWriterOnlyUsesProvidedOutputStream() {
        val writer = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "subtitle",
            "SubtitleExportWriter.kt"
        )

        assertTrue(writer.contains("object SubtitleExportWriter"))
        assertTrue(writer.contains("fun writePlanToOutputStream"))
        assertTrue(writer.contains("OutputStream"))
        assertTrue(writer.contains("plan.bytes"))
        assertTrue(writer.contains("WRITE_FAILED"))
        assertTrue(!writer.contains("ContentResolver"))
        assertTrue(!writer.contains("Uri"))
        assertTrue(!writer.contains("FileOutputStream"))
        assertTrue(!writer.contains("openOutputStream"))
        assertTrue(!writer.contains("writeText("))
    }

    @Test
    fun subtitleToolboxCacheCopyPolicyPlansCacheTargetsWithoutFilesystemWrites() {
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
            "SubtitleCacheCopyPolicy.kt"
        )

        assertTrue(policy.contains("object SubtitleCacheCopyPolicy"))
        assertTrue(policy.contains("data class SubtitleCacheTarget"))
        assertTrue(policy.contains("data class SubtitleCacheRetentionPlan"))
        assertTrue(policy.contains("const val DIRECTORY_NAME: String = \"subtitles\""))
        assertTrue(policy.contains("DEFAULT_MAX_COPIES"))
        assertTrue(policy.contains("planRetention("))
        assertTrue(policy.contains("overwritesOriginal: Boolean = false"))
        assertTrue(!policy.contains("File("))
        assertTrue(!policy.contains("deleteRecursively"))
        assertTrue(!policy.contains("writeBytes"))
        assertTrue(!policy.contains("outputStream("))
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
