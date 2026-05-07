package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerHistorySourceTest {

    @Test
    fun activityPassesLocalPathToPlayerViewModel() {
        val source = String(Files.readAllBytes(sourceFile("PlayerActivity.kt")))
        val onCreate = source.substringAfter("override fun onCreate(savedInstanceState: Bundle?) {")
            .substringBefore("\n    private fun applyPlayerSettings()")

        assertTrue(
            "PlayerActivity should pass video_path into PlayerViewModel so local playback history keeps a playable local path",
            onCreate.contains("viewModel.initialize(Uri.parse(uriString), title, id, videoPath)")
        )
    }

    @Test
    fun playerReadyImmediatelySavesHistoryForRecentPlayback() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))
        val readyBlock = source.substringAfter("if (playbackState == androidx.media3.common.Player.STATE_READY) {")
            .substringBefore("\n                }")

        assertTrue(
            "PlayerViewModel should save history when media becomes ready so local-menu playback appears in Recent immediately",
            readyBlock.contains("markPlaybackStarted()")
        )
    }

    @Test
    fun playbackStartUpdatesRecentWithoutOverwritingSavedProgress() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))

        assertTrue(
            "Playback start should use a dedicated history update for Recent instead of saveHistory with currentPosition=0",
            source.contains("private fun markPlaybackStarted()")
        )
        assertTrue(
            "Playback start should preserve old resume progress when a history row already exists",
            source.contains("history?.lastPosition ?: 0L")
        )
    }

    @Test
    fun viewModelRegistersPlayerListenerBeforePreparingMedia() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))
        val initialize = source.substringAfter("fun initialize(uri: Uri, title: String, id: Long, path: String = \"\") {")
            .substringBefore("\n    fun restorePosition")

        assertTrue(
            "PlayerViewModel should add its listener before setMediaUri so fast local files cannot miss STATE_READY history saving",
            initialize.indexOf("playerManager.addListener(playerListener!!)") < initialize.indexOf("playerManager.setMediaUri(uri)")
        )
    }

    @Test
    fun saveHistoryUsesStoredVideoPathInsteadOfContentUriWhenAvailable() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))

        assertTrue(
            "PlayerViewModel should store the original video path when provided by local folders",
            source.contains("private var videoPath: String = \"\"")
        )
        assertTrue(
            "saveHistory should prefer stored videoPath over uri.toString()",
            source.contains("path = videoPath.ifBlank { uri.toString() }")
        )
    }

    private fun sourceFile(name: String): Path {
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
