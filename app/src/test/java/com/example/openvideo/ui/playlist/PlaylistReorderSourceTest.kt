package com.example.openvideo.ui.playlist

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlaylistReorderSourceTest {

    @Test
    fun playlistDetailUsesItemTouchHelperAndPersistsReorderedSnapshot() {
        val source = sourceText("PlaylistDetailFragment.kt")

        assertTrue(source.contains("import androidx.recyclerview.widget.ItemTouchHelper"))
        assertTrue(source.contains("private lateinit var itemTouchHelper: ItemTouchHelper"))
        assertTrue(source.contains("setupReorderTouchHelper()"))
        assertTrue(source.contains("override fun onMove("))
        assertTrue(source.contains("PlaylistReorderPolicy.move("))
        assertTrue(source.contains("adapter.submitList(reordered)"))
        assertTrue(source.contains("override fun clearView("))
        assertTrue(source.contains("viewModel.reorderPlaylistVideos(playlistId, playlistVideosSnapshot)"))
    }

    @Test
    fun viewModelAndDaoExposePositionPersistenceForReorder() {
        val viewModel = sourceText("PlaylistViewModel.kt")
        val dao = sourceText("PlaylistDao.kt", "data", "local")

        assertTrue(viewModel.contains("fun reorderPlaylistVideos("))
        assertTrue(viewModel.contains("playlistDao.updatePositions("))
        assertTrue(dao.contains("suspend fun updatePositions(videos: List<PlaylistVideoEntity>)"))
        assertTrue(dao.contains("@Update"))
    }

    private fun sourceText(name: String, vararg packageSegments: String): String {
        val segments = if (packageSegments.isEmpty()) arrayOf("ui", "playlist") else packageSegments
        return String(Files.readAllBytes(sourceFile(name, *segments)))
    }

    private fun sourceFile(name: String, vararg packageSegments: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", *packageSegments, name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
