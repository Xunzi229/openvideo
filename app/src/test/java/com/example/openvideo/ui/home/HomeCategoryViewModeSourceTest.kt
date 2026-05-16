package com.example.openvideo.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeCategoryViewModeSourceTest {

    @Test
    fun homeViewModelStoresIndependentViewModesPerCategory() {
        val source = String(Files.readAllBytes(sourceFile("HomeViewModel.kt")))

        assertTrue(source.contains("private val _categoryViewModes = MutableStateFlow(loadCategoryViewModes())"))
        assertTrue(source.contains("val categoryViewModes: StateFlow<Map<HomeCategory, ViewMode>> = _categoryViewModes"))
        assertTrue(source.contains("val currentCategory = _category.value"))
        assertTrue(source.contains("_categoryViewModes.value = _categoryViewModes.value + (currentCategory to mode)"))
        assertTrue(source.contains("saveViewModeForCategory(currentCategory, mode)"))
        assertTrue(source.contains("private fun loadCategoryViewModes(): Map<HomeCategory, ViewMode>"))
        assertTrue(source.contains("private fun saveViewModeForCategory(category: HomeCategory, mode: ViewMode)"))
    }

    @Test
    fun homeViewModelNoLongerReadsSingleGlobalViewModePreference() {
        val source = String(Files.readAllBytes(sourceFile("HomeViewModel.kt")))

        assertFalse(source.contains("MutableStateFlow(viewModeFromPrefs(appPrefs.viewMode))"))
        assertFalse(source.contains("appPrefs.viewMode = mode.name.lowercase()"))
    }

    @Test
    fun appPrefsExposeDedicatedViewModeKeysForEachHomeCategory() {
        val source = String(Files.readAllBytes(appPrefsSource()))

        assertTrue(source.contains("var homeAllViewMode: String"))
        assertTrue(source.contains("var homeRecentViewMode: String"))
        assertTrue(source.contains("var homeFavoriteViewMode: String"))
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun appPrefsSource(): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "core", "prefs", "AppPrefs.kt")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
