package com.example.openvideo.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeCategoryViewModeUiSourceTest {

    @Test
    fun homeFragmentAppliesViewModePerCategoryInsteadOfBroadcastingOneModeToAllLists() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("viewModel.categoryViewModes.collect { modes ->"))
        assertTrue(source.contains("applyCategoryViewModes(modes)"))
        assertTrue(source.contains("private fun applyCategoryViewModes(modes: Map<HomeCategory, ViewMode>)"))
        assertTrue(source.contains("private fun applyViewMode(category: HomeCategory, mode: ViewMode)"))
        assertFalse(source.contains("adapters.values.forEach { adapter -> adapter.viewMode = mode }"))
        assertFalse(source.contains("recyclerViews.values.forEach { recyclerView ->\n                            val spanCount = if (mode == ViewMode.GRID) 2 else 1"))
    }

    @Test
    fun homeFragmentRefreshesToggleButtonsFromActiveCategoryViewMode() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("updateViewModeButtons(modes[activeCategory] ?: ViewMode.LIST)"))
        assertTrue(source.contains("updateViewModeButtons(viewModel.categoryViewModes.value[category] ?: ViewMode.LIST)"))
    }

    private fun sourceFile(): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", "HomeFragment.kt")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
