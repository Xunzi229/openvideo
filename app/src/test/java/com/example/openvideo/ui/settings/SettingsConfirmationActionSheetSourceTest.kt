package com.example.openvideo.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SettingsConfirmationActionSheetSourceTest {

    @Test
    fun clearConfirmationsUseBottomActionSheetWithoutChangingCallbacks() {
        val source = String(Files.readAllBytes(settingsFragmentSource()))
        val clearCacheBlock = source.substringAfter("view.findViewById<View>(R.id.row_clear_cache).setOnClickListener")
            .substringBefore("\n        view.findViewById<View>(R.id.row_clear_history)")
        val clearHistoryBlock = source.substringAfter("view.findViewById<View>(R.id.row_clear_history).setOnClickListener")
            .substringBefore("\n        view.findViewById<View>(R.id.row_project_repo)")

        listOf(clearCacheBlock, clearHistoryBlock).forEach { block ->
            assertTrue(block.contains("showExclusiveSettingsDialog"))
            assertTrue(block.contains("SettingsConfirmationActionSheet.show("))
            assertFalse(block.contains("MaterialAlertDialogBuilder"))
            assertFalse(block.contains(".setPositiveButton("))
            assertFalse(block.contains(".setNegativeButton("))
        }

        assertTrue(clearCacheBlock.contains("titleRes = R.string.dialog_clear_cache_title"))
        assertTrue(clearCacheBlock.contains("messageRes = R.string.dialog_clear_cache_message"))
        assertTrue(clearCacheBlock.contains("confirmRes = R.string.action_clear"))
        assertTrue(clearCacheBlock.contains("cancelRes = R.string.action_cancel"))
        assertTrue(clearCacheBlock.contains("onConfirm = { viewModel.clearCache() }"))

        assertTrue(clearHistoryBlock.contains("titleRes = R.string.dialog_clear_history_title"))
        assertTrue(clearHistoryBlock.contains("messageRes = R.string.dialog_clear_history_message"))
        assertTrue(clearHistoryBlock.contains("confirmRes = R.string.action_clear"))
        assertTrue(clearHistoryBlock.contains("cancelRes = R.string.action_cancel"))
        assertTrue(clearHistoryBlock.contains("onConfirm = { viewModel.clearHistory() }"))
    }

    @Test
    fun actionSheetUsesIosBottomSheetVisualContract() {
        val source = String(Files.readAllBytes(actionSheetSource()))
        val chrome = String(Files.readAllBytes(appleActionSheetSource()))

        assertTrue(source.contains("AppleActionSheet.show("))
        assertTrue(source.contains("AppleActionStyle.DESTRUCTIVE"))
        assertFalse(source.contains("MaterialAlertDialogBuilder"))

        assertTrue(chrome.contains("Gravity.BOTTOM"))
        assertTrue(chrome.contains("setCanceledOnTouchOutside(true)"))
        assertTrue(chrome.contains("OverlayWindowInsets.bind"))
        assertTrue(chrome.contains("AppleOverlayColors.from"))
        assertFalse(chrome.contains("MaterialAlertDialogBuilder"))
    }

    @Test
    fun actionSheetRequestsCancelDefaultFocusForRemoteUse() {
        val chrome = String(Files.readAllBytes(appleActionSheetSource()))

        assertTrue(chrome.contains("private var defaultFocusView: View? = null"))
        assertTrue(chrome.contains("defaultFocusCancel"))
        assertTrue(chrome.contains("defaultFocusView?.requestFocus()"))
    }

    private fun settingsFragmentSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "settings",
            "SettingsFragment.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun actionSheetSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "settings",
            "SettingsConfirmationActionSheet.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun appleActionSheetSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "ui",
            "AppleActionSheet.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
