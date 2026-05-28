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
        val source = String(Files.readAllBytes(playerQuickDialogControllerSource()))
        val openBlock = source
            .substringAfter("fun openSubtitleSettingsSheet()")
            .substringBefore("\n    fun onSubtitleSettingsSheetDismissed()")
        val dismissBlock = source
            .substringAfter("fun onSubtitleSettingsSheetDismissed()")
            .substringBefore("\n    fun dismissSubtitleSettingsSheet()")

        assertTrue(openBlock.contains("onHideChromeForSettingsOverlay()"))
        assertTrue(openBlock.contains("onSettingsOverlayVisibleChanged(true)"))
        assertTrue(openBlock.contains("onDismissListener = ::onSubtitleSettingsSheetDismissed"))
        assertTrue(dismissBlock.contains("onRestoreChromeAfterSettingsOverlay()"))
        assertTrue(dismissBlock.contains("onSettingsOverlayVisibleChanged(false)"))
        assertTrue(dismissBlock.contains("onApplyPlayerSettings()"))
        assertTrue(dismissBlock.contains("onScheduleHideControls()"))
    }

    @Test
    fun settingsSheetWeakensControlsAndImmediatePreferencesAffectPlaybackChrome() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val displayController = String(Files.readAllBytes(playerDisplayControllerSource()))
        val tickController = String(Files.readAllBytes(playerPlaybackTickControllerSource()))
        val quickDialogs = String(Files.readAllBytes(playerQuickDialogControllerSource()))

        assertTrue(source.contains("openPlayerSettingsDialog"))
        assertTrue(quickDialogs.contains("onHideChromeForSettingsOverlay()"))
        assertTrue(quickDialogs.contains("onRestoreChromeAfterSettingsOverlay()"))
        assertTrue(quickDialogs.contains("onSettingsOverlayVisibleChanged(true)"))
        assertTrue(quickDialogs.contains("onSettingsOverlayVisibleChanged(false)"))
        assertTrue(tickController.contains("PlayerSubtitlePresentationPolicy.resolveSubtitleText("))
        assertTrue(displayController.contains("PlayerDisplayVisibilityPolicy.videoLayerAlpha(playerPrefs.videoDisplayEnabled)"))
        assertTrue(source.contains("controlsChromeMaxAlpha()"))
        assertTrue(displayController.contains("controlsContainer.alpha = controlsChromeMaxAlpha()"))
        assertTrue(displayController.contains("playerManager.applyVideoAdjustments("))
    }

    @Test
    fun brightnessPreferenceAppliesOnlyScreenBrightnessWithoutRebuildingVideoEffects() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val displayController = String(Files.readAllBytes(playerDisplayControllerSource()))
        val subtitleController = String(Files.readAllBytes(playerSubtitleControllerSource()))
        val prefsListener = subtitleController
            .substringAfter("prefsListener = SharedPreferences.OnSharedPreferenceChangeListener")
            .substringBefore("settingsPrefs.registerOnSharedPreferenceChangeListener")
        val applyPlayerSettings = displayController
            .substringAfter("fun applyPlayerSettings() {")
            .substringBefore("\n    fun applyDisplaySettings()")

        assertTrue(prefsListener.contains("key == PlayerPrefs.KEY_BRIGHTNESS_ADJUSTMENT"))
        assertTrue(prefsListener.contains("onApplyScreenBrightness(playerPrefs.brightnessAdjustment)"))
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

    private fun playerQuickDialogControllerSource(): Path {
        return playerSource("PlayerQuickDialogController.kt")
    }

    private fun playerDisplayControllerSource(): Path {
        return playerSource("PlayerDisplayController.kt")
    }

    private fun playerSubtitleControllerSource(): Path {
        return playerSource("PlayerSubtitleController.kt")
    }

    private fun playerPlaybackTickControllerSource(): Path {
        return playerSource("PlayerPlaybackTickController.kt")
    }

    private fun playerSource(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            name
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
