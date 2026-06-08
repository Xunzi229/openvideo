package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomePrivacySourceTest {

    @Test
    fun recentAndFavoriteFallbackItemsRespectHiddenFolders() {
        val source = String(Files.readAllBytes(homeViewModelSource()))

        assertTrue(source.contains("_hiddenFolders"))
        assertTrue(source.contains("videosFromHistory(scanned, history, hiddenFolders, permissionDenied)"))
        assertTrue(source.contains("videosFromFavorites(scanned, favorites, hiddenFolders, permissionDenied)"))
        assertTrue(source.contains("MediaLibraryPolicy.shouldExposeStoredFallback("))
        assertTrue(source.contains("hiddenFolders = hiddenFolders"))
    }

    @Test
    fun privacyAddDialogRequestsPathInputDefaultFocusForRemoteUse() {
        val source = String(Files.readAllBytes(privacyFragmentSource()))
        val addBlock = source.substringAfter("private fun showAddDialog()")
            .substringBefore("\n    private fun confirmRemove(")

        assertTrue(addBlock.contains("MaterialAlertDialogBuilder(requireContext())"))
        assertTrue(addBlock.contains("input.post"))
        assertTrue(addBlock.contains("input.requestFocus()"))
    }

    @Test
    fun privacyRemoveDialogRequestsCancelDefaultFocusForRemoteUse() {
        val source = String(Files.readAllBytes(privacyFragmentSource()))
        val removeBlock = source.substringAfter("private fun confirmRemove(path: String)")
            .substringBefore("\n}")

        assertTrue(removeBlock.contains("setNegativeButton(R.string.action_cancel, null)"))
        assertTrue(removeBlock.contains("getButton(android.app.AlertDialog.BUTTON_NEGATIVE)"))
        assertTrue(removeBlock.contains("cancelButton.post"))
        assertTrue(removeBlock.contains("cancelButton.requestFocus()"))
    }

    private fun homeViewModelSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "home",
            "HomeViewModel.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun privacyFragmentSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "privacy",
            "PrivacyFragment.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
