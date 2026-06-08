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

        assertTrue(source.contains("Gravity.BOTTOM"))
        assertTrue(source.contains("dimAmount = 0.52f"))
        assertTrue(source.contains("setCanceledOnTouchOutside(true)"))
        assertTrue(source.contains("SettingsConfirmationActionSheet.enter(content)"))
        assertTrue(source.contains("fun dismissWithAnimation"))
        assertTrue(source.contains("Color.parseColor(\"#FF3B30\")"))
        assertTrue(source.contains("Color.parseColor(\"#007AFF\")"))
        assertTrue(source.contains("Color.parseColor(\"#EBFFFFFF\")"))
        assertTrue(source.contains("Color.parseColor(\"#2E3C3C43\")"))
        assertTrue(source.contains("ViewCompat.setOnApplyWindowInsetsListener"))
        assertTrue(source.contains("WindowInsetsCompat.Type.systemBars()"))
        assertTrue(source.contains("Configuration.UI_MODE_NIGHT_YES"))
        assertTrue(source.contains("ActionSheetColors"))
        assertTrue(source.contains("Color.parseColor(\"#D91C1C1E\")"))
        assertTrue(source.contains("Color.parseColor(\"#F2F2F7\")"))
        assertTrue(source.contains("Color.parseColor(\"#AEAEB2\")"))
        assertTrue(source.contains("Color.parseColor(\"#FF453A\")"))
        assertTrue(source.contains("Color.parseColor(\"#0A84FF\")"))
        assertFalse(source.contains("MaterialAlertDialogBuilder"))
    }

    @Test
    fun actionSheetRequestsCancelDefaultFocusForRemoteUse() {
        val source = String(Files.readAllBytes(actionSheetSource()))

        assertTrue(source.contains("private var defaultFocusView: View? = null"))
        assertTrue(source.contains("defaultFocusView = cancelAction"))
        assertTrue(source.contains("requestDefaultFocus()"))
        assertTrue(source.contains("private fun requestDefaultFocus()"))
        assertTrue(source.contains("defaultFocusView?.post"))
        assertTrue(source.contains("defaultFocusView?.requestFocus()"))
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
}
