package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeRecentSortUiSourceTest {

    @Test
    fun recentCategoryHidesOnlySortControlsButKeepsLayoutToggle() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("updateSortControlsVisibility(category)"))
        assertTrue(source.contains("val hideSortControls = category == HomeCategory.RECENT"))
        assertTrue(source.contains("sortLabel.visibility = if (hideSortControls) View.GONE else View.VISIBLE"))
        assertTrue(source.contains("btnSortOrder.visibility = if (hideSortControls) View.GONE else View.VISIBLE"))
        assertTrue(source.contains("sortRow.visibility = View.VISIBLE"))
    }

    private fun sourceFile(): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", "HomeFragment.kt")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
