package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeHistoryCleanupSourceTest {

    @Test
    fun homeViewModelPrunesStaleHistoryAfterPublishedScan() {
        val source = String(Files.readAllBytes(homeViewModelSource()))

        assertTrue(source.contains("repository.pruneStaleHistory(list)"))
    }

    @Test
    fun localFolderViewModelPrunesStaleHistoryAfterPublishedScan() {
        val source = String(Files.readAllBytes(localFolderViewModelSource()))

        assertTrue(source.contains("repository.pruneStaleHistory(videos)"))
    }

    @Test
    fun repositoryDelegatesStaleHistoryRemovalToCleanupPolicy() {
        val source = String(Files.readAllBytes(repositorySource()))

        assertTrue(source.contains("HistoryCleanupPolicy.videoIdsToRemove"))
        assertTrue(source.contains("historyDao.delete(it)"))
    }

    private fun homeViewModelSource(): Path = moduleSource("ui", "home", "HomeViewModel.kt")

    private fun localFolderViewModelSource(): Path =
        moduleSource("ui", "local", "LocalFolderViewModel.kt")

    private fun repositorySource(): Path =
        moduleSource("data", "repository", "VideoRepository.kt")

    private fun moduleSource(vararg segments: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", *segments)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
