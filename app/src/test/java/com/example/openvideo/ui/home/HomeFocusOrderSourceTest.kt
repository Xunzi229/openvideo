package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeFocusOrderSourceTest {

    @Test
    fun homeFragmentWiresStaticHeaderAndFilterFocusOrder() {
        val source = String(Files.readAllBytes(homeFragmentSource()))

        assertTrue(source.contains("configureHomeFocusOrder(view)"))
        assertTrue(source.contains("private fun configureHomeFocusOrder(view: View)"))
        assertTrue(source.contains("view.findViewById<View>(R.id.btn_open_url).nextFocusRightId = R.id.btn_refresh"))
        assertTrue(source.contains("view.findViewById<View>(R.id.btn_refresh).nextFocusLeftId = R.id.btn_open_url"))
        assertTrue(source.contains("searchView.nextFocusUpId = R.id.btn_open_url"))
        assertTrue(source.contains("searchView.nextFocusDownId = R.id.chip_all"))
        assertTrue(source.contains("chipAll.nextFocusRightId = R.id.chip_recent"))
        assertTrue(source.contains("chipRecent.nextFocusLeftId = R.id.chip_all"))
        assertTrue(source.contains("chipRecent.nextFocusRightId = R.id.chip_favorite"))
        assertTrue(source.contains("chipFavorite.nextFocusLeftId = R.id.chip_recent"))
        assertTrue(source.contains("chipAll.nextFocusDownId = R.id.tv_sort_label"))
        assertTrue(source.contains("sortLabel.isFocusable = true"))
    }

    @Test
    fun homeFragmentPointsSortControlsAtVisibleCategoryList() {
        val source = String(Files.readAllBytes(homeFragmentSource()))

        assertTrue(source.contains("refreshHomeContentFocusOrder(category)"))
        assertTrue(source.contains("private fun refreshHomeContentFocusOrder(category: HomeCategory = activeCategory)"))
        assertTrue(source.contains("val recyclerId = activeRecyclerViewId(category)"))
        assertTrue(source.contains("control.nextFocusDownId = contentFocusTargetId"))
        assertTrue(source.contains("recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS"))
        assertTrue(source.contains("recyclerView.nextFocusUpId = R.id.btn_list_view"))
        assertTrue(source.contains("private fun activeRecyclerViewId(category: HomeCategory): Int = when (category)"))
        assertTrue(source.contains("HomeCategory.ALL -> R.id.recycler_all_videos"))
        assertTrue(source.contains("HomeCategory.RECENT -> R.id.recycler_recent_videos"))
        assertTrue(source.contains("HomeCategory.FAVORITES -> R.id.recycler_favorite_videos"))
    }

    @Test
    fun homeEmptyStateKeepsDpadPathToExistingActions() {
        val source = String(Files.readAllBytes(homeFragmentSource()))
        val layout = String(Files.readAllBytes(homeLayoutSource()))

        assertTrue(source.contains("emptyView.isFocusable = true"))
        assertTrue(source.contains("emptyView.nextFocusUpId = R.id.btn_refresh"))
        assertTrue(source.contains("val contentFocusTargetId = if (categoryLists[category].orEmpty().isEmpty()) R.id.btn_refresh else recyclerId"))
        assertTrue(source.contains("control.nextFocusDownId = contentFocusTargetId"))
        assertTrue(source.contains("refreshHomeContentFocusOrder(activeCategory)"))
        assertTrue(layout.contains("""android:foreground="@drawable/bg_focusable_card""""))
    }

    @Test
    fun homeDynamicFilterChipsHaveExplicitDpadOrder() {
        val source = String(Files.readAllBytes(homeFragmentSource()))

        assertTrue(source.contains("refreshDynamicFilterChipFocusOrder()"))
        assertTrue(source.contains("private fun refreshDynamicFilterChipFocusOrder()"))
        assertTrue(source.contains("val smartChips = dynamicChipChildren(smartFilterGroup)"))
        assertTrue(source.contains("val folderChips = dynamicChipChildren(folderGroup)"))
        assertTrue(source.contains("chipAll.nextFocusDownId = firstDynamicChipId"))
        assertTrue(source.contains("chipRecent.nextFocusDownId = firstDynamicChipId"))
        assertTrue(source.contains("chipFavorite.nextFocusDownId = firstDynamicChipId"))
        assertTrue(source.contains("wireDynamicChipRow(smartChips,"))
        assertTrue(source.contains("wireDynamicChipRow(folderChips,"))
        assertTrue(source.contains("private fun dynamicChipChildren(group: ChipGroup): List<Chip>"))
        assertTrue(source.contains("if (chip.id == View.NO_ID) chip.id = View.generateViewId()"))
        assertTrue(source.contains("private fun wireDynamicChipRow(chips: List<Chip>, nextUpId: Int, nextDownId: Int)"))
        assertTrue(source.contains("chip.nextFocusLeftId = chips.getOrNull(index - 1)?.id ?: chip.id"))
        assertTrue(source.contains("chip.nextFocusRightId = chips.getOrNull(index + 1)?.id ?: chip.id"))
        assertTrue(source.contains("chip.nextFocusUpId = nextUpId"))
        assertTrue(source.contains("chip.nextFocusDownId = nextDownId"))
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
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun homeLayoutSource(): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", "fragment_home.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
