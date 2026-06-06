package com.example.openvideo.ui.playlist

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlaylistCleanupUiSourceTest {

    @Test
    fun playlistDetailLayoutExposesCleanupButton() {
        val layout = layoutText("fragment_playlist_detail.xml")

        assertTrue(layout.contains("@+id/btn_cleanup_playlist"))
        assertTrue(layout.contains("@string/playlist_cleanup"))
    }

    @Test
    fun playlistDetailFragmentConfirmsBeforeCleanup() {
        val source = sourceText("PlaylistDetailFragment.kt")

        assertTrue(source.contains("R.id.btn_cleanup_playlist"))
        assertTrue(source.contains("confirmCleanup()"))
        assertTrue(source.contains("private fun confirmCleanup()"))
        assertTrue(source.contains("R.string.playlist_cleanup_title"))
        assertTrue(source.contains("R.string.playlist_cleanup_message"))
        assertTrue(source.contains("val removedVideos = viewModel.cleanupPlaylistVideosForUndo(playlistId)"))
        assertTrue(source.contains("showCleanupUndo(removedVideos)"))
    }

    @Test
    fun playlistDetailFragmentOffersUndoAfterCleanup() {
        val source = sourceText("PlaylistDetailFragment.kt")

        assertTrue(source.contains("import com.google.android.material.snackbar.Snackbar"))
        assertTrue(source.contains("cleanupPlaylistVideosForUndo(playlistId)"))
        assertTrue(source.contains("showCleanupUndo(removedVideos)"))
        assertTrue(source.contains("private fun showCleanupUndo(removedVideos: List<PlaylistVideoEntity>)"))
        assertTrue(source.contains("Snackbar.make(requireView(), R.string.playlist_cleanup_complete, Snackbar.LENGTH_LONG)"))
        assertTrue(source.contains(".setAction(R.string.action_undo)"))
        assertTrue(source.contains("viewModel.restorePlaylistVideos(removedVideos)"))
    }

    private fun sourceText(name: String): String = String(Files.readAllBytes(sourceFile(name)))

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "playlist", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun layoutText(name: String): String = String(Files.readAllBytes(layoutFile(name)))

    private fun layoutFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
