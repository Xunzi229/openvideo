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
        val chrome = String(Files.readAllBytes(appleActionSheetSource()))

        assertTrue(options.contains("AppleActionSheet.show("))
        assertTrue(options.contains("AppleActionStyle.DESTRUCTIVE"))
        assertFalse(options.contains("MaterialAlertDialogBuilder"))

        assertTrue(rename.contains("Gravity.BOTTOM"))
        assertTrue(rename.contains("setCanceledOnTouchOutside(true)"))
        assertTrue(rename.contains("AppleOverlayColors.from"))
        assertTrue(rename.contains("OverlayWindowInsets.bind"))
        assertFalse(rename.contains("MaterialAlertDialogBuilder"))

        assertTrue(chrome.contains("Gravity.BOTTOM"))
        assertTrue(chrome.contains("setCanceledOnTouchOutside(true)"))
        assertTrue(chrome.contains("OverlayWindowInsets.bind"))
        assertTrue(chrome.contains("AppleOverlayColors.from"))
        assertFalse(chrome.contains("MaterialAlertDialogBuilder"))
    }

    @Test
    fun playlistOptionsActionSheetRequestsCancelDefaultFocusForRemoteUse() {
        val chrome = String(Files.readAllBytes(appleActionSheetSource()))

        assertTrue(chrome.contains("private var defaultFocusView: View? = null"))
        assertTrue(chrome.contains("defaultFocusCancel"))
        assertTrue(chrome.contains("defaultFocusView?.requestFocus()"))
    }

    @Test
    fun playlistCreateDialogRequestsNameInputDefaultFocusForRemoteUse() {
        val source = String(Files.readAllBytes(playlistFragmentSource()))
        val createBlock = source.substringAfter("private fun showCreateDialog()")
            .substringBefore("\n    private fun showPlaylistOptions(")

        assertTrue(createBlock.contains("AppleAlertDialog.show"))
        assertTrue(createBlock.contains("input.post"))
        assertTrue(createBlock.contains("input.requestFocus()"))
        assertFalse(createBlock.contains("MaterialAlertDialogBuilder"))
    }

    private fun playlistFragmentSource(): Path = moduleSource("ui", "playlist", "PlaylistFragment.kt")
    private fun playlistOptionsActionSheetSource(): Path = moduleSource("ui", "playlist", "PlaylistOptionsActionSheet.kt")
    private fun playlistRenameActionSheetSource(): Path = moduleSource("ui", "playlist", "PlaylistRenameActionSheet.kt")
    private fun appleActionSheetSource(): Path = moduleSource("core", "ui", "AppleActionSheet.kt")

    private fun moduleSource(vararg parts: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", *parts)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
