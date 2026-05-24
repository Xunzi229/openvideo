package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeCategoryScrollSourceTest {

    @Test
    fun categoryChipsRouteThroughScrollToTopHandler() {
        val source = String(Files.readAllBytes(homeFragmentSource()))

        assertTrue(source.contains("chipAll.setOnClickListener { switchCategory(HomeCategory.ALL) }"))
        assertTrue(source.contains("chipRecent.setOnClickListener { switchCategory(HomeCategory.RECENT) }"))
        assertTrue(source.contains("chipFavorite.setOnClickListener { switchCategory(HomeCategory.FAVORITES) }"))
    }

    @Test
    fun categoryPagesUseIndependentRecyclerViewsAndAdapters() {
        val fragmentSource = String(Files.readAllBytes(homeFragmentSource()))
        val layoutSource = String(Files.readAllBytes(homeLayoutSource()))

        assertTrue(layoutSource.contains("android:id=\"@+id/recycler_all_videos\""))
        assertTrue(layoutSource.contains("android:id=\"@+id/recycler_recent_videos\""))
        assertTrue(layoutSource.contains("android:id=\"@+id/recycler_favorite_videos\""))
        assertTrue(fragmentSource.contains("private val adapters = mutableMapOf<HomeCategory, VideoGridAdapter>()"))
        assertTrue(fragmentSource.contains("private val recyclerViews = mutableMapOf<HomeCategory, RecyclerView>()"))
        assertTrue(fragmentSource.contains("initCategoryList(HomeCategory.ALL"))
        assertTrue(fragmentSource.contains("initCategoryList(HomeCategory.RECENT"))
        assertTrue(fragmentSource.contains("initCategoryList(HomeCategory.FAVORITES"))
    }

    @Test
    fun categorySwitchDoesNotForceOtherPagesToTop() {
        val source = String(Files.readAllBytes(homeFragmentSource()))
        val method = source.substringAfter("private fun switchCategory(category: HomeCategory) {")
            .substringBefore("\n    private fun")

        assertTrue(method.contains("viewModel.setCategory(category)"))
        assertFalse(method.contains("scrollToPosition"))
    }

    @Test
    fun categoryChipsShowFilteredCountsForEachCategory() {
        val source = String(Files.readAllBytes(homeFragmentSource()))
        val submitBody = source.substringAfter("private fun submitCategoryList(category: HomeCategory, list: List<VideoItem>) {")
            .substringBefore("\n    private fun")
        val updateBody = source.substringAfter("private fun updateCategoryChips(category: HomeCategory) {")
            .substringBefore("\n    private fun")
        val bindBody = source.substringAfter("private fun bindCategoryChip(")
            .substringBefore("\n    private fun")

        assertTrue(submitBody.contains("updateCategoryChips(activeCategory)"))
        assertTrue(updateBody.contains("categoryLists[HomeCategory.ALL].orEmpty().size"))
        assertTrue(updateBody.contains("categoryLists[HomeCategory.RECENT].orEmpty().size"))
        assertTrue(updateBody.contains("categoryLists[HomeCategory.FAVORITES].orEmpty().size"))
        assertTrue(bindBody.contains("R.string.home_filter_count"))
    }

    private fun homeFragmentSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "home",
            "HomeFragment.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun homeLayoutSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "layout",
            "fragment_home.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
