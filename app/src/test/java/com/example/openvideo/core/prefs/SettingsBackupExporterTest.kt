package com.example.openvideo.core.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupExporterTest {

    @Test
    fun playerSectionFromMapsEnumKeysNotSensitiveUris() {
        val section = SettingsBackupSchema.PlayerSection(
            loopMode = "list",
            playbackEndBehavior = "follow",
            aspectRatio = "fit",
            leftVerticalGesture = "brightness"
        )
        val encoded = SettingsBackupSchema.encode(
            SettingsBackupSchema.Document(
                schemaVersion = 1,
                exportedAt = "2026-05-19T00:00:00Z",
                player = section,
                app = SettingsBackupSchema.AppSection()
            )
        )
        SettingsBackupAllowlistPolicy.assertExportContainsNoSensitiveKeys(encoded)
        assertTrue(encoded.contains("\"loopMode\""))
        assertFalse(encoded.contains("external_subtitle"))
        assertFalse(encoded.contains("last_stream_url"))
    }

    @Test
    fun encodedExportFromTypicalSectionsPassesAllowlist() {
        val json = SettingsBackupSchema.encode(
            SettingsBackupSchema.Document(
                schemaVersion = SettingsBackupSchema.SCHEMA_VERSION,
                exportedAt = "2026-05-19T08:00:00Z",
                player = SettingsBackupSchema.PlayerSection(
                    speed = 1.25f,
                    bgAudio = true,
                    softwareAudioDecoder = false
                ),
                app = SettingsBackupSchema.AppSection(
                    themeMode = "dark",
                    language = "zh",
                    defaultSpeed = 1f
                )
            )
        )
        SettingsBackupAllowlistPolicy.assertExportContainsNoSensitiveKeys(json)
        val decoded = SettingsBackupSchema.decode(json)
        assertTrue(decoded is SettingsBackupSchema.ParseResult.Success)
        val document = (decoded as SettingsBackupSchema.ParseResult.Success).document
        assertEquals(1.25f, document.player.speed)
        assertEquals("zh", document.app.language)
    }
}
