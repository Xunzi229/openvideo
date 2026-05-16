package com.example.openvideo.ui.playlist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlaylistDetailEpisodeOrderingSourceTest {

    @Test
    fun playlistPlaybackUsesEligibleEpisodeOrdering() {
        val source = String(Files.readAllBytes(playlistDetailSource()))
        val onClick = source.substringAfter("onClick = { video ->")
            .substringBefore("onRemove = { video ->")

        assertTrue(onClick.contains("PlayerEpisodeOrderingPolicy.orderQueueIfEligible(queue)"))
        assertTrue(onClick.contains("putSessionQueue(orderedQueue)"))
    }

    @Test
    fun homePlaybackDoesNotApplyEpisodeOrdering() {
        val source = String(Files.readAllBytes(homeFragmentSource()))

        assertFalse(source.contains("orderQueueIfEligible"))
        assertFalse(source.contains("orderSameFolderQueue"))
    }

    @Test
    fun historyPlaybackDoesNotApplyEpisodeOrdering() {
        val source = String(Files.readAllBytes(historyFragmentSource()))

        assertFalse(source.contains("orderQueueIfEligible"))
        assertFalse(source.contains("orderSameFolderQueue"))
    }

    private fun playlistDetailSource(): Path = moduleSource("ui", "playlist", "PlaylistDetailFragment.kt")

    private fun homeFragmentSource(): Path = moduleSource("ui", "home", "HomeFragment.kt")

    private fun historyFragmentSource(): Path = moduleSource("ui", "history", "HistoryFragment.kt")

    private fun moduleSource(vararg segments: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", *segments)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
