package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeMediaScanSourceTest {

    @Test
    fun homeViewModelHandlesScanOutcomes() {
        val source = String(Files.readAllBytes(homeViewModelSource()))

        assertTrue(source.contains("VideoScanOutcome.PermissionDenied"))
        assertTrue(source.contains("is VideoScanOutcome.Success"))
        assertTrue(source.contains("is VideoScanOutcome.Error"))
        assertTrue(source.contains("is VideoScanOutcome.Progress"))
        assertTrue(source.contains("scanProgress"))
        assertTrue(source.contains("MediaLibraryScanProgress"))
    }

    @Test
    fun homeFragmentShowsPermissionAndScanErrorStates() {
        val source = String(Files.readAllBytes(homeFragmentSource()))
        val layout = String(Files.readAllBytes(homeLayoutSource()))

        assertTrue(source.contains("MediaLibraryEmptyState.PERMISSION_DENIED"))
        assertTrue(source.contains("MediaLibraryEmptyState.SCAN_ERROR"))
        assertTrue(source.contains("checkPermissionAndLoad()"))
        assertTrue(source.contains("MediaLibraryScanLoadingUi.bind"))
        assertTrue(layout.contains("include_media_library_scan_loading"))
    }

    private fun homeLayoutSource(): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", "fragment_home.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    @Test
    fun videoRepositorySharesSingleScannerFlow() {
        val source = String(Files.readAllBytes(repositorySource()))

        assertTrue(source.contains("shareIn"))
        assertTrue(source.contains("sharedScanResults"))
    }

    private fun repositorySource(): Path = moduleSource("data", "repository", "VideoRepository.kt")

    private fun homeViewModelSource(): Path = moduleSource("ui", "home", "HomeViewModel.kt")

    private fun homeFragmentSource(): Path = moduleSource("ui", "home", "HomeFragment.kt")

    private fun moduleSource(vararg segments: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", *segments)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
