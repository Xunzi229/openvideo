package com.example.openvideo.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeRecentSourceTest {

    @Test
    fun recentCategoryDefaultsToLatestPlaybackButStillUsesSortControls() {
        val source = String(Files.readAllBytes(homeViewModelSource()))
        val setCategoryBody = source
            .substringAfter("fun setCategory(category: HomeCategory) {")
            .substringBefore("\n    }")
        val videosCombineBody = source
            .substringAfter("val videos: StateFlow<List<VideoItem>> = combine(")
            .substringBefore(".stateIn")

        assertTrue(
            "Recent category should reset to date sorting so latest playback appears first by default",
            setCategoryBody.contains("if (category == HomeCategory.RECENT)")
                && setCategoryBody.contains("_sortField.value = SortField.DATE")
                && setCategoryBody.contains("_sortAsc.value = false")
        )
        assertFalse(
            "Recent playback should still flow through normal sorting so asc/desc and sort-field controls work",
            videosCombineBody.contains("return@combine filtered")
        )
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
}
