package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeSmartListFilterSourceTest {

    @Test
    fun homeViewModelAppliesSelectedSmartListToAllCategoryOnly() {
        val source = sourceText("HomeViewModel.kt")

        assertTrue(source.contains("private val _selectedSmartListType = MutableStateFlow<MediaSmartListType?>(null)"))
        assertTrue(source.contains("val selectedSmartListType: StateFlow<MediaSmartListType?> = _selectedSmartListType"))
        assertTrue(source.contains("private val smartFilteredAllCategoryVideos"))
        assertTrue(source.contains("selectedSmartListType"))
        assertTrue(source.contains("section.type == selectedType"))
        assertTrue(source.contains("fun setSmartListFilter(type: MediaSmartListType?)"))
        assertTrue(source.contains("_selectedSmartListType.value = type"))
        assertTrue(source.contains("hasActiveUserFilter = selectedFolderKey != null || smartListIsActive ||"))
    }

    @Test
    fun homeViewModelResetsSmartListFilterOutsideAllCategory() {
        val source = sourceText("HomeViewModel.kt")

        assertTrue(source.contains("if (category != HomeCategory.ALL)"))
        assertTrue(source.contains("_selectedSmartListType.value = null"))
        assertTrue(source.contains("val smartListIsActive = category == HomeCategory.ALL && selectedSmartListType != null"))
        assertTrue(source.contains("hasActiveUserFilter = selectedFolderKey != null || smartListIsActive ||"))
    }

    @Test
    fun homeFragmentBindsSmartListChipsFromViewModel() {
        val source = sourceText("HomeFragment.kt")
        val layout = layoutText("fragment_home.xml")

        assertTrue(layout.contains("@+id/smart_filter_scroll"))
        assertTrue(layout.contains("@+id/smart_filter_group"))
        assertTrue(source.contains("private lateinit var smartFilterScroll: View"))
        assertTrue(source.contains("private lateinit var smartFilterGroup: ChipGroup"))
        assertTrue(source.contains("viewModel.smartLists.collect { sections ->"))
        assertTrue(source.contains("viewModel.selectedSmartListType.collect { type ->"))
        assertTrue(source.contains("bindSmartListChips()"))
        assertTrue(source.contains("viewModel.setSmartListFilter(null)"))
        assertTrue(source.contains("viewModel.setSmartListFilter(section.type)"))
        assertTrue(source.contains("HomeSmartListLabels.labelRes(section.type)"))
    }

    private fun sourceText(name: String): String = String(Files.readAllBytes(sourceFile(name)))

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun layoutText(name: String): String = String(Files.readAllBytes(layoutFile(name)))

    private fun layoutFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
