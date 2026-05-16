package com.example.openvideo.ui.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FolderVideosEpisodeOrderingSourceTest {

    @Test
    fun folderPlaybackQueueUsesEpisodeOrderingPolicy() {
        val source = String(Files.readAllBytes(folderVideosFragmentSource()))
        val openPlayer = source.substringAfter("private fun openPlayer(video: VideoItem)")
            .substringBefore("\n    private fun showVideoOptions")

        assertTrue(openPlayer.contains("PlayerEpisodeOrderingPolicy.orderSameFolderQueue(folderVideosSnapshot)"))
        assertTrue(openPlayer.contains("putSessionQueue(orderedQueue)"))
    }

    private fun folderVideosFragmentSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "local",
            "FolderVideosFragment.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
