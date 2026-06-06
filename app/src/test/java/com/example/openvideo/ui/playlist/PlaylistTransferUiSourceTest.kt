package com.example.openvideo.ui.playlist

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlaylistTransferUiSourceTest {

    @Test
    fun playlistDetailWiresSafImportAndJsonExportActions() {
        val fragment = sourceText("PlaylistDetailFragment.kt")
        val layout = layoutText("fragment_playlist_detail.xml")

        assertTrue(fragment.contains("ActivityResultContracts.CreateDocument(PlaylistTransferFormat.JSON_MIME_TYPE)"))
        assertTrue(fragment.contains("exportPlaylistLauncher.launch(PlaylistTransferFormat.suggestedJsonFileName(playlistName))"))
        assertTrue(fragment.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(fragment.contains("importPlaylistLauncher.launch(PlaylistTransferFormat.SUPPORTED_IMPORT_MIME_TYPES)"))
        assertTrue(fragment.contains("viewModel.writePlaylistExportTo(requireContext(), uri, playlistId, playlistName)"))
        assertTrue(fragment.contains("viewModel.readAndImportPlaylist(requireContext(), uri, playlistId)"))
        assertTrue(layout.contains("@+id/btn_import_playlist"))
        assertTrue(layout.contains("@+id/btn_export_playlist"))
    }

    @Test
    fun playlistViewModelUsesTransferFormatAndImportPolicy() {
        val source = sourceText("PlaylistViewModel.kt")

        assertTrue(source.contains("fun writePlaylistExportTo("))
        assertTrue(source.contains("PlaylistTransferFormat.exportJson("))
        assertTrue(source.contains("fun readAndImportPlaylist("))
        assertTrue(source.contains("PlaylistTransferFormat.parseJson"))
        assertTrue(source.contains("PlaylistTransferFormat.parseM3u"))
        assertTrue(source.contains("PlaylistImportPolicy.createRows("))
        assertTrue(source.contains("playlistDao.insertVideo(row)"))
    }

    private fun sourceText(name: String): String {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "playlist", name)
        return String(Files.readAllBytes(rootFile(relativePath)))
    }

    private fun layoutText(name: String): String =
        String(Files.readAllBytes(rootFile(Paths.get("src", "main", "res", "layout", name))))

    private fun rootFile(relativePath: Path): Path =
        sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
}
