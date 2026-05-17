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
        val source = playerActivitySource()
        val block = source.substringAfter("private fun applyControlVisibility() {")
            .substringBefore("\n    private fun View.setPlayerClickListener(")

        assertTrue(block.contains("PlayerLockedControlsPolicy.visibility(isScreenLocked, controlsVisible)"))
        assertTrue(block.contains("PlayerLockedControlsPolicy.isChromeRegionVisible("))
        assertTrue(block.contains("PlayerChromeRegion.TOP_SCRIM"))
        assertTrue(block.contains("PlayerChromeRegion.LAND_RIGHT_FLOAT_COLUMN"))
        assertFalse(
            "Chrome visibility must not bypass PlayerLockedControlsPolicy.",
            block.contains("PlayerControlState.visibilityFor(isScreenLocked")
        )
    }

    @Test
    fun setupControlsGuardTransportAndSettingsWhileLocked() {
        val source = playerActivitySource()
        val block = source.substringAfter("private fun setupControls() {")
            .substringBefore("\n        playerListener = object : Player.Listener {")

        assertTrue(block.contains("setPlayerClickListener(PlayerLockedInteraction.TRANSPORT)"))
        assertTrue(block.contains("setPlayerClickListener(PlayerLockedInteraction.SETTINGS)"))
        assertTrue(block.contains("setPlayerClickListener(PlayerLockedInteraction.LOCK_TOGGLE)"))
        assertTrue(block.contains("PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SEEK_BAR"))
        assertTrue(block.contains("PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.CHROME_TOGGLE"))
    }

    private fun playerActivitySource(): String {
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
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
