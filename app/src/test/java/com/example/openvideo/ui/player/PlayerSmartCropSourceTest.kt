package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSmartCropSourceTest {

    @Test
    fun landscapeControlsExposeSmartCropQuickButton() {
        val source = String(Files.readAllBytes(landscapeControlsSource()))

        assertTrue(source.contains("@+id/btn_land_smart_crop"))
        assertTrue(source.contains("@string/player_action_smart_crop"))
        assertTrue(source.contains("@drawable/ic_cut"))
    }

    @Test
    fun playerActivityRoutesSmartCropButtonToHandler() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter(
            "findViewById<View>(R.id.btn_land_smart_crop)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {"
        ).substringBefore("\n        }\n\n        findViewById<View>(R.id.btn_land_aspect)")

        assertTrue(block.contains("handleSmartCropQuickToggle()"))
    }

    @Test
    fun smartCropHandlerUsesPolicyAndAppliesDisplaySettings() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("private fun handleSmartCropQuickToggle() {")
            .substringBefore("\n    private fun showAspectRatioQuickDialog()")

        assertTrue(block.contains("PlayerSmartCropPolicy.quickToggleDecision("))
        assertTrue(block.contains("playerPrefs.contentFrameMode = decision.contentFrameMode"))
        assertTrue(block.contains("decision.aspectRatioOverride?.let"))
        assertTrue(block.contains("applyDisplaySettings()"))
    }

    private fun landscapeControlsSource(): Path =
        resource("layout-land", "player_controls.xml")

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
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun resource(dir: String, file: String): Path {
        val relativePath = Paths.get("src", "main", "res", dir, file)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
