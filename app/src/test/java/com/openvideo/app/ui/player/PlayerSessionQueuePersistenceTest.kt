package com.openvideo.app.ui.player

import android.app.Application
import android.content.ContextWrapper
import android.net.Uri
import com.openvideo.app.data.model.VideoItem
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class PlayerSessionQueuePersistenceTest {
    @get:Rule val temporary = TemporaryFolder()
    private val context get() = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
        override fun getApplicationContext() = this
        override fun getNoBackupFilesDir() = temporary.root
    }
    private fun video(id: Long) = VideoItem(id, "Episode $id", "content://media/$id", Uri.parse("content://media/$id"),
        1000, 2000, 1920, 1080, 100, Uri.parse("content://thumb/$id"), "/Movies/$id.mp4", 200, 90)
    private fun record(thumbnail: String?) = PlayerSessionQueueRecord(42, "Restored", "content://media/42", "content://media/42",
        1000, 2000, 1920, 1080, 100, thumbnail, "/Movies/42.mp4", 200, 90)

    @Test fun registeredQueueIsSnapshotAndPersistsAllMetadata() {
        val videos = mutableListOf(video(1), video(2).copy(thumbnailUri = null))
        val expected = videos.toList()
        val token = PlayerSessionQueueStore.register(context, videos)
        videos.clear()
        assertEquals(expected, PlayerSessionQueueStore.resolve(context, token))
        val disk = File(temporary.root, "player_session_queues/$token.queue")
        val records = PlayerSessionQueueCodec.read(disk.inputStream())
        assertEquals(listOf(1L, 2L), records.map { it.id })
        assertEquals(expected.map { it.thumbnailUri?.toString() }, records.map { it.thumbnailUri })
        assertEquals(expected.map { it.libraryPath }, records.map { it.libraryPath })
        assertEquals(listOf(90, 90), records.map { it.orientationDegrees })
    }

    @Test fun validDiskQueueRestoresFieldsAndNormalizesEmptyThumbnail() {
        for (thumbnail in listOf(null, "", "content://thumb/42")) {
            val token = UUID.randomUUID().toString()
            val directory = File(temporary.root, "player_session_queues").apply { mkdirs() }
            val file = File(directory, "$token.queue")
            PlayerSessionQueueCodec.write(listOf(record(thumbnail)), file.outputStream())
            val actual = PlayerSessionQueueStore.resolve(context, token).single()
            assertEquals(video(42).copy(title = "Restored", thumbnailUri = thumbnail?.takeIf { it.isNotBlank() }?.let(Uri::parse)), actual)
            file.delete()
            assertEquals(listOf(actual), PlayerSessionQueueStore.resolve(context, token))
        }
    }

    @Test fun invalidTokensMissingAndCorruptFilesReturnEmptyQueue() {
        for (token in listOf(null, "", "../bad", "1-1-1-1-1", UUID.randomUUID().toString().uppercase(), UUID.randomUUID().toString())) {
            assertTrue(PlayerSessionQueueStore.resolve(context, token).isEmpty())
        }
        val token = UUID.randomUUID().toString()
        val directory = File(temporary.root, "player_session_queues").apply { mkdirs() }
        File(directory, "$token.queue").writeText("corrupt")
        assertTrue(PlayerSessionQueueStore.resolve(context, token).isEmpty())
    }

    @Test fun cacheRetainsEightQueuesAndLeavesUnrelatedFilesAlone() {
        val directory = File(temporary.root, "player_session_queues").apply { mkdirs() }
        val unrelated = File(directory, "other.txt").apply { writeText("keep") }
        File(directory, "directory.queue").mkdir()
        repeat(10) { PlayerSessionQueueStore.register(context, listOf(video(it.toLong()))) }
        assertEquals(8, directory.listFiles()!!.count { it.isFile && it.extension == "queue" })
        assertEquals("keep", unrelated.readText())
    }

    @Test fun unwritableCacheStillTransfersQueueFromMemory() {
        File(temporary.root, "player_session_queues").writeText("not a directory")
        val videos = listOf(video(1))
        val token = PlayerSessionQueueStore.register(context, videos)
        assertEquals(videos, PlayerSessionQueueStore.resolve(context, token))
    }
}
