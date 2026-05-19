package com.example.openvideo.core.prefs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SettingsBackupSchemaSourceTest {

    @Test
    fun schemaDefinesVersionSectionsAndCodec() {
        val source = schemaSource()
        assertTrue(source.contains("const val SCHEMA_VERSION = 1"))
        assertTrue(source.contains("data class Document("))
        assertTrue(source.contains("data class PlayerSection("))
        assertTrue(source.contains("data class AppSection("))
        assertTrue(source.contains("fun encode(document: Document): String"))
        assertTrue(source.contains("fun decode(json: String): ParseResult"))
        assertTrue(source.contains("SettingsBackupJson.stringify"))
        assertTrue(source.contains("EXCLUDED_PLAYER_PREF_KEYS"))
        assertTrue(source.contains("EXCLUDED_APP_PREF_KEYS"))
    }

    @Test
    fun adbRegressionScriptIsPresentAndChecksMediaSession() {
        val script = adbScriptSource()
        assertTrue(script.contains("OpenVideoSession"))
        assertTrue(script.contains("PlaybackService"))
        assertTrue(script.contains("cmd media_session dispatch play-pause"))
        assertTrue(script.contains("bg_audio"))
    }

    private fun schemaSource(): String = loadSource(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "core", "prefs", "SettingsBackupSchema.kt")
    )

    private fun adbScriptSource(): String = loadRootFile(
        Paths.get("tools", "adb", "phase0-media-session-regression.ps1")
    )

    private fun loadSource(relativePath: Path): String {
        val path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }

    private fun loadRootFile(relativePath: Path): String {
        val path = sequenceOf(
            relativePath,
            Paths.get("..").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
