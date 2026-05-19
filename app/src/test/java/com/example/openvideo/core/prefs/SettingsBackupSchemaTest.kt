package com.example.openvideo.core.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SettingsBackupSchemaTest {

    @Test
    fun newDocumentUsesCurrentSchemaVersionAndUtcTimestamp() {
        val instant = Instant.parse("2026-05-19T08:30:00Z")
        val document = SettingsBackupSchema.newDocument(instant)

        assertEquals(SettingsBackupSchema.SCHEMA_VERSION, document.schemaVersion)
        assertEquals("2026-05-19T08:30:00Z", document.exportedAt)
        assertTrue(document.player == SettingsBackupSchema.PlayerSection())
        assertTrue(document.app == SettingsBackupSchema.AppSection())
    }

    @Test
    fun encodeDecodeRoundTripPreservesPlayerAndAppFields() {
        val original = SettingsBackupSchema.Document(
            schemaVersion = SettingsBackupSchema.SCHEMA_VERSION,
            exportedAt = "2026-05-17T00:00:00Z",
            player = SettingsBackupSchema.PlayerSection(
                speed = 1.25f,
                loopMode = "list",
                bgAudio = true,
                aspectRatio = "fit",
                subtitlesEnabled = false,
                leftVerticalGesture = "brightness",
                longPressSpeed = 2.5f
            ),
            app = SettingsBackupSchema.AppSection(
                themeMode = "dark",
                language = "zh",
                defaultSpeed = 1.5f,
                sortField = "date",
                sortAsc = true
            )
        )

        val decoded = SettingsBackupSchema.decode(SettingsBackupSchema.encode(original))
        assertTrue(decoded is SettingsBackupSchema.ParseResult.Success)
        val document = (decoded as SettingsBackupSchema.ParseResult.Success).document

        assertEquals(original.schemaVersion, document.schemaVersion)
        assertEquals(original.exportedAt, document.exportedAt)
        assertEquals(1.25f, document.player.speed)
        assertEquals("list", document.player.loopMode)
        assertEquals(true, document.player.bgAudio)
        assertEquals("fit", document.player.aspectRatio)
        assertEquals(false, document.player.subtitlesEnabled)
        assertEquals("brightness", document.player.leftVerticalGesture)
        assertEquals(2.5f, document.player.longPressSpeed)
        assertEquals("dark", document.app.themeMode)
        assertEquals("zh", document.app.language)
        assertEquals(1.5f, document.app.defaultSpeed)
        assertEquals("date", document.app.sortField)
        assertEquals(true, document.app.sortAsc)
    }

    @Test
    fun decodeAcceptsEmptyPlayerAndAppObjects() {
        val json = """
            {
              "schemaVersion": 1,
              "exportedAt": "2026-05-17T00:00:00Z",
              "player": {},
              "app": {}
            }
        """.trimIndent()

        val result = SettingsBackupSchema.decode(json)
        assertTrue(result is SettingsBackupSchema.ParseResult.Success)
    }

    @Test
    fun decodeRejectsUnsupportedSchemaVersion() {
        val json = """
            {
              "schemaVersion": 99,
              "exportedAt": "2026-05-17T00:00:00Z",
              "player": {},
              "app": {}
            }
        """.trimIndent()

        val result = SettingsBackupSchema.decode(json)
        assertTrue(result is SettingsBackupSchema.ParseResult.Failure)
        val failure = result as SettingsBackupSchema.ParseResult.Failure
        assertEquals(SettingsBackupSchema.Reason.UNSUPPORTED_VERSION, failure.reason)
    }

    @Test
    fun decodeRejectsMalformedJson() {
        val result = SettingsBackupSchema.decode("{not-json")
        assertTrue(result is SettingsBackupSchema.ParseResult.Failure)
        assertEquals(
            SettingsBackupSchema.Reason.INVALID_JSON,
            (result as SettingsBackupSchema.ParseResult.Failure).reason
        )
    }

    @Test
    fun decodeRejectsMissingExportedAt() {
        val result = SettingsBackupSchema.decode("""{"schemaVersion":1,"player":{},"app":{}}""")
        assertTrue(result is SettingsBackupSchema.ParseResult.Failure)
        assertEquals(
            SettingsBackupSchema.Reason.MISSING_REQUIRED_FIELD,
            (result as SettingsBackupSchema.ParseResult.Failure).reason
        )
    }

    @Test
    fun decodeRejectsInvalidExportedAt() {
        val result = SettingsBackupSchema.decode(
            """{"schemaVersion":1,"exportedAt":"not-a-date","player":{},"app":{}}"""
        )
        assertTrue(result is SettingsBackupSchema.ParseResult.Failure)
        assertEquals(
            SettingsBackupSchema.Reason.INVALID_EXPORTED_AT,
            (result as SettingsBackupSchema.ParseResult.Failure).reason
        )
    }

    @Test
    fun excludedPlayerKeysCoverSensitivePathsAndSessionState() {
        val excluded = SettingsBackupSchema.EXCLUDED_PLAYER_PREF_KEYS
        assertTrue(excluded.contains(PlayerPrefs.KEY_EXTERNAL_SUBTITLE))
        assertTrue(excluded.contains("last_stream_url"))
        assertTrue(excluded.contains("bookmark_position_ms"))
        assertFalse(excluded.contains("speed"))
        assertFalse(excluded.contains("loop_mode"))
    }

    @Test
    fun excludedAppKeysCoverPinnedFoldersAndUpdateCache() {
        val excluded = SettingsBackupSchema.EXCLUDED_APP_PREF_KEYS
        assertTrue(excluded.contains("pinned_folder_keys"))
        assertTrue(excluded.contains("github_pending_download_url"))
        assertFalse(excluded.contains("theme_mode"))
        assertFalse(excluded.contains("language"))
    }

    @Test
    fun encodeOmitsNullFieldsFromSections() {
        val encoded = SettingsBackupSchema.encode(
            SettingsBackupSchema.Document(
                schemaVersion = 1,
                exportedAt = "2026-05-17T00:00:00Z",
                player = SettingsBackupSchema.PlayerSection(speed = 2f),
                app = SettingsBackupSchema.AppSection(language = "en")
            )
        )
        assertTrue(encoded.contains("\"speed\""))
        assertFalse(encoded.contains("loopMode"))
        assertTrue(encoded.contains("\"language\""))
        assertFalse(encoded.contains("themeMode"))
    }

    @Test
    fun isValidExportedAtAcceptsInstantParseAndLegacyPattern() {
        assertTrue(SettingsBackupSchema.isValidExportedAt("2026-05-17T00:00:00Z"))
        assertNotNull(runCatching { Instant.parse("2026-05-17T12:34:56.789Z") }.getOrNull())
        assertTrue(SettingsBackupSchema.isValidExportedAt("2026-05-17T12:34:56.789Z"))
    }
}
