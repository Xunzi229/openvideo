package com.openvideo.app.core.prefs

import android.app.Application
import android.content.Context
import com.openvideo.app.core.subtitle.SubtitleLanguage
import com.openvideo.app.core.subtitle.SubtitleLanguagePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class PlayerPrefsTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun readsDefaultsAndResetClearsPersistedValuesImmediately() {
        val prefs = PlayerPrefs(context)
        val defaults = SettingsBackupExporter.playerSectionFrom(prefs)
        assertEquals(1f, prefs.speed)
        assertEquals(LoopMode.LIST, prefs.loopMode)
        assertEquals(60, prefs.settingsPanelOpacity)
        assertTrue(prefs.rememberProgress)
        assertFalse(prefs.bgAudio)
        assertEquals(SubtitleLanguagePreference(), prefs.subtitleLanguagePreference())
        assertTrue(prefs.seekThumbnailEnabled)
        prefs.speed = 2f
        prefs.seekThumbnailEnabled = false
        prefs.externalSubtitleUri = "content://temporary"
        assertFalse(PlayerPrefs(context).seekThumbnailEnabled)
        prefs.resetToDefaults()
        assertEquals(defaults, SettingsBackupExporter.playerSectionFrom(PlayerPrefs(context)))
        assertEquals("", prefs.externalSubtitleUri)
        assertTrue(prefs.seekThumbnailEnabled)
    }

    @Test
    fun migratesLegacyOpacityKeysWithDefinedPrecedence() {
        val store = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        val prefs = PlayerPrefs(context)
        store.edit().putInt("settings_sheet_opacity", 35).commit()
        assertEquals(35, prefs.settingsPanelOpacity)
        store.edit().putInt("settings_sheet_transparency", 20).commit()
        assertEquals(80, prefs.settingsPanelOpacity)
        store.edit().putInt("settings_panel_opacity", 45).commit()
        assertEquals(45, prefs.settingsPanelOpacity)
        prefs.settingsPanelOpacity = 70
        assertEquals(70, PlayerPrefs(context).settingsPanelOpacity)
        assertFalse(store.contains("settings_sheet_opacity"))
        assertFalse(store.contains("settings_sheet_transparency"))
    }

    @Test
    fun clampsPanelAndBackdropValuesAtBothBounds() {
        val prefs = PlayerPrefs(context)
        for ((input, percentage) in listOf(-1 to 0, 0 to 0, 50 to 50, 100 to 100, 101 to 100)) {
            prefs.settingsPanelOpacity = input
            prefs.settingsSheetBackdropDimPercent = input
            assertEquals(percentage, prefs.settingsPanelOpacity)
            assertEquals(percentage, prefs.settingsSheetBackdropDimPercent)
        }
        for ((input, expected) in listOf(-1 to 0, 0 to 0, 32 to 32, 64 to 64, 65 to 64)) {
            prefs.settingsSheetBackdropBlurDp = input
            assertEquals(expected, prefs.settingsSheetBackdropBlurDp)
        }
    }

    @Test
    fun secondarySubtitleInheritsPrimaryUntilExplicitlyConfigured() {
        val prefs = PlayerPrefs(context)
        prefs.subtitleSize = 25
        prefs.subtitleColor = 0xFF123456.toInt()
        prefs.subtitleBgStyle = SubtitleBgStyle.OPAQUE
        prefs.subtitlePosition = 0.4f
        assertEquals(25, prefs.secondarySubtitleSize)
        assertEquals(0xFF123456.toInt(), prefs.secondarySubtitleColor)
        assertEquals(SubtitleBgStyle.OPAQUE, prefs.secondarySubtitleBgStyle)
        assertEquals(0.4f, prefs.secondarySubtitlePosition)
        prefs.secondarySubtitleSize = 16
        prefs.secondarySubtitleColor = 0xFFABCDEF.toInt()
        prefs.secondarySubtitleBgStyle = SubtitleBgStyle.NONE
        prefs.secondarySubtitlePosition = 0.7f
        assertEquals(16, prefs.secondarySubtitleSize)
        assertEquals(0xFFABCDEF.toInt(), prefs.secondarySubtitleColor)
        assertEquals(SubtitleBgStyle.NONE, prefs.secondarySubtitleBgStyle)
        assertEquals(0.7f, prefs.secondarySubtitlePosition)
        assertEquals(25, prefs.subtitleSize)
    }

    @Test
    fun restoresSubtitleLanguagesFromPersistedKeys() {
        val prefs = PlayerPrefs(context)
        prefs.subtitlePrimaryLanguage = "chinese"
        prefs.subtitleSecondaryLanguage = "english"
        prefs.subtitlePreferBilingual = true
        assertEquals(
            SubtitleLanguagePreference(SubtitleLanguage.CHINESE, SubtitleLanguage.ENGLISH, true),
            PlayerPrefs(context).subtitleLanguagePreference()
        )
    }

    @Test
    fun appViewModesInheritLegacyModeUntilIndividuallySet() {
        val prefs = AppPrefs(context)
        assertEquals("list", prefs.viewMode)
        prefs.viewMode = "grid"
        assertEquals("grid", prefs.homeAllViewMode)
        assertEquals("grid", prefs.homeRecentViewMode)
        assertEquals("grid", prefs.homeFavoriteViewMode)
        prefs.homeAllViewMode = "list"
        assertEquals("list", AppPrefs(context).homeAllViewMode)
        assertEquals("grid", prefs.homeRecentViewMode)
    }

    @Test
    fun normalizesLegacyLanguageBeforePersisting() {
        val prefs = AppPrefs(context)
        for ((input, expected) in listOf("zh" to "zh", "zh-CN" to "zh", "zh-rCN" to "zh", "zh_CN" to "zh",
            "en" to "en", "en-US" to "en", "en-rUS" to "en", "en_GB" to "en", "system" to "system", "invalid" to "system")) {
            prefs.language = input
            assertEquals(expected, AppPrefs(context).language)
            assertEquals(expected, context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getString("language", null))
        }
    }
}
