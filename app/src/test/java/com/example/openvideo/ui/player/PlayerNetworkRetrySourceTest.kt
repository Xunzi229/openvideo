package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerNetworkRetrySourceTest {

    @Test
    fun viewModelOwnsBoundedNetworkAutoRetryState() {
        val source = playerSource("PlayerViewModel.kt")

        assertTrue(source.contains("import com.example.openvideo.core.network.NetworkPlaybackRetryPolicy"))
        assertTrue(source.contains("private var networkAutoRetryAttempts = 0"))
        assertTrue(source.contains("private var networkAutoRetryJob: Job? = null"))
        assertTrue(source.contains("fun handleNetworkAutoRetry(error: PlaybackException): Boolean"))
        assertTrue(source.contains("NetworkPlaybackRetryPolicy.nextDecision("))
        assertTrue(source.contains("completedAttempts = networkAutoRetryAttempts"))
        assertTrue(source.contains("networkAutoRetryAttempts = decision.nextAttempt"))
        assertTrue(source.contains("delay(decision.delayMs)"))
        assertTrue(source.contains("retryPlayback(resetAutoRetry = false)"))
        assertTrue(source.contains("resetNetworkAutoRetry()"))
    }

    @Test
    fun eventControllerSuppressesHudWhileAutoRetryIsScheduled() {
        val source = playerSource("PlayerEventController.kt")
        val errorBlock = source.substringAfter("override fun onPlayerError(error: PlaybackException)")
            .substringBefore("\n            }")

        assertTrue(errorBlock.contains("val autoRetryScheduled = viewModel.handleNetworkAutoRetry(error)"))
        assertTrue(errorBlock.contains("if (!autoRetryScheduled)"))
        assertTrue(errorBlock.contains("onShowPlayerError(error)"))
    }

    @Test
    fun manualHudRetryResetsAutoRetryBudget() {
        val source = playerSource("PlayerErrorHudController.kt")
        val retryBlock = source.substringAfter("retryButtonProvider()?.setOnClickListener {")
            .substringBefore("\n        }")

        assertTrue(retryBlock.contains("viewModel.retryPlayback()"))
        assertFalse(retryBlock.contains("resetAutoRetry = false"))
    }

    private fun playerSource(file: String): String = loadText(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", file)
    )

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
