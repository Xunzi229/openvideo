package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeSmartListSourceTest {

    @Test
    fun homeViewModelExposesSmartListSectionsFromScannedVideosAndHistory() {
        val source = String(Files.readAllBytes(sourceFile("HomeViewModel.kt")))

        assertTrue(source.contains("val smartLists: StateFlow<List<HomeSmartListSection>>"))
        assertTrue(source.contains("combine("))
        assertTrue(source.contains("_videos,"))
        assertTrue(source.contains("repository.getHistory()"))
        assertTrue(source.contains("HomeSmartListBuilder.build("))
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
