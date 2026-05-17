package com.example.openvideo.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Pure source-level guards for [PlaybackServiceIntents]:
 *
 * - `start(...)` must target [PlaybackService], use [PlaybackService.ACTION_START], and forward
 *   the title + isPlaying extras.
 * - `stop(...)` must target [PlaybackService] without setting an explicit action (Service stop is
 *   driven by `stopService`, not by an action string).
 *
 * Also asserts the call sites in `PlayerActivity` go through the helper instead of building the
 * intent inline, so future Service contract changes only need one update.
 */
class PlaybackServiceIntentsSourceTest {

    @Test
    fun startIntentContainsActionAndExtras() {
        val source = intentsSource()
        val startBlock = source.substringAfter("fun start(")
            .substringBefore("\n\n    fun stop(")

        assertTrue(startBlock.contains("Intent(context, PlaybackService::class.java)"))
        assertTrue(startBlock.contains("action = PlaybackService.ACTION_START"))
        assertTrue(startBlock.contains("putExtra(PlaybackService.EXTRA_TITLE, title)"))
        assertTrue(startBlock.contains("putExtra(PlaybackService.EXTRA_IS_PLAYING, isPlaying)"))
    }

    @Test
    fun stopIntentDismissesForegroundNotification() {
        val source = intentsSource()
        val stopBlock = source.substringAfter("fun stop(")
            .substringBefore("\n\n    fun openPlayer(")

        assertTrue(stopBlock.contains("Intent(context, PlaybackService::class.java)"))
        assertTrue(stopBlock.contains("action = PlaybackService.ACTION_DISMISS"))
    }

    @Test
    fun controlIntentsUseExplicitServiceActions() {
        val source = intentsSource()
        assertTrue(source.contains("PlaybackService.ACTION_TOGGLE_PLAY_PAUSE"))
        assertTrue(source.contains("PlaybackService.ACTION_SKIP_TO_NEXT"))
        assertTrue(source.contains("PlaybackService.ACTION_SKIP_TO_PREVIOUS"))
        assertTrue(source.contains("PlaybackService.ACTION_REFRESH"))
    }

    @Test
    fun openPlayerIntentCarriesSessionExtrasAndSingleTopFlags() {
        val source = intentsSource()
        val openBlock = source.substringAfter("fun openPlayer(")
            .substringBefore("\n\n    private fun serviceAction(")

        assertTrue(openBlock.contains("FLAG_ACTIVITY_SINGLE_TOP"))
        assertTrue(openBlock.contains("FLAG_ACTIVITY_CLEAR_TOP"))
        assertTrue(openBlock.contains("putSessionQueue(snapshot.queue)"))
    }

    @Test
    fun playerActivityRoutesPlaybackServiceThroughHelper() {
        val activitySource = playerActivitySource()
        assertTrue(
            "Activity must build the start intent via PlaybackServiceIntents.start(...).",
            activitySource.contains("PlaybackServiceIntents.start(")
        )
        assertTrue(
            "Activity must build the stop intent via PlaybackServiceIntents.stop(...).",
            activitySource.contains("PlaybackServiceIntents.stop(this)")
        )
        assertFalse(
            "Activity must not inline PlaybackService.ACTION_START anymore.",
            activitySource.contains("PlaybackService.ACTION_START")
        )
        assertFalse(
            "Activity must not branch on Build.VERSION_CODES.O for startForegroundService anymore.",
            activitySource.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.O") &&
                activitySource.contains("ContextCompat.startForegroundService")
        )
        assertTrue(
            "Activity must sync notification snapshot before starting the service.",
            activitySource.contains("syncPlaybackNotificationSnapshot()")
        )
        assertTrue(
            "Activity must register notification control handlers.",
            activitySource.contains("registerPlaybackNotificationHandlers()")
        )
        assertTrue(
            "Activity must dismiss playback notification when exiting.",
            activitySource.contains("dismissPlaybackNotification()")
        )
    }

    private fun intentsSource(): String = loadSource(
        Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "player",
            "PlaybackServiceIntents.kt"
        )
    )

    private fun playerActivitySource(): String = loadSource(
        Paths.get(
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
    )

    private fun loadSource(relativePath: Path): String {
        val path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
