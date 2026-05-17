package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeSearchFilterSourceTest {

    @Test
    fun homeViewModelAppliesSearchPolicyAndAdvancedFilters() {
        val source = String(Files.readAllBytes(homeViewModelSource()))

        assertTrue(source.contains("MediaLibrarySearchPolicy.matchesLibrary"))
        assertTrue(source.contains("applyLibraryFilters"))
        assertTrue(source.contains("_advancedFilters"))
        assertTrue(source.contains("setAdvancedFilters"))
        assertTrue(source.contains("clearAdvancedFilters"))
    }

    @Test
    fun homeFragmentExposesAdvancedFilterEntryPoint() {
        val source = String(Files.readAllBytes(homeFragmentSource()))
        val layout = String(Files.readAllBytes(homeLayoutSource()))
        val popoverLayout = String(
            Files.readAllBytes(
                moduleResPath("layout", "view_video_library_filter_popover.xml")
            )
        )

        assertTrue(layout.contains("@+id/btn_library_filter"))
        assertTrue(source.contains("VideoLibraryFilterPopover"))
        assertTrue(source.contains("toggleAdvancedFilterPopover"))
        assertTrue(popoverLayout.contains("@+id/chip_group_duration"))
        assertTrue(popoverLayout.contains("@+id/btn_filter_apply"))
    }

    private fun moduleResPath(vararg segments: String): Path {
        val relativePath = Paths.get("src", "main", "res", *segments)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun homeViewModelSource(): Path = moduleSource("ui", "home", "HomeViewModel.kt")

    private fun homeFragmentSource(): Path = moduleSource("ui", "home", "HomeFragment.kt")

    private fun homeLayoutSource(): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", "fragment_home.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun moduleSource(vararg segments: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", *segments)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
