package com.example.openvideo.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeRecentSourceTest {

    @Test
    fun recentCategoryUsesFixedLatestPlaybackOrderWithoutGlobalSortControls() {
        val source = String(Files.readAllBytes(homeViewModelSource()))
        val recentVideosBody = source
            .substringAfter("val recentVideos: StateFlow<List<VideoItem>> = ")
            .substringBefore("\n\n")

        assertTrue(recentVideosBody.contains("filteredRecentVideos(recentCategoryVideos)"))
        assertFalse(recentVideosBody.contains("filteredSortedVideos(recentCategoryVideos)"))
    }

    @Test
    fun filteredRecentVideosDoesNotDependOnSortFieldOrSortOrder() {
        val source = String(Files.readAllBytes(homeViewModelSource()))
        val methodBody = source
            .substringAfter("private fun filteredRecentVideos(source: Flow<List<VideoItem>>): StateFlow<List<VideoItem>> = combine(")
            .substringBefore("}.stateIn")

        assertFalse(methodBody.contains("_sortField"))
        assertFalse(methodBody.contains("_sortAsc"))
        assertFalse(methodBody.contains("SortField.NAME"))
        assertFalse(methodBody.contains("SortField.SIZE"))
        assertFalse(methodBody.contains("SortField.DURATION"))
        assertFalse(methodBody.contains("SortField.DATE"))
        assertTrue(methodBody.contains("if (query.isBlank()) folderFiltered"))
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
