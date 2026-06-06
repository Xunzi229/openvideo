package com.example.openvideo.ui.playlist

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlaylistCleanupSourceTest {

    @Test
    fun viewModelExposesConservativePlaylistCleanupEntryPoint() {
        val source = sourceText("PlaylistViewModel.kt")

        assertTrue(source.contains("fun cleanupPlaylistVideos(playlistId: Long)"))
        assertTrue(source.contains("val plan = PlaylistCleanupPolicy.plan(list)"))
        assertTrue(source.contains("cleanupPlaylistVideosForUndo(playlistId)"))
        assertTrue(source.contains("removePlaylistVideosNow(playlistId, plan.removableVideoIds)"))
    }

    @Test
    fun viewModelCanReturnRemovedRowsForCleanupUndo() {
        val source = sourceText("PlaylistViewModel.kt")

        assertTrue(source.contains("suspend fun cleanupPlaylistVideosForUndo(playlistId: Long): List<PlaylistVideoEntity>"))
        assertTrue(source.contains("val removableIds = plan.removableVideoIds.toSet()"))
        assertTrue(source.contains("val removedVideos = list.filter { video -> video.videoId in removableIds }"))
        assertTrue(source.contains("return removedVideos"))
        assertTrue(source.contains("fun restorePlaylistVideos(videos: List<PlaylistVideoEntity>)"))
        assertTrue(source.contains("videos.forEach { video -> playlistDao.insertVideo(video) }"))
    }

    private fun sourceText(name: String): String = String(Files.readAllBytes(sourceFile(name)))

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "playlist", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
