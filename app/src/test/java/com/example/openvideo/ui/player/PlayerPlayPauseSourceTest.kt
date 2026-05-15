package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerPlayPauseSourceTest {

    @Test
    fun playerActivityDelegatesPlayPauseStateAndToggleSyncToPolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val updateBlock = source.substringAfter("private fun updatePlayPauseIcon(")
            .substringBefore("\n    private fun togglePlayPauseAndSyncIcon()")
        val toggleBlock = source.substringAfter("private fun togglePlayPauseAndSyncIcon() {")
            .substringBefore("\n    private fun syncPlayPauseIcon()")
        val syncBlock = source.substringAfter("private fun syncPlayPauseIcon() {")
            .substringBefore("\n    private fun saveProgressPeriodically")

        assertTrue(updateBlock.contains("PlayerPlayPausePolicy"))
        assertTrue(toggleBlock.contains("PlayerPlayPausePolicy"))
        assertTrue(toggleBlock.contains("viewModel.togglePlayPause()"))
        assertTrue(syncBlock.contains("PlayerPlayPausePolicy"))
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
