package com.example.openvideo.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class BrowseAdaptiveLayoutSourceTest {

    @Test
    fun folderAndSeriesPagesUseBreakpointGridLayoutManagers() {
        listOf(
            sourceFile("ui", "local", "LocalFolderFragment.kt"),
            sourceFile("ui", "local", "FolderVideosFragment.kt"),
            sourceFile("ui", "series", "SeriesListFragment.kt"),
            sourceFile("ui", "series", "SeriesDetailFragment.kt")
        ).forEach { file ->
            val source = String(Files.readAllBytes(file))

            assertTrue("${file.fileName} must use GridLayoutManager", source.contains("import androidx.recyclerview.widget.GridLayoutManager"))
            assertTrue("${file.fileName} must use shared span policy", source.contains("BrowseAdaptiveLayoutPolicy.contentSpanCount(currentBreakpoint())"))
            assertTrue("${file.fileName} must read MainActivity breakpoint", source.contains("(activity as? MainActivity)?.breakpoint ?: ScreenBreakpoint.COMPACT"))
            assertFalse("${file.fileName} must not keep fixed single-column manager", source.contains("LinearLayoutManager(requireContext())"))
        }
    }

    @Test
    fun folderVideoPageSwitchesVideoCardsOnlyWhenMultipleColumnsAreActive() {
        val source = String(Files.readAllBytes(sourceFile("ui", "local", "FolderVideosFragment.kt")))

        assertTrue(source.contains("adapter.viewMode = if (spanCount > 1) ViewMode.GRID else ViewMode.LIST"))
    }

    private fun sourceFile(vararg parts: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", *parts)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
