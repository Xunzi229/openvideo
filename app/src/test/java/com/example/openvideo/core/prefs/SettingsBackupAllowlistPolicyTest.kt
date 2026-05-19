package com.example.openvideo.core.prefs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupAllowlistPolicyTest {

    @Test
    fun rejectsExcludedPlayerPrefKeys() {
        assertFalse(SettingsBackupAllowlistPolicy.isExportablePlayerPrefKey("last_stream_url"))
        assertFalse(SettingsBackupAllowlistPolicy.isExportablePlayerPrefKey(PlayerPrefs.KEY_EXTERNAL_SUBTITLE))
        assertTrue(SettingsBackupAllowlistPolicy.isExportablePlayerPrefKey("speed"))
        assertTrue(SettingsBackupAllowlistPolicy.isExportablePlayerPrefKey("loop_mode"))
    }

    @Test
    fun rejectsExcludedAppPrefKeys() {
        assertFalse(SettingsBackupAllowlistPolicy.isExportableAppPrefKey("pinned_folder_keys"))
        assertFalse(SettingsBackupAllowlistPolicy.isExportableAppPrefKey("github_pending_download_url"))
        assertTrue(SettingsBackupAllowlistPolicy.isExportableAppPrefKey("theme_mode"))
    }

    @Test
    fun findsLeakedSensitiveMarkersInJson() {
        val safe = SettingsBackupSchema.encode(
            SettingsBackupSchema.Document(
                schemaVersion = 1,
                exportedAt = "2026-05-19T00:00:00Z",
                player = SettingsBackupSchema.PlayerSection(speed = 1.5f),
                app = SettingsBackupSchema.AppSection(language = "zh")
            )
        )
        assertTrue(SettingsBackupAllowlistPolicy.findLeakedSensitiveMarkers(safe).isEmpty())

        val leaked = """{"player":{"last_stream_url":"http://x?token=abc"}}"""
        assertTrue(SettingsBackupAllowlistPolicy.findLeakedSensitiveMarkers(leaked).isNotEmpty())
    }

    @Test
    fun assertExportGuardThrowsWhenSensitiveMarkerPresent() {
        var threw = false
        try {
            SettingsBackupAllowlistPolicy.assertExportContainsNoSensitiveKeys(
                """{"external_subtitle_uri":"/storage/emulated/0/a.srt"}"""
            )
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun exportableJsonKeySetsMatchSchemaSections() {
        assertTrue(SettingsBackupAllowlistPolicy.EXPORTABLE_PLAYER_JSON_KEYS.contains("loopMode"))
        assertFalse(SettingsBackupAllowlistPolicy.EXPORTABLE_PLAYER_JSON_KEYS.contains("externalSubtitleUri"))
        assertTrue(SettingsBackupAllowlistPolicy.EXPORTABLE_APP_JSON_KEYS.contains("language"))
        assertFalse(SettingsBackupAllowlistPolicy.EXPORTABLE_APP_JSON_KEYS.contains("pinnedFolderKeys"))
    }
}
