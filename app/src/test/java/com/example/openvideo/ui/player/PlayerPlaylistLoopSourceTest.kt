package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerPlaylistLoopSourceTest {

    @Test
    fun playerActivityHandlesEndedStateWithSessionQueuePolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val listener = String(Files.readAllBytes(playerEventControllerSource()))
            .substringAfter("override fun onPlaybackStateChanged(playbackState: Int)")
            .substringBefore("\n            @OptIn(UnstableApi::class)")

        assertTrue(listener.contains("Player.STATE_ENDED"))
        assertTrue(listener.contains("onPlaybackEnded()"))
        assertTrue(source.contains("onPlaybackEnded = { playbackEnd.handleEnded() }"))

        val controller = String(Files.readAllBytes(playerPlaybackEndControllerSource()))
        val endedHandler = controller
            .substringAfter("fun handleEnded()")
            .substringBefore("\n    private fun", missingDelimiterValue = "")

        assertTrue(endedHandler.contains("PlayerPlaybackEndPolicy.decide"))
        assertTrue(endedHandler.contains("viewModel.sessionQueue.value"))
        assertTrue(endedHandler.contains("viewModel.playingVideoId"))
        assertTrue(endedHandler.contains("loopMode = playerPrefs.loopMode"))
        assertTrue(endedHandler.contains("endBehavior = playerPrefs.playbackEndBehavior"))
        assertTrue(endedHandler.contains("PlayerPlaybackEndAction.PLAY_NEXT"))
        assertTrue(endedHandler.contains("PlayerPlaybackEndAction.REPLAY_CURRENT"))
        assertTrue(endedHandler.contains("PlayerPlaybackEndAction.RETURN_TO_LIST"))
        assertTrue(endedHandler.contains("playNextQueueVideoAfterEnded(queue, decision.nextIndex)"))

        val nextHandler = controller
            .substringAfter("private fun playNextQueueVideoAfterEnded(queue: List<VideoItem>, nextIndex: Int?)")

        assertTrue(nextHandler.contains("onSwitchSessionVideo(item)"))
        assertTrue(nextHandler.contains("onApplyPlayerSettings()"))

        val switchHandler = source
            .substringAfter("private fun switchSessionVideo(item: VideoItem, onSwitched: () -> Unit = {})")
            .substringBefore("\n    private fun", missingDelimiterValue = "")

        assertTrue(switchHandler.contains("viewModel.switchToVideo("))
        assertTrue(switchHandler.contains("item = item"))
        assertTrue(switchHandler.contains("onPlayerRecreated = ::reattachPlayerAfterRetry"))
        assertTrue(switchHandler.contains("resetPlaybackSessionForNewVideo()"))

        val viewModel = String(Files.readAllBytes(playerViewModelSource()))
        val switchToVideo = viewModel
            .substringAfter("fun switchToVideo(")
            .substringBefore("\n    private fun", missingDelimiterValue = "")

        assertTrue(switchToVideo.contains("playerManager.initialize(item.uri)"))
        assertTrue(switchToVideo.contains("playerListener?.let { playerManager.addListener(it) }"))
        assertTrue(switchToVideo.contains("onPlayerRecreated()"))
        assertTrue(switchToVideo.contains("playerManager.setMediaUri(item.uri)"))
    }

    private fun playerActivitySource(): Path {
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerPlaybackEndControllerSource(): Path {
        return kotlinSource("PlayerPlaybackEndController.kt")
    }

    private fun playerEventControllerSource(): Path {
        return kotlinSource("PlayerEventController.kt")
    }

    private fun playerViewModelSource(): Path {
        return kotlinSource("PlayerViewModel.kt")
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
