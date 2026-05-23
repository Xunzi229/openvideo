package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSettingsActivityIntegrationTest {

    @Test
    fun subtitleSettingsSheetHidesControlsWhileOpenAndRestoresOnDismiss() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val openBlock = source
            .substringAfter("private fun openSubtitleSettingsSheet()")
            .substringBefore("\n    private fun onSubtitleSettingsSheetDismissed()")
        val dismissBlock = source
            .substringAfter("private fun onSubtitleSettingsSheetDismissed()")
            .substringBefore("\n    private fun dismissSubtitleSettingsSheet()")

        assertTrue(openBlock.contains("hideChromeForSettingsOverlay()"))
        assertTrue(openBlock.contains("isSettingsOverlayVisible = true"))
        assertTrue(openBlock.contains("onDismissListener = ::onSubtitleSettingsSheetDismissed"))
        assertTrue(dismissBlock.contains("restoreChromeAfterSettingsOverlay()"))
        assertTrue(dismissBlock.contains("isSettingsOverlayVisible = false"))
        assertTrue(dismissBlock.contains("applyPlayerSettings()"))
        assertTrue(dismissBlock.contains("scheduleHideControls()"))
    }

    @Test
    fun settingsSheetWeakensControlsAndImmediatePreferencesAffectPlaybackChrome() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(source.contains("openPlayerSettingsDialog"))
        assertTrue(source.contains("hideChromeForSettingsOverlay()"))
        assertTrue(source.contains("restoreChromeAfterSettingsOverlay()"))
        assertTrue(source.contains("isSettingsOverlayVisible = true"))
        assertTrue(source.contains("isSettingsOverlayVisible = false"))
        assertTrue(source.contains("PlayerSubtitlePresentationPolicy.resolveSubtitleText("))
        assertTrue(source.contains("PlayerDisplayVisibilityPolicy.videoLayerAlpha(playerPrefs.videoDisplayEnabled)"))
        assertTrue(source.contains("controlsChromeMaxAlpha()"))
        assertTrue(source.contains("controlsContainer.alpha = controlsChromeMaxAlpha()"))
        assertTrue(source.contains("playerManager.applyVideoAdjustments("))
    }

    @Test
    fun brightnessPreferenceAppliesOnlyScreenBrightnessWithoutRebuildingVideoEffects() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val prefsListener = source
            .substringAfter("prefsListener = SharedPreferences.OnSharedPreferenceChangeListener")
            .substringBefore("settingsPrefs.registerOnSharedPreferenceChangeListener")
        val applyPlayerSettings = source
            .substringAfter("private fun applyPlayerSettings() {")
            .substringBefore("\n    private fun initViews()")

        assertTrue(prefsListener.contains("key == PlayerPrefs.KEY_BRIGHTNESS_ADJUSTMENT"))
        assertTrue(prefsListener.contains("applyScreenBrightness(playerPrefs.brightnessAdjustment)"))
        assertFalse(
            "Brightness changes should not run the full player setting apply path because it reconfigures Media3 video effects.",
            prefsListener.contains("PlayerPrefs.requiresImmediatePlayerApply(key)")
                && PlayerPrefsBrightnessKeySource.containsBrightnessImmediateApply(source)
        )
        assertTrue(applyPlayerSettings.contains("playerManager.applyVideoAdjustments("))
        assertTrue(applyPlayerSettings.contains("0f,"))
        assertTrue(applyPlayerSettings.contains("applyScreenBrightness(playerPrefs.brightnessAdjustment)"))
    }

    @Test
    fun displayEffectPreferencesAreAppliedByDialogWithoutSharedPrefsDuplicateApply() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        listOf("KEY_CONTRAST_ADJUSTMENT", "KEY_SATURATION_ADJUSTMENT").forEach { key ->
            assertFalse(
                "$key should not be in requiresImmediatePlayerApply because the dialog applies video effects on SeekBar release.",
                PlayerPrefsBrightnessKeySource.containsImmediateApplyKey(source, key)
            )
        }
    }

    private fun playerActivitySource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerActivity.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private object PlayerPrefsBrightnessKeySource {
        fun containsBrightnessImmediateApply(activitySource: String): Boolean {
            return activitySource.contains("PlayerPrefs.requiresImmediatePlayerApply(key)") &&
                containsImmediateApplyKey(activitySource, "KEY_BRIGHTNESS_ADJUSTMENT")
        }

        fun containsImmediateApplyKey(activitySource: String, key: String): Boolean {
            val prefsSource = String(Files.readAllBytes(playerPrefsSource()))
            val immediateBlock = prefsSource
                .substringAfter("fun requiresImmediatePlayerApply(key: String?): Boolean")
                .substringBefore("\n        }\n    }")
            return activitySource.contains("PlayerPrefs.requiresImmediatePlayerApply(key)") &&
                immediateBlock.contains(key)
        }

        private fun playerPrefsSource(): Path {
            val relativePath = Paths.get(
                "src",
                "main",
                "java",
                "com",
                "example",
                "openvideo",
                "core",
                "prefs",
                "PlayerPrefs.kt"
            )
            return sequenceOf(
                relativePath,
                Paths.get("app").resolve(relativePath)
            ).first(Files::exists)
        }
    }
}
