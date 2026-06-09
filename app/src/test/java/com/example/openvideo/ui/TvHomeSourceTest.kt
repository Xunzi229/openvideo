package com.example.openvideo.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TvHomeSourceTest {

    @Test
    fun mainActivityLoadsTvHomeAndHidesPhoneBottomNavInTvMode() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "MainActivity.kt")
        )

        assertTrue(source.contains("import androidx.core.view.isVisible"))
        assertTrue(source.contains("import com.example.openvideo.ui.tv.TvHomeFragment"))
        assertTrue(source.contains("bottomNav.isVisible = !isTvMode"))
        assertTrue(source.contains("loadFragment(if (isTvMode) TvHomeFragment() else LocalFolderFragment())"))
    }

    @Test
    fun tvHomeFragmentRoutesLargeCardsToExistingScreens() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("R.layout.fragment_tv_home"))
        assertTrue(source.contains("R.id.tv_card_continue"))
        assertTrue(source.contains("R.id.tv_card_folders"))
        assertTrue(source.contains("R.id.tv_card_series"))
        assertTrue(source.contains("R.id.tv_card_sources"))
        assertTrue(source.contains("R.id.tv_card_settings"))
        assertTrue(source.contains("HomeFragment.newInstance(HomeCategory.RECENT)"))
        assertTrue(source.contains("LocalFolderFragment()"))
        assertTrue(source.contains("SeriesListFragment()"))
        assertTrue(source.contains("SourcesFragment()"))
        assertTrue(source.contains("SettingsFragment()"))
        assertTrue(source.contains("requestFocus()"))
    }

    @Test
    fun tvHomeLayoutUsesFocusableTenFootCards() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))

        listOf(
            "tv_card_continue",
            "tv_card_folders",
            "tv_card_series",
            "tv_card_sources",
            "tv_card_settings"
        ).forEach { id ->
            assertTrue(layout.contains("""android:id="@+id/$id""""))
        }
        assertTrue(layout.contains("""android:minHeight="112dp""""))
        assertTrue(layout.contains("""android:focusable="true""""))
        assertTrue(layout.contains("""android:foreground="@drawable/bg_focusable_card""""))
        assertTrue(layout.contains("""@string/tv_home_title"""))
    }

    @Test
    fun tvHomeLayoutDefinesExplicitDpadFocusOrder() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))

        listOf(
            """android:nextFocusRight="@id/tv_card_folders"""",
            """android:nextFocusDown="@id/tv_card_series"""",
            """android:nextFocusLeft="@id/tv_card_continue"""",
            """android:nextFocusDown="@id/tv_card_sources"""",
            """android:nextFocusUp="@id/tv_card_continue"""",
            """android:nextFocusRight="@id/tv_card_sources"""",
            """android:nextFocusUp="@id/tv_card_folders"""",
            """android:nextFocusLeft="@id/tv_card_series"""",
            """android:nextFocusRight="@id/tv_card_settings"""",
            """android:nextFocusLeft="@id/tv_card_sources""""
        ).forEach { focusRule ->
            assertTrue("Missing focus rule: $focusRule", layout.contains(focusRule))
        }
    }

    @Test
    fun tvHomeSeriesCardReusesSeriesPosterCoverWithFallback() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(layout.contains("""android:id="@+id/tv_home_series_cover""""))
        assertTrue(layout.contains("""android:scaleType="centerCrop""""))
        assertTrue(layout.contains("""android:src="@drawable/ic_movie""""))
        assertTrue(source.contains("@AndroidEntryPoint"))
        assertTrue(source.contains("import com.bumptech.glide.Glide"))
        assertTrue(source.contains("import com.example.openvideo.ui.series.SeriesListViewModel"))
        assertTrue(source.contains("private val seriesViewModel: SeriesListViewModel by viewModels()"))
        assertTrue(source.contains("Glide.with(seriesCover)"))
        assertTrue(source.contains(".load(posterModel(series.firstOrNull"))
        assertTrue(source.contains(".placeholder(R.drawable.ic_movie)"))
        assertTrue(source.contains(".fallback(R.drawable.ic_movie)"))
        assertTrue(source.contains(".error(R.drawable.ic_movie)"))
    }

    @Test
    fun tvHomeContinueCardReusesRecentVideoThumbnailWithFallback() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(layout.contains("""android:id="@+id/tv_home_continue_cover""""))
        assertTrue(layout.contains("""android:contentDescription="@string/tv_home_continue""""))
        assertTrue(layout.contains("""android:scaleType="centerCrop""""))
        assertTrue(layout.contains("""android:src="@drawable/ic_play""""))
        assertTrue(source.contains("import com.example.openvideo.ui.home.HomeViewModel"))
        assertTrue(source.contains("private val homeViewModel: HomeViewModel by viewModels()"))
        assertTrue(source.contains("bindContinueCover(view)"))
        assertTrue(source.contains("val continueCover = root.findViewById<ImageView>(R.id.tv_home_continue_cover)"))
        assertTrue(source.contains("homeViewModel.recentVideos.collect { recent ->"))
        assertTrue(source.contains(".load(firstRecent?.thumbnailUri)"))
        assertTrue(source.contains(".placeholder(R.drawable.ic_play)"))
        assertTrue(source.contains(".fallback(R.drawable.ic_play)"))
        assertTrue(source.contains(".error(R.drawable.ic_play)"))
    }

    @Test
    fun tvHomeContinueCardShowsRecentTitleAndCountSummary() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val strings = loadText(Paths.get("src", "main", "res", "values", "strings.xml"))

        assertTrue(layout.contains("""android:id="@+id/tv_home_continue_detail""""))
        assertTrue(layout.contains("""android:text="@string/tv_home_continue_desc""""))
        assertTrue(strings.contains("""name="tv_home_continue_recent_summary""""))
        assertTrue(source.contains("import android.widget.TextView"))
        assertTrue(source.contains("val continueDetail = root.findViewById<TextView>(R.id.tv_home_continue_detail)"))
        assertTrue(source.contains("val firstRecent = recent.firstOrNull()"))
        assertTrue(source.contains("continueDetail.text = if (firstRecent == null)"))
        assertTrue(source.contains("getString(R.string.tv_home_continue_desc)"))
        assertTrue(source.contains("getString(R.string.tv_home_continue_recent_summary, firstRecent.title, recent.size)"))
    }

    @Test
    fun tvHomeContinueCardOpensExistingRecentCategory() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val homeSource = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", "HomeFragment.kt")
        )

        assertTrue(source.contains("import com.example.openvideo.ui.home.HomeCategory"))
        assertTrue(source.contains("bindCard(view, R.id.tv_card_continue) { HomeFragment.newInstance(HomeCategory.RECENT) }"))
        assertTrue(homeSource.contains("fun newInstance(initialCategory: HomeCategory): HomeFragment"))
        assertTrue(homeSource.contains("putString(ARG_INITIAL_CATEGORY, initialCategory.name)"))
        assertTrue(homeSource.contains("val initialCategory = initialCategory()"))
        assertTrue(homeSource.contains("viewModel.setCategory(initialCategory)"))
        assertTrue(homeSource.contains("showCategoryPage(initialCategory)"))
    }

    @Test
    fun tvHomeExplainsMediaPermissionBeforeLocalLibraryScan() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(layout.contains("""android:id="@+id/tv_permission_panel""""))
        assertTrue(layout.contains("""@string/tv_permission_title"""))
        assertTrue(layout.contains("""@string/tv_permission_desc"""))
        assertTrue(layout.contains("""@string/tv_permission_action"""))
        assertTrue(layout.contains("""android:focusable="true""""))
        assertTrue(layout.contains("""android:foreground="@drawable/bg_focusable_card""""))
        assertTrue(layout.contains("""android:nextFocusDown="@id/tv_card_continue""""))
        assertTrue(layout.contains("""android:nextFocusUp="@id/tv_permission_panel""""))

        assertTrue(source.contains("import androidx.activity.result.contract.ActivityResultContracts"))
        assertTrue(source.contains("import androidx.core.content.ContextCompat"))
        assertTrue(source.contains("import androidx.core.view.isVisible"))
        assertTrue(source.contains("import com.example.openvideo.ui.home.MediaLibraryPermissionPolicy"))
        assertTrue(source.contains("private val permissionLauncher = registerForActivityResult("))
        assertTrue(source.contains("ActivityResultContracts.RequestMultiplePermissions()"))
        assertTrue(source.contains("bindPermissionPanel(view)"))
        assertTrue(source.contains("MediaLibraryPermissionPolicy.hasReadAccess"))
        assertTrue(source.contains("permissionLauncher.launch(MediaLibraryPermissionPolicy.requiredPermissions())"))
        assertTrue(source.contains("R.id.tv_permission_panel"))
        assertTrue(source.contains("navigateTo(LocalFolderFragment())"))
    }

    @Test
    fun tvHomePermissionFocusDoesNotTargetHiddenPermissionPanel() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("val permissionPanel = root.findViewById<View>(R.id.tv_permission_panel)"))
        assertTrue(source.contains("val continueCard = root.findViewById<View>(R.id.tv_card_continue)"))
        assertTrue(source.contains("continueCard.nextFocusUpId = if (permissionPanel.isVisible) permissionPanel.id else continueCard.id"))
    }

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
