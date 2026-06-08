package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerPlaybackEndBehaviorSourceTest {

    @Test
    fun playerActivityUsesPlaybackEndBehaviorPreference() {
        val source = String(Files.readAllBytes(playerPlaybackEndControllerSource()))

        assertTrue(source.contains("endBehavior = playerPrefs.playbackEndBehavior"))
    }

    @Test
    fun playbackSettingsExposePlaybackEndBehaviorPicker() {
        val activitySource = String(Files.readAllBytes(playbackSettingsActivitySource()))
        val sheetSource = String(Files.readAllBytes(playbackSettingsSheetSource()))
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))

        assertTrue(activitySource.contains("tv_playback_end_value"))
        assertTrue(activitySource.contains("playerPrefs.playbackEndBehavior"))
        assertTrue(sheetSource.contains("tv_playback_end_value"))
        assertTrue(sheetSource.contains("playerPrefs.playbackEndBehavior"))
        assertTrue(dialogSource.contains("settings_playback_end_behavior"))
        assertTrue(dialogSource.contains("playbackEndBehaviorLabel"))
    }

    @Test
    fun playbackEndBehaviorPickerRequestsListDefaultFocusForRemoteUse() {
        listOf(
            String(Files.readAllBytes(playbackSettingsActivitySource())),
            String(Files.readAllBytes(playbackSettingsSheetSource()))
        ).forEach { source ->
            val pickerBlock = source.substringAfter("tvPlaybackEnd.setOnClickListener")
                .substringBefore("\n        }\n")

            assertTrue(pickerBlock.contains("val dialog = AlertDialog.Builder"))
            assertTrue(pickerBlock.contains("dialog.listView?.post"))
            assertTrue(pickerBlock.contains("dialog.listView?.requestFocus()"))
        }
    }

    private fun playerActivitySource(): Path = moduleSource("ui", "player", "PlayerActivity.kt")

    private fun playerPlaybackEndControllerSource(): Path =
        moduleSource("ui", "player", "PlayerPlaybackEndController.kt")

    private fun playbackSettingsActivitySource(): Path =
        moduleSource("ui", "player", "PlayerPlaybackSettingsActivity.kt")

    private fun playbackSettingsSheetSource(): Path =
        moduleSource("ui", "player", "PlayerPlaybackSettingsSheet.kt")

    private fun playerSettingsDialogSource(): Path =
        moduleSource("ui", "player", "PlayerSettingsDialog.kt")

    private fun moduleSource(vararg segments: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", *segments)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
