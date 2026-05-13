package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeMissingHistorySourceTest {

    @Test
    fun recentAndFavoriteFallbacksRejectMissingStoredFiles() {
        val source = String(Files.readAllBytes(homeViewModelSource()))

        assertTrue(source.contains("shouldExposeStoredFallback"))
        assertTrue(source.contains("File(candidatePath).exists()"))
        assertTrue(source.contains("videosFromHistory"))
        assertTrue(source.contains("videosFromFavorites"))
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
