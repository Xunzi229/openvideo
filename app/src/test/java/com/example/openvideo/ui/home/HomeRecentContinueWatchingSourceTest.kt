package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeRecentContinueWatchingSourceTest {

    @Test
    fun homeViewModelBuildsRecentContinueWatchingBadgesFromHistory() {
        val source = String(Files.readAllBytes(homeViewModelSource()))

        assertTrue(source.contains("val recentContinueWatchingBadges"))
        assertTrue(source.contains("HistoryContinueWatchingPolicy.buildItems("))
        assertTrue(source.contains("ContinueWatchingBadge("))
    }

    @Test
    fun homeFragmentInjectsBadgesOnlyIntoRecentAdapter() {
        val source = String(Files.readAllBytes(homeFragmentSource()))

        assertTrue(source.contains("viewModel.recentContinueWatchingBadges.collect { badges ->"))
        assertTrue(source.contains("adapters.getValue(HomeCategory.RECENT).continueWatchingBadges = badges"))
    }

    @Test
    fun videoGridAdapterRendersContinueWatchingBadgesWhenProvided() {
        val source = String(Files.readAllBytes(videoGridAdapterSource()))

        assertTrue(source.contains("var continueWatchingBadges: Map<Long, ContinueWatchingBadge> = emptyMap()"))
        assertTrue(source.contains("val continueWatchingBadge = continueWatchingBadges[item.id]"))
        assertTrue(source.contains("holder.size?.text = continueWatchingBadge?.progressLabel ?: formatSize(item.size)"))
        assertTrue(source.contains("holder.resolution?.text = continueWatchingBadge?.watchedTimeLabel ?: \"\${item.width}x\${item.height}\""))
        assertTrue(source.contains("continueWatchingBadge?.isAvailable != false"))
    }

    private fun homeViewModelSource(): Path = sourcePath("HomeViewModel.kt")

    private fun homeFragmentSource(): Path = sourcePath("HomeFragment.kt")

    private fun videoGridAdapterSource(): Path = sourcePath("VideoGridAdapter.kt")

    private fun sourcePath(fileName: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", fileName)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
