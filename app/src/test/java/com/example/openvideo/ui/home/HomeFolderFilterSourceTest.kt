package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeFolderFilterSourceTest {

    @Test
    fun homeViewModelExposesFolderFilterState() {
        val source = String(Files.readAllBytes(homeViewModelSource()))

        assertTrue(source.contains("_selectedFolderKey"))
        assertTrue(source.contains("val folders: StateFlow"))
        assertTrue(source.contains("fun setFolderFilter(folderKey: String?)"))
        assertTrue(source.contains("MediaLibraryPolicy.validFolderKey"))
        assertTrue(source.contains("folderKey = selectedFolderKey"))
        assertTrue(source.contains("videosForFolderChips"))
        assertTrue(source.contains("VideoFolderFilterPolicy.displayFolders"))
        assertTrue(source.contains("togglePinnedFolder"))
        assertTrue(source.contains("prunePinnedKeys"))
    }

    @Test
    fun homeFragmentBindsFolderFilterChips() {
        val source = String(Files.readAllBytes(homeFragmentSource()))
        val layout = String(Files.readAllBytes(homeLayoutSource()))

        assertTrue(layout.contains("@+id/folder_group"))
        assertTrue(source.contains("folderGroup"))
        assertTrue(source.contains("viewModel.folders.collect"))
        assertTrue(source.contains("viewModel.selectedFolderKey.collect"))
        assertTrue(source.contains("bindFolderChips"))
        assertTrue(source.contains("togglePinnedFolder"))
        assertTrue(source.contains("folderChipLabel"))
    }

    @Test
    fun homeFragmentShowsFolderPinHintWhenFoldersExist() {
        val source = String(Files.readAllBytes(homeFragmentSource()))
        val layout = String(Files.readAllBytes(homeLayoutSource()))

        assertTrue(layout.contains("@+id/folder_pin_hint"))
        assertTrue(layout.contains("@string/home_folder_pin_hint"))
        assertTrue(source.contains("private lateinit var folderPinHint: TextView"))
        assertTrue(source.contains("folderPinHint = view.findViewById(R.id.folder_pin_hint)"))
        assertTrue(source.contains("folderPinHint.visibility = if (currentFolders.size > 1) View.VISIBLE else View.GONE"))
    }

    private fun homeViewModelSource(): Path = sourcePath(
        "main",
        "java",
        "com",
        "example",
        "openvideo",
        "ui",
        "home",
        "HomeViewModel.kt"
    )

    private fun homeFragmentSource(): Path = sourcePath(
        "main",
        "java",
        "com",
        "example",
        "openvideo",
        "ui",
        "home",
        "HomeFragment.kt"
    )

    private fun homeLayoutSource(): Path = sourcePath("main", "res", "layout", "fragment_home.xml")

    private fun sourcePath(vararg segments: String): Path {
        val relativePath = Paths.get("src", *segments)
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
