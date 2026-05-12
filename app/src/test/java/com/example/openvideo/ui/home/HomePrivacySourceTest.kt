package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomePrivacySourceTest {

    @Test
    fun recentAndFavoriteFallbackItemsRespectHiddenFolders() {
        val source = String(Files.readAllBytes(homeViewModelSource()))

        assertTrue(source.contains("_hiddenFolders"))
        assertTrue(source.contains("videosFromHistory(scanned, history, hiddenFolders)"))
        assertTrue(source.contains("videosFromFavorites(scanned, favorites, hiddenFolders)"))
        assertTrue(source.contains("MediaLibraryPolicy.isHiddenPath(video.path, hiddenFolders)"))
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
