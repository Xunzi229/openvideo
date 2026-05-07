package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
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
    fun categorySwitchScrollsRecyclerViewToTopEveryTime() {
        val source = String(Files.readAllBytes(homeFragmentSource()))
        val method = source.substringAfter("private fun switchCategory(category: HomeCategory) {")
            .substringBefore("\n    private fun")

        assertTrue(method.contains("viewModel.setCategory(category)"))
        assertTrue(method.contains("recyclerView.scrollToPosition(0)"))
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
}
