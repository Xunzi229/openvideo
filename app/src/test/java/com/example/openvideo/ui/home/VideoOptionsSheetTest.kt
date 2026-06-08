package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoOptionsSheetTest {

    @Test
    fun playOptionInvokesCallerPlayCallback() {
        val source = String(Files.readAllBytes(sourceFile("VideoOptionsSheet.kt")))

        assertTrue(source.contains("private val onPlay: () -> Unit"))
        assertTrue(source.contains("onPlay()"))
    }

    @Test
    fun homeAndFolderMenusRoutePlayToPlayer() {
        val homeSource = String(Files.readAllBytes(sourceFile("HomeFragment.kt")))
        val folderSource = String(Files.readAllBytes(folderSourceFile()))

        assertTrue(homeSource.contains("onPlay = { openPlayer(video) }"))
        assertTrue(folderSource.contains("onPlay = { openPlayer(video) }"))
    }

    @Test
    fun videoOptionsSheetRequestsPlayDefaultFocusForRemoteUse() {
        val source = String(Files.readAllBytes(sourceFile("VideoOptionsSheet.kt")))

        assertTrue(source.contains("val playOption = view.findViewById<TextView>(R.id.option_play)"))
        assertTrue(source.contains("playOption.post"))
        assertTrue(source.contains("playOption.requestFocus()"))
    }

    @Test
    fun videoOptionsSheetRowsAreFocusableForRemoteUse() {
        val layout = String(Files.readAllBytes(layoutFile("sheet_video_options.xml")))

        assertTrue(layout.contains("""android:id="@+id/option_play""""))
        assertTrue(layout.contains("""android:focusable="true""""))
        assertTrue(layout.contains("""android:foreground="?attr/selectableItemBackground""""))
    }

    @Test
    fun videoDetailsDialogRequestsOkDefaultFocusForRemoteUse() {
        val source = String(Files.readAllBytes(sourceFile("VideoOptionsSheet.kt")))
        val detailsBlock = source.substringAfter("private fun showDetails()")
            .substringBefore("\n}")

        assertTrue(detailsBlock.contains("val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)"))
        assertTrue(detailsBlock.contains("setPositiveButton(R.string.action_ok, null)"))
        assertTrue(detailsBlock.contains("getButton(android.app.AlertDialog.BUTTON_POSITIVE)"))
        assertTrue(detailsBlock.contains("okButton.post"))
        assertTrue(detailsBlock.contains("okButton.requestFocus()"))
    }

    @Test
    fun createPlaylistForVideoDialogsRequestNameInputDefaultFocusForRemoteUse() {
        val homeSource = String(Files.readAllBytes(sourceFile("HomeFragment.kt")))
        val folderSource = String(Files.readAllBytes(folderSourceFile()))

        listOf(homeSource, folderSource).forEach { source ->
            val createBlock = source.substringAfter("private fun showCreatePlaylistForVideoDialog(video: VideoItem)")
                .substringBefore("\n    private fun ")

            assertTrue(createBlock.contains("MaterialAlertDialogBuilder(requireContext())"))
            assertTrue(createBlock.contains("input.post"))
            assertTrue(createBlock.contains("input.requestFocus()"))
        }
    }

    @Test
    fun addToPlaylistPickerDialogsRequestListDefaultFocusForRemoteUse() {
        val homeSource = String(Files.readAllBytes(sourceFile("HomeFragment.kt")))
        val folderSource = String(Files.readAllBytes(folderSourceFile()))

        listOf(homeSource, folderSource).forEach { source ->
            val pickerBlock = source.substringAfter("private fun showPlaylistPicker(video: VideoItem, playlists: List<PlaylistEntity>)")
                .substringBefore("\n    private fun showCreatePlaylistForVideoDialog(")

            assertTrue(pickerBlock.contains("val dialog = MaterialAlertDialogBuilder(requireContext())"))
            assertTrue(pickerBlock.contains("dialog.listView?.post"))
            assertTrue(pickerBlock.contains("dialog.listView?.requestFocus()"))
        }
    }

    @Test
    fun videoDeleteConfirmationDialogsRequestCancelDefaultFocusForRemoteUse() {
        val homeSource = String(Files.readAllBytes(sourceFile("HomeFragment.kt")))
        val folderSource = String(Files.readAllBytes(folderSourceFile()))
        val homeDeleteBlock = homeSource.substringAfter("private fun confirmDelete(video: VideoItem)")
            .substringBefore("\n    private fun startMultiSelectMode(")
        val homeBatchDeleteBlock = homeSource.substringAfter("private fun confirmDeleteSelected()")
            .substringBefore("\n    private fun deleteVideosWithSystemRequest(")
        val folderDeleteBlock = folderSource.substringAfter("private fun confirmDelete(video: VideoItem)")
            .substringBefore("\n    private fun deleteVideosWithSystemRequest(")

        listOf(homeDeleteBlock, homeBatchDeleteBlock, folderDeleteBlock).forEach { block ->
            assertTrue(block.contains("setNegativeButton(R.string.action_cancel, null)"))
            assertTrue(block.contains("getButton(android.app.AlertDialog.BUTTON_NEGATIVE)"))
            assertTrue(block.contains("cancelButton.post"))
            assertTrue(block.contains("cancelButton.requestFocus()"))
        }
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "home",
            name
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun layoutFile(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "layout",
            name
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun folderSourceFile(): Path {
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
