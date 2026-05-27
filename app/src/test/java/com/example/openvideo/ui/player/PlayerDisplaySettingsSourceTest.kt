package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerDisplaySettingsSourceTest {

    @Test
    fun applyPlayerSettingsDelegatesDisplayOnlyStateToDedicatedHelper() {
        val activitySource = String(Files.readAllBytes(playerActivitySource()))
        val source = String(Files.readAllBytes(playerDisplayControllerSource()))
        val applySettingsBlock = source.substringAfter("fun applyPlayerSettings() {")
            .substringBefore("\n    fun applyDisplaySettings()")
        val displayBlock = source.substringAfter("fun applyDisplaySettings() {")
            .substringBefore("\n    fun initBrightnessAndVolume()")

        assertTrue(activitySource.contains("private fun applyPlayerSettings() = playerDisplay.applyPlayerSettings()"))
        assertTrue(applySettingsBlock.contains("applyDisplaySettings()"))
        assertTrue(displayBlock.contains("setPlayerResizeMode()"))
        assertTrue(displayBlock.contains("onApplyContentAspectRatio()"))
        assertTrue(displayBlock.contains("onApplyContentFrameTransform()"))
        assertTrue(displayBlock.contains("playerView.rotation = playerPrefs.rotation.toFloat()"))
        // mirror is routed through PlayerDisplayAdjustment so future mirror policy changes
        // (e.g. clamp / animate) only need updating the helper, not the Activity.
        assertTrue(
            displayBlock.contains(
                "playerView.scaleX = PlayerDisplayAdjustment.mirrorScaleX(playerPrefs.mirror)"
            )
        )
    }

    private fun playerActivitySource(): Path {
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerDisplayControllerSource(): Path {
        return kotlinSource("PlayerDisplayController.kt")
    }

    private fun kotlinSource(name: String): Path {
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
}
