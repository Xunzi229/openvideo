package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerLockPauseSourceTest {

    @Test
    fun pausingOnExitClearsPlayerLockBeforePausing() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val onPause = source.substringAfter("override fun onPause() {")
            .substringBefore("\n    override fun onDestroy()")

        val unlockIndex = onPause.indexOf("unlockPlayerForPause()")
        val pauseIndex = onPause.indexOf("pause()")

        assertTrue("onPause should unlock the player when it is about to pause playback", unlockIndex >= 0)
        assertTrue(
            "onPause should unlock before pausing so returning to the player is not trapped behind lock mode",
            unlockIndex < pauseIndex
        )
    }

    @Test
    fun playerPauseEventsClearPlayerLock() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(
            "Player listener should clear lock when playback changes to a paused intent",
            source.contains("if (!playWhenReady) unlockPlayerForPause()")
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
