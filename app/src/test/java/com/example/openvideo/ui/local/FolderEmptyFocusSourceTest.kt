package com.example.openvideo.ui.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FolderEmptyFocusSourceTest {

    @Test
    fun localFolderEmptyStateKeepsDpadPathToHeaderActions() {
        val source = sourceText("LocalFolderFragment.kt")
        val layout = layoutText("fragment_local_folders.xml")

        assertTrue(source.contains("emptyView.isFocusable = true"))
        assertTrue(source.contains("emptyView.nextFocusUpId = R.id.btn_refresh"))
        assertTrue(source.contains("private fun updateFolderFocusOrder(view: View, hasFolders: Boolean)"))
        assertTrue(source.contains("val contentFocusTargetId = if (hasFolders) R.id.recycler_folders else R.id.tv_empty"))
        assertTrue(source.contains("view.findViewById<View>(R.id.btn_series).nextFocusDownId = contentFocusTargetId"))
        assertTrue(source.contains("view.findViewById<View>(R.id.btn_refresh).nextFocusDownId = contentFocusTargetId"))
        assertTrue(layout.contains("""android:foreground="@drawable/bg_focusable_card""""))
    }

    @Test
    fun folderVideosEmptyStateKeepsDpadPathToBackAction() {
        val source = sourceText("FolderVideosFragment.kt")
        val layout = layoutText("fragment_folder_videos.xml")

        assertTrue(source.contains("emptyView.isFocusable = true"))
        assertTrue(source.contains("emptyView.nextFocusUpId = R.id.btn_back"))
        assertTrue(source.contains("private fun updateFolderVideoFocusOrder(view: View, hasVideos: Boolean)"))
        assertTrue(source.contains("val contentFocusTargetId = if (hasVideos) R.id.recycler_videos else R.id.tv_empty"))
        assertTrue(source.contains("view.findViewById<View>(R.id.btn_back).nextFocusDownId = contentFocusTargetId"))
        assertTrue(layout.contains("""android:foreground="@drawable/bg_focusable_card""""))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "local", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }

    private fun layoutText(name: String): String =
        layoutFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun layoutFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "res", "layout", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
