package com.example.openvideo.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AppleLibraryChromeSourceTest {

    @Test
    fun libraryThemeUsesStaticPressedHighlightAndPlayerKeepsRipple() {
        val theme = rootFile("app", "src", "main", "res", "values", "themes.xml").readText()
        val tokens = rootFile("app", "src", "main", "res", "values", "design_tokens.xml").readText()
        val badge = rootFile("app", "src", "main", "res", "drawable", "bg_update_badge_dot.xml").readText()

        assertTrue(theme.contains("@drawable/bg_ov_pressed"))
        assertTrue(theme.contains("@drawable/bg_player_ripple"))
        assertTrue(theme.contains("TextAppearance.OpenVideo.NavTitle"))
        assertTrue(tokens.contains("ov_row_pressed"))
        assertTrue(tokens.contains("<dimen name=\"ov_row_height\">48dp</dimen>"))
        assertTrue(badge.contains("@color/ov_danger"))
        assertFalse(badge.contains("#FFFF4444"))
    }

    @Test
    fun phoneTabsKeepAliveAndSupportEdgeSwipeBack() {
        val activity = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "MainActivity.kt"
        ).readText()
        val layout = rootFile("app", "src", "main", "res", "layout", "activity_main.xml").readText()
        val navigator = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "LibraryNavigator.kt"
        ).readText()
        val tabHost = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "LibraryTabHostFragment.kt"
        ).readText()

        assertTrue(layout.contains("phone_tab_host"))
        assertTrue(layout.contains("LibrarySwipeFrameLayout"))
        assertTrue(layout.contains("@+id/side_nav"))
        assertTrue(activity.contains("LibraryTabHostFragment.newInstance"))
        assertTrue(activity.contains("showTab(R.id.nav_home)"))
        assertTrue(activity.contains("breakpoint.isAtLeastMedium"))
        assertTrue(navigator.contains("fun push(from: Fragment, target: Fragment, name: String? = null)"))
        assertTrue(navigator.contains("R.id.tab_child_container"))
        assertTrue(navigator.contains("setReorderingAllowed(true)"))
        assertTrue(navigator.contains(".hide(from)"))
        assertTrue(navigator.contains(".add(from.libraryContainerId(), target)"))
        assertFalse(navigator.contains(".replace(from.libraryContainerId(), target)"))
        assertTrue(navigator.contains("pendingPushManagers"))
        assertTrue(navigator.contains("addOnBackStackChangedListener"))
        assertTrue(navigator.contains("removeOnBackStackChangedListener"))
        assertFalse(navigator.contains("runOnCommit"))
        assertTrue(activity.contains("setOnItemReselectedListener"))
        assertTrue(activity.contains("currentTabHost()?.popToRoot()"))
        assertFalse(activity.contains("while (host.canPop())"))
        assertTrue(tabHost.contains("fun popToRoot(): Boolean"))
        assertTrue(tabHost.contains("popBackStackImmediate("))
        assertTrue(tabHost.contains("FragmentManager.POP_BACK_STACK_INCLUSIVE"))
    }

    @Test
    fun libraryPushUsesCoveringSlideInsteadOfShortParallax() {
        val enter = rootFile("app", "src", "main", "res", "anim", "ov_slide_in_right.xml").readText()
        val exit = rootFile("app", "src", "main", "res", "anim", "ov_slide_out_left.xml").readText()
        val popEnter = rootFile("app", "src", "main", "res", "anim", "ov_slide_in_left.xml").readText()
        val popExit = rootFile("app", "src", "main", "res", "anim", "ov_slide_out_right.xml").readText()

        assertTrue(enter.contains("android:fromXDelta=\"100%p\""))
        assertTrue(enter.contains("android:zAdjustment=\"top\""))
        assertTrue(exit.contains("android:toXDelta=\"-30%p\""))
        assertTrue(popEnter.contains("android:fromXDelta=\"-30%p\""))
        assertTrue(popExit.contains("android:toXDelta=\"100%p\""))
        assertTrue(popExit.contains("android:zAdjustment=\"top\""))
        assertFalse(enter.contains("24%"))
        assertFalse(exit.contains("-12%"))
    }

    @Test
    fun homeFiltersAndSettingsNotificationsDropMaterialChips() {
        val home = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "home", "HomeFragment.kt"
        ).readText()
        val homeLayout = rootFile("app", "src", "main", "res", "layout", "fragment_home.xml").readText()
        val filter = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "home", "VideoLibraryFilterPopover.kt"
        ).readText()
        val settings = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "settings", "SettingsFragment.kt"
        ).readText()

        assertFalse(home.contains("import com.google.android.material.chip.Chip"))
        assertFalse(homeLayout.contains("TabLayout"))
        assertFalse(homeLayout.contains("com.google.android.material.chip.ChipGroup"))
        assertTrue(home.contains("AppleFilterChrome.pill"))
        assertTrue(filter.contains("AppleFilterChrome.optionRow"))
        assertFalse(filter.contains("com.google.android.material.chip.Chip"))
        assertTrue(settings.contains("LibraryNavigator.push(this, NotificationSettingsFragment())"))
    }

    private fun Path.readText(): String = String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
