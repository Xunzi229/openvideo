package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerLockedControlsSourceTest {

    @Test
    fun applyControlVisibilityUsesLockedControlsPolicy() {
        val source = playerChromeControllerSource()
        val block = source.substringAfter("fun applyControlVisibility() {")
            .substringBefore("\n    fun hideChromeForSettingsOverlay()")

        assertTrue(block.contains("PlayerLockedControlsPolicy.visibility(isScreenLocked, controlsVisible)"))
        assertTrue(block.contains("PlayerLockedControlsPolicy.isChromeRegionVisible("))
        assertTrue(block.contains("PlayerChromeRegion.TOP_SCRIM"))
        assertTrue(block.contains("PlayerChromeRegion.LAND_RIGHT_FLOAT_COLUMN"))
        assertTrue(block.contains("visibility.fullscreenButtonVisible"))
        assertTrue(block.contains("fullscreenButtonProvider().visibility"))
        assertFalse(
            "Chrome visibility must not bypass PlayerLockedControlsPolicy.",
            block.contains("PlayerControlState.visibilityFor(isScreenLocked")
        )
    }

    @Test
    fun setupControlsGuardTransportAndSettingsWhileLocked() {
        val source = playerControlsBinderSource()
        val seekController = playerSeekBarControllerSource()
        val block = source.substringAfter("fun bind() {")
            .substringBefore("\n    private fun View.setGuardedClick")

        assertTrue(block.contains("setGuardedClick(PlayerLockedInteraction.TRANSPORT)"))
        assertTrue(block.contains("setGuardedClick(PlayerLockedInteraction.SETTINGS)"))
        assertTrue(block.contains("setGuardedClick(PlayerLockedInteraction.LOCK_TOGGLE)"))
        assertTrue(seekController.contains("PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SEEK_BAR"))
        assertTrue(block.contains("PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.CHROME_TOGGLE"))
    }

    private fun playerActivitySource(): String {
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerChromeControllerSource(): String {
        return kotlinSource("PlayerChromeController.kt")
    }

    private fun playerSeekBarControllerSource(): String {
        return kotlinSource("PlayerSeekBarController.kt")
    }

    private fun playerControlsBinderSource(): String {
        return kotlinSource("PlayerControlsBinder.kt")
    }

    private fun kotlinSource(name: String): String {
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
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
