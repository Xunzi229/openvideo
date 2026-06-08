package com.example.openvideo.ui.playlist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlaylistActionSheetSourceTest {

    @Test
    fun playlistRenameAndDeleteUseBottomActionSheetsWithoutChangingCallbacks() {
        val source = String(Files.readAllBytes(playlistFragmentSource()))
        val optionsBlock = source.substringAfter("private fun showPlaylistOptions(")
            .substringBefore("\n    private fun showRenameDialog")
        val renameBlock = source.substringAfter("private fun showRenameDialog(")
            .substringBefore("\n    private fun confirmDelete")
        val deleteBlock = source.substringAfter("private fun confirmDelete(")
            .substringBefore("\n    override fun onDestroyView")

        assertTrue(source.contains("private var activePlaylistDialog: Dialog? = null"))
        assertTrue(source.contains("private fun showExclusivePlaylistDialog("))

        assertTrue(optionsBlock.contains("PlaylistOptionsActionSheet.show("))
        assertTrue(optionsBlock.contains("onRename = { showRenameDialog(playlist) }"))
        assertTrue(optionsBlock.contains("onDelete = { confirmDelete(playlist) }"))
        assertFalse(optionsBlock.contains("MaterialAlertDialogBuilder"))
        assertFalse(optionsBlock.contains(".setItems("))

        assertTrue(renameBlock.contains("PlaylistRenameActionSheet.show("))
        assertTrue(renameBlock.contains("initialName = playlist.name"))
        assertTrue(renameBlock.contains("onConfirm = { name ->"))
        assertTrue(renameBlock.contains("viewModel.renamePlaylist(playlist.id, name)"))
        assertFalse(renameBlock.contains("MaterialAlertDialogBuilder"))

        assertTrue(deleteBlock.contains("SettingsConfirmationActionSheet.show("))
        assertTrue(deleteBlock.contains("titleRes = R.string.playlist_delete_title"))
        assertTrue(deleteBlock.contains("message = getString(R.string.playlist_delete_message, playlist.name)"))
        assertTrue(deleteBlock.contains("confirmRes = R.string.action_delete"))
        assertTrue(deleteBlock.contains("cancelRes = R.string.action_cancel"))
        assertTrue(deleteBlock.contains("onConfirm = { viewModel.deletePlaylist(playlist.id) }"))
        assertFalse(deleteBlock.contains("MaterialAlertDialogBuilder"))
    }

    @Test
    fun playlistActionSheetsUseIosBottomSheetVisualContract() {
        val options = String(Files.readAllBytes(playlistOptionsActionSheetSource()))
        val rename = String(Files.readAllBytes(playlistRenameActionSheetSource()))

        listOf(options, rename).forEach { source ->
            assertTrue(source.contains("Gravity.BOTTOM"))
            assertTrue(source.contains("setCanceledOnTouchOutside(true)"))
            assertTrue(source.contains("Configuration.UI_MODE_NIGHT_YES"))
            assertTrue(source.contains("Color.parseColor(\"#EBFFFFFF\")"))
            assertTrue(source.contains("Color.parseColor(\"#D91C1C1E\")"))
            assertTrue(source.contains("Color.parseColor(\"#FF453A\")"))
            assertTrue(source.contains("Color.parseColor(\"#0A84FF\")"))
            assertTrue(source.contains("ViewCompat.setOnApplyWindowInsetsListener"))
            assertTrue(source.contains("WindowInsetsCompat.Type.systemBars()"))
            assertFalse(source.contains("MaterialAlertDialogBuilder"))
        }
    }

    @Test
    fun playlistOptionsActionSheetRequestsCancelDefaultFocusForRemoteUse() {
        val source = String(Files.readAllBytes(playlistOptionsActionSheetSource()))

        assertTrue(source.contains("private var defaultFocusView: View? = null"))
        assertTrue(source.contains("defaultFocusView = cancelAction"))
        assertTrue(source.contains("requestDefaultFocus()"))
        assertTrue(source.contains("private fun requestDefaultFocus()"))
        assertTrue(source.contains("defaultFocusView?.post"))
        assertTrue(source.contains("defaultFocusView?.requestFocus()"))
    }

    @Test
    fun playlistCreateDialogRequestsNameInputDefaultFocusForRemoteUse() {
        val source = String(Files.readAllBytes(playlistFragmentSource()))
        val createBlock = source.substringAfter("private fun showCreateDialog()")
            .substringBefore("\n    private fun showPlaylistOptions(")

        assertTrue(createBlock.contains("MaterialAlertDialogBuilder(requireContext())"))
        assertTrue(createBlock.contains("input.post"))
        assertTrue(createBlock.contains("input.requestFocus()"))
    }

    private fun playlistFragmentSource(): Path = moduleSource("PlaylistFragment.kt")
    private fun playlistOptionsActionSheetSource(): Path = moduleSource("PlaylistOptionsActionSheet.kt")
    private fun playlistRenameActionSheetSource(): Path = moduleSource("PlaylistRenameActionSheet.kt")

    private fun moduleSource(fileName: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "playlist",
            fileName
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
