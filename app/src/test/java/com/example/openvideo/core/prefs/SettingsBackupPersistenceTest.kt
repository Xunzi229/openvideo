package com.example.openvideo.core.prefs

import android.app.Application
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class SettingsBackupPersistenceTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val instant = Instant.parse("2026-09-06T00:00:00Z")

    @Test
    fun defaultExportTimestampProducesAValidDocumentAndJson() {
        val player = PlayerPrefs(context)
        val app = AppPrefs(context)
        val document = SettingsBackupExporter.exportDocument(player, app)
        assertEquals(SettingsBackupSchema.SCHEMA_VERSION, document.schemaVersion)
        assertTrue(SettingsBackupSchema.isValidExportedAt(document.exportedAt))
        val decoded = SettingsBackupSchema.decode(SettingsBackupExporter.exportJson(player, app))
            as SettingsBackupSchema.ParseResult.Success
        assertEquals(document.player, decoded.document.player)
        assertEquals(document.app, decoded.document.app)
    }

    @Test
    fun exportsAndImportsEverySupportedSettingAcrossNewPreferenceInstances() {
        val expected = completeDocument()
        val player = PlayerPrefs(context)
        val app = AppPrefs(context)
        SettingsBackupImporter.apply(expected, player, app)
        val json = SettingsBackupExporter.exportJson(PlayerPrefs(context), AppPrefs(context), instant)
        val decoded = SettingsBackupSchema.decode(json) as SettingsBackupSchema.ParseResult.Success
        assertEquals(expected, decoded.document)

        player.resetToDefaults()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        SettingsBackupImporter.apply(decoded.document, PlayerPrefs(context), AppPrefs(context))
        assertEquals(expected, SettingsBackupExporter.exportDocument(PlayerPrefs(context), AppPrefs(context), instant))
    }

    @Test
    fun emptySectionsDoNotOverwriteAnyExistingSetting() {
        val player = PlayerPrefs(context)
        val app = AppPrefs(context)
        val expected = completeDocument()
        SettingsBackupImporter.apply(expected, player, app)
        SettingsBackupImporter.apply(SettingsBackupSchema.newDocument(instant), player, app)
        assertEquals(expected, SettingsBackupExporter.exportDocument(player, app, instant))
    }

    @Test
    fun falseAndZeroValuesOverwritePreviouslyEnabledSettings() {
        val player = PlayerPrefs(context)
        val app = AppPrefs(context)
        player.bgAudio = true
        player.audioMuted = true
        player.subtitleDelayMs = 200
        app.sortAsc = true
        SettingsBackupImporter.apply(
            SettingsBackupSchema.Document(1, instant.toString(),
                player = SettingsBackupSchema.PlayerSection(bgAudio = false, audioMuted = false, subtitleDelayMs = 0),
                app = SettingsBackupSchema.AppSection(sortAsc = false)),
            player, app
        )
        assertFalse(player.bgAudio)
        assertFalse(player.audioMuted)
        assertEquals(0, player.subtitleDelayMs)
        assertFalse(app.sortAsc)
    }

    @Test
    fun exportOmitsSensitiveSessionValuesAndImportPreservesThem() {
        val player = PlayerPrefs(context)
        val app = AppPrefs(context)
        player.externalSubtitleUri = "content://private/subtitle"
        player.lastStreamUrl = "https://example.com/private-stream"
        player.clipStartMs = 123
        player.clipEndMs = 456
        player.clipLoopPreview = true
        player.bookmarkPositionMs = 321
        app.pinnedFolderKeys = setOf("/private/folder")
        app.lastGitHubReleaseCheckMs = 789
        app.githubUpdateBadgeVisible = true
        app.githubPendingDownloadUrl = "https://example.com/pending"

        val json = SettingsBackupExporter.exportJson(player, app, instant)
        SettingsBackupAllowlistPolicy.assertExportContainsNoSensitiveKeys(json)
        assertFalse(json.contains("content://private"))
        assertFalse(json.contains("https://example.com"))
        assertFalse(json.contains("/private/folder"))
        SettingsBackupImporter.apply(completeDocument(), player, app)
        assertEquals("content://private/subtitle", player.externalSubtitleUri)
        assertEquals("https://example.com/private-stream", player.lastStreamUrl)
        assertEquals(123L, player.clipStartMs)
        assertEquals(456L, player.clipEndMs)
        assertTrue(player.clipLoopPreview)
        assertEquals(321L, player.bookmarkPositionMs)
        assertEquals(setOf("/private/folder"), app.pinnedFolderKeys)
        assertEquals(789L, app.lastGitHubReleaseCheckMs)
        assertTrue(app.githubUpdateBadgeVisible)
        assertEquals("https://example.com/pending", app.githubPendingDownloadUrl)
    }

    private fun completeDocument() = SettingsBackupSchema.Document(
        schemaVersion = 1,
        exportedAt = instant.toString(),
        player = SettingsBackupSchema.PlayerSection(
            speed = 1.75f, loopMode = "single", seekInterval = 20, rememberProgress = false,
            autoPlayNext = false, playbackEndBehavior = "return", hwAcceleration = false,
            pauseOnExit = true, bgAudio = true, bgPlaybackNotificationEnabled = false,
            skipIntroOutro = true, introSeconds = 12, outroSeconds = 34, keepScreenOn = false,
            controlsAutoHide = 7, aspectRatio = "crop", contentFrameMode = "center_4_3",
            rotation = 90, mirror = true, autoOrientationByVideo = false, videoDisplayEnabled = false,
            brightnessAdjustment = 11, contrastAdjustment = 22, saturationAdjustment = 33,
            progressStyle = "thin", controlsOpacity = 75, settingsPanelOpacity = 45,
            settingsSheetBackdropDimPercent = 35, settingsSheetBackdropBlurDp = 12,
            speedPreservePitch = false, volumeBoost = true, audioChannel = "right", audioDelay = -200,
            audioMuted = true, softwareAudioDecoder = true, audioSyncEnabled = false,
            subtitleSize = 24, subtitleColor = 0xFFFF0000.toInt(), subtitleBgStyle = "none", subtitlePosition = 0.8f,
            secondarySubtitleSize = 16, secondarySubtitleColor = 0xFF00FF00.toInt(),
            secondarySubtitleBgStyle = "opaque", secondarySubtitlePosition = 0.2f,
            subtitleEncoding = "UTF-8", subtitleDelayMs = -300, subtitlesEnabled = false,
            leftVerticalGesture = "none", rightVerticalGesture = "seek", doubleTapAction = "forward",
            longPressAction = "none", horizontalSwipeAction = "volume", gestureSensitivity = 3,
            doubleTapSeconds = 30, longPressSpeed = 3f, swipeRange = 80, edgeSwipeBack = true,
            keyboardShortcuts = false
        ),
        app = SettingsBackupSchema.AppSection(
            themeMode = "light", language = "en", defaultAspectRatio = "16_9", defaultSpeed = 1.5f,
            brightness = 0.6f, viewMode = "grid", homeAllViewMode = "list", homeRecentViewMode = "grid",
            homeFavoriteViewMode = "list", sortField = "name", sortAsc = true
        )
    )
}
