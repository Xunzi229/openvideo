package com.example.openvideo.core.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class SettingsBackupFileWriterTest {

    @Test
    fun writeJsonToOutputStreamPreservesUtf8Payload() {
        val json = """{"schemaVersion":1,"exportedAt":"2026-05-19T00:00:00Z"}"""
        val out = ByteArrayOutputStream()
        SettingsBackupFileWriter.writeJsonToOutputStream(out, json)
        assertEquals(json, out.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun exportedJsonRoundTripsThroughSchemaDecode() {
        val json = SettingsBackupSchema.encode(
            SettingsBackupSchema.Document(
                schemaVersion = SettingsBackupSchema.SCHEMA_VERSION,
                exportedAt = "2026-05-19T08:00:00Z",
                player = SettingsBackupSchema.PlayerSection(
                    speed = 1.25f,
                    bgAudio = true
                ),
                app = SettingsBackupSchema.AppSection(
                    themeMode = "dark",
                    language = "zh"
                )
            )
        )
        val out = ByteArrayOutputStream()
        SettingsBackupFileWriter.writeJsonToOutputStream(out, json)
        val result = SettingsBackupSchema.decode(out.toString(Charsets.UTF_8.name()))
        assertTrue(result is SettingsBackupSchema.ParseResult.Success)
        val document = (result as SettingsBackupSchema.ParseResult.Success).document
        assertEquals(SettingsBackupSchema.SCHEMA_VERSION, document.schemaVersion)
        assertEquals("dark", document.app.themeMode)
        assertEquals(1.25f, document.player.speed)
    }
}
