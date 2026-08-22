package com.example.openvideo.ui.home

import org.junit.Assert.assertFalse
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
        assertTrue(source.contains("onPlay"))
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

        assertTrue(source.contains("AppleActionSheet.show"))
        assertTrue(source.contains("R.string.option_play"))
        assertTrue(source.contains("defaultFocusCancel = false"))
    }

    @Test
    fun videoOptionsSheetUsesAppleActionSheetNotMaterialBottomSheet() {
        val source = String(Files.readAllBytes(sourceFile("VideoOptionsSheet.kt")))

        assertTrue(source.contains("AppleActionSheet.show"))
        assertTrue(source.contains("AppleActionStyle.DESTRUCTIVE"))
        assertFalse(source.contains("BottomSheetDialog"))
        assertFalse(source.contains("sheet_video_options"))
    }

    @Test
    fun videoDetailsDialogRequestsOkDefaultFocusForRemoteUse() {
        val source = String(Files.readAllBytes(sourceFile("VideoOptionsSheet.kt")))
        val detailsBlock = source.substringAfter("private fun showDetails()")
            .substringBefore("\n}")

        assertTrue(detailsBlock.contains("AppleAlertDialog.show"))
        assertTrue(detailsBlock.contains("R.string.action_ok"))
        assertTrue(detailsBlock.contains("AppleActionStyle.CANCEL"))
        assertFalse(detailsBlock.contains("MaterialAlertDialogBuilder"))
    }

    @Test
    fun createPlaylistForVideoDialogsRequestNameInputDefaultFocusForRemoteUse() {
        val homeSource = String(Files.readAllBytes(sourceFile("HomeFragment.kt")))
        val folderSource = String(Files.readAllBytes(folderSourceFile()))

        listOf(homeSource, folderSource).forEach { source ->
            val createBlock = source.substringAfter("private fun showCreatePlaylistForVideoDialog(video: VideoItem)")
                .substringBefore("\n    private fun ")

            assertTrue(createBlock.contains("AppleAlertDialog.show"))
            assertTrue(createBlock.contains("input.post"))
            assertTrue(createBlock.contains("input.requestFocus()"))
            assertFalse(createBlock.contains("MaterialAlertDialogBuilder"))
        }
    }

    @Test
    fun addToPlaylistPickerDialogsRequestListDefaultFocusForRemoteUse() {
        val homeSource = String(Files.readAllBytes(sourceFile("HomeFragment.kt")))
        val folderSource = String(Files.readAllBytes(folderSourceFile()))

        listOf(homeSource, folderSource).forEach { source ->
            val pickerBlock = source.substringAfter("private fun showPlaylistPicker(video: VideoItem, playlists: List<PlaylistEntity>)")
                .substringBefore("\n    private fun showCreatePlaylistForVideoDialog(")

            assertTrue(pickerBlock.contains("AppleActionSheet.show"))
            assertTrue(pickerBlock.contains("defaultFocusCancel = false"))
            assertFalse(pickerBlock.contains("MaterialAlertDialogBuilder"))
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
            assertTrue(block.contains("AppleAlertDialog.show"))
            assertTrue(block.contains("AppleActionStyle.CANCEL"))
            assertTrue(block.contains("AppleActionStyle.DESTRUCTIVE"))
            assertFalse(block.contains("MaterialAlertDialogBuilder"))
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
