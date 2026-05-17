package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerDisplaySettingsSourceTest {

    @Test
    fun applyPlayerSettingsDelegatesDisplayOnlyStateToDedicatedHelper() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val applySettingsBlock = source.substringAfter("private fun applyPlayerSettings() {")
            .substringBefore("\n    private fun initBrightnessAndVolume()")
        val displayBlock = source.substringAfter("private fun applyDisplaySettings() {")
            .substringBefore("\n    private fun initBrightnessAndVolume()")

        assertTrue(applySettingsBlock.contains("applyDisplaySettings()"))
        assertTrue(displayBlock.contains("setPlayerResizeMode()"))
        assertTrue(displayBlock.contains("applyPlayerContentAspectRatio()"))
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
}
