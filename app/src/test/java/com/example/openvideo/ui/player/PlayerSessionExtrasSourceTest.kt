package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSessionExtrasSourceTest {

    @Test
    fun sessionQueueUsesLightweightTokenAndPersistentCache() {
        val extras = loadText("PlayerSessionExtras.kt")
        val store = loadText("PlayerSessionQueueStore.kt")

        assertTrue(extras.contains("putSessionQueueToken(PlayerSessionQueueStore.register(context, videos))"))
        assertTrue(extras.contains("PlayerSessionQueueStore.resolve(context, sessionQueueToken())"))
        assertTrue(store.contains("PlayerSessionQueueCodec.write("))
        assertTrue(store.contains("PlayerSessionQueueCodec.read("))
        assertTrue(store.contains("context.applicationContext.noBackupFilesDir"))
        assertTrue(store.contains("MAX_RETAINED_QUEUES"))
        assertFalse(extras.contains("putParcelableArrayListExtra"))
        assertFalse(extras.contains("Bundle"))
    }

    @Test
    fun notificationSnapshotReusesStableSessionQueueToken() {
        val controller = loadText("PlayerPlaybackNotificationController.kt")
        val serviceIntents = loadCoreText("PlaybackServiceIntents.kt")

        assertTrue(controller.contains("viewModel.sessionQueueToken"))
        assertTrue(controller.contains("PlayerSessionQueueStore.register(activity, queue)"))
        assertTrue(controller.contains("sessionQueueToken = sessionQueueToken"))
        assertTrue(serviceIntents.contains("putSessionQueueToken(snapshot.sessionQueueToken)"))
        assertFalse(serviceIntents.contains("putSessionQueue(snapshot.queue)"))
    }

    @Test
    fun playerFallsBackToCurrentVideoWhenCacheIsUnavailable() {
        val source = loadText("PlayerActivity.kt")
        val startupBlock = source.substringAfter("val intentSessionQueue = intent.sessionVideoQueue(this)")
            .substringBefore("val warmResume")

        assertTrue(startupBlock.contains("snapshotSessionQueue.isNotEmpty()"))
        assertTrue(startupBlock.contains("VideoItem("))
        assertTrue(startupBlock.contains("sessionQueueToken = null"))
        assertTrue(startupBlock.contains("viewModel.setSessionQueue(sessionQueue, sessionQueueToken)"))
    }

    private fun loadText(fileName: String): String = load(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", fileName)
    )

    private fun loadCoreText(fileName: String): String = load(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "core", "player", fileName)
    )

    private fun load(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
