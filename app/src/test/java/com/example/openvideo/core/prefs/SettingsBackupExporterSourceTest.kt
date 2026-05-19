package com.example.openvideo.core.prefs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SettingsBackupExporterSourceTest {

    @Test
    fun exporterMapsPrefsAndRunsAllowlistGuard() {
        val source = exporterSource()
        assertTrue(source.contains("fun exportJson("))
        assertTrue(source.contains("playerSectionFrom(playerPrefs)"))
        assertTrue(source.contains("appSectionFrom(appPrefs)"))
        assertTrue(source.contains("SettingsBackupAllowlistPolicy.assertExportContainsNoSensitiveKeys"))
        assertTrue(source.contains("playerPrefs.loopMode.key"))
        assertTrue(source.contains("appPrefs.themeMode.key"))
        assertFalse(source.contains("externalSubtitleUri"))
        assertFalse(source.contains("lastStreamUrl"))
        assertFalse(source.contains("pinnedFolderKeys"))
        assertFalse(source.contains("githubPendingDownloadUrl"))
    }

    private fun exporterSource(): String = loadSource(
        Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "prefs",
            "SettingsBackupExporter.kt"
        )
    )

    private fun loadSource(relativePath: Path): String {
        val path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
