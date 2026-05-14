package com.example.openvideo.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeSortControlSourceTest {

    @Test
    fun sortLabelChangesFieldAndArrowButtonTogglesSortOrder() {
        val source = String(Files.readAllBytes(sourceFile("java", "HomeFragment.kt")))

        assertTrue(source.contains("private lateinit var btnSortOrder: ImageButton"))
        assertTrue(source.contains("sortLabel.setOnClickListener { changeSortFieldAndScrollToTop() }"))
        assertTrue(source.contains("btnSortOrder.setOnClickListener { toggleSortOrderAndScrollToTop() }"))
    }

    @Test
    fun sortChangesScrollVideoListBackToTop() {
        val source = String(Files.readAllBytes(sourceFile("java", "HomeFragment.kt")))
        val changeField = source.substringAfter("private fun changeSortFieldAndScrollToTop() {")
            .substringBefore("\n    private fun")
        val toggleOrder = source.substringAfter("private fun toggleSortOrderAndScrollToTop() {")
            .substringBefore("\n    private fun")
        val requestScroll = source.substringAfter("private fun requestVideoListJumpToTop() {")
            .substringBefore("\n    private fun")
        val submit = source.substringAfter("private fun submitCategoryList(")
            .substringBefore("\n    private fun")

        assertTrue(changeField.contains("viewModel.cycleSortField()"))
        assertTrue(changeField.contains("requestVideoListJumpToTop()"))
        assertTrue(toggleOrder.contains("viewModel.toggleSortOrder()"))
        assertTrue(toggleOrder.contains("requestVideoListJumpToTop()"))
        assertTrue(requestScroll.contains("pendingJumpToTopCategory = activeCategory"))
        assertTrue(submit.contains("jumpVideoListToTopIfNeeded(category)"))
    }

    @Test
    fun sortJumpToTopIsImmediateWithoutSmoothScrolling() {
        val source = String(Files.readAllBytes(sourceFile("java", "HomeFragment.kt")))
        val jumpMethod = source.substringAfter("private fun jumpVideoListToTopIfNeeded(category: HomeCategory) {")
            .substringBefore("\n    private fun")

        assertTrue(jumpMethod.contains("pendingJumpToTopCategory = null"))
        assertTrue(jumpMethod.contains("scrollToPositionWithOffset(0, 0)"))
        assertFalse(
            "Sorting should jump immediately to the top instead of animating a long smooth scroll",
            jumpMethod.contains("smoothScrollToPosition")
        )
    }

    @Test
    fun sortArrowButtonSitsBesideSortLabel() {
        val source = String(Files.readAllBytes(sourceFile("res", "fragment_home.xml")))
        val label = source.substringAfter("""android:id="@+id/tv_sort_label"""")
            .substringBefore("/>")
        val arrow = source.substringAfter("""android:id="@+id/btn_sort_order"""")
            .substringBefore("/>")

        assertTrue(label.contains("""android:layout_width="wrap_content""""))
        assertFalse(label.contains("""android:layout_weight="1""""))
        assertTrue(arrow.contains("""android:layout_width="40dp""""))
        assertTrue(arrow.contains("""android:layout_height="40dp""""))
        assertTrue(arrow.contains("""android:layout_marginStart="2dp""""))
        assertTrue(arrow.contains("""android:padding="8dp""""))
    }

    @Test
    fun sortArrowIconsAreLargeEnoughForTheSortControl() {
        val up = String(Files.readAllBytes(drawableFile("ic_arrow_up.xml")))
        val down = String(Files.readAllBytes(drawableFile("ic_arrow_down.xml")))

        assertTrue(up.contains("""android:width="22dp""""))
        assertTrue(up.contains("""android:height="22dp""""))
        assertTrue(down.contains("""android:width="22dp""""))
        assertTrue(down.contains("""android:height="22dp""""))
    }

    private fun sourceFile(kind: String, name: String): Path {
        val relativePath = when (kind) {
            "java" -> Paths.get(
                "src", "main", "java", "com", "example", "openvideo", "ui", "home", name
            )
            else -> Paths.get("src", "main", "res", "layout", name)
        }
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun drawableFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "res", "drawable", name)
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
