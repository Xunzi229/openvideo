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
    fun tvHomeFocusableCardsAllowProgrammaticFocusInTouchMode() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))

        listOf(
            "tv_permission_panel",
            "tv_card_continue",
            "tv_card_folders",
            "tv_card_series",
            "tv_card_sources",
            "tv_card_settings"
        ).forEach { id ->
            assertTrue(
                "Missing focusableInTouchMode for $id",
                cardOpeningTag(layout, id).contains("""android:focusableInTouchMode="true"""")
            )
        }
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
            """android:nextFocusLeft="@id/tv_card_sources"""",
            """android:nextFocusUp="@id/tv_card_folders"""",
            """android:nextFocusRight="@id/tv_card_settings"""",
            """android:nextFocusDown="@id/tv_card_settings""""
        ).forEach { focusRule ->
            assertTrue("Missing focus rule: $focusRule", layout.contains(focusRule))
        }
    }

    @Test
    fun tvHomeLayoutKeepsDpadFocusInsideCardGridAtEdges() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))

        assertTrue(cardBlock(layout, "tv_card_continue").contains("""android:nextFocusLeft="@id/tv_card_continue""""))
        assertTrue(cardBlock(layout, "tv_card_folders").contains("""android:nextFocusUp="@id/tv_card_folders""""))
        assertTrue(cardBlock(layout, "tv_card_folders").contains("""android:nextFocusRight="@id/tv_card_folders""""))
        assertTrue(cardBlock(layout, "tv_card_series").contains("""android:nextFocusLeft="@id/tv_card_series""""))
        assertTrue(cardBlock(layout, "tv_card_series").contains("""android:nextFocusDown="@id/tv_card_series""""))
        assertTrue(cardBlock(layout, "tv_card_sources").contains("""android:nextFocusDown="@id/tv_card_sources""""))
        assertTrue(cardBlock(layout, "tv_card_settings").contains("""android:nextFocusRight="@id/tv_card_settings""""))
        assertTrue(cardBlock(layout, "tv_card_settings").contains("""android:nextFocusDown="@id/tv_card_settings""""))
    }

    @Test
    fun tvHomeFocusableCardsHaveStaticContentDescriptionFallbacks() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))

        mapOf(
            "tv_card_continue" to "tv_home_continue",
            "tv_card_folders" to "tv_home_folders",
            "tv_card_series" to "tv_home_series",
            "tv_card_sources" to "tv_home_sources",
            "tv_card_settings" to "tv_home_settings"
        ).forEach { (cardId, stringName) ->
            assertTrue(
                "Missing contentDescription for $cardId",
                cardOpeningTag(layout, cardId).contains("""android:contentDescription="@string/$stringName"""")
            )
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
        assertTrue(source.contains(".load(posterModel(firstPosterSeries?.posterPath))"))
        assertTrue(source.contains(".placeholder(R.drawable.ic_movie)"))
        assertTrue(source.contains(".fallback(R.drawable.ic_movie)"))
        assertTrue(source.contains(".error(R.drawable.ic_movie)"))
    }


    @Test
    fun tvHomeSeriesCardUpdatesCoverDescriptionFromPosterSeriesTitle() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("val firstPosterSeries = series.firstOrNull { it.posterPath?.isNotBlank() == true }"))
        assertTrue(source.contains("seriesCover.contentDescription = firstPosterSeries?.title"))
        assertTrue(source.contains("?: getString(R.string.tv_home_series)"))
    }

    @Test
    fun tvHomeSeriesCardShowsPosterSeriesTitleAndCountSummary() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val strings = loadText(Paths.get("src", "main", "res", "values", "strings.xml"))

        assertTrue(layout.contains("""android:id="@+id/tv_home_series_detail""""))
        assertTrue(layout.contains("""android:text="@string/tv_home_series_desc""""))
        assertTrue(strings.contains("""name="tv_home_series_summary""""))
        assertTrue(source.contains("val seriesDetail = root.findViewById<TextView>(R.id.tv_home_series_detail)"))
        assertTrue(source.contains("val seriesSummary = if (firstPosterSeries == null) {"))
        assertTrue(source.contains("seriesDetail.text = seriesSummary"))
        assertTrue(source.contains("getString(R.string.tv_home_series_desc)"))
        assertTrue(source.contains("getString(R.string.tv_home_series_summary, firstPosterSeries.title, series.size)"))
    }

    @Test
    fun tvHomeSeriesFocusableCardUsesSeriesSummaryDescription() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("val seriesCard = root.findViewById<View>(R.id.tv_card_series)"))
        assertTrue(source.contains("val seriesSummary = if (firstPosterSeries == null) {"))
        assertTrue(source.contains("seriesDetail.text = seriesSummary"))
        assertTrue(source.contains("seriesCard.contentDescription = seriesSummary"))
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
        assertTrue(source.contains("homeViewModel.recentVideos,"))
        assertTrue(source.contains(".load(continueCoverModel)"))
        assertTrue(source.contains(".placeholder(R.drawable.ic_play)"))
        assertTrue(source.contains(".fallback(R.drawable.ic_play)"))
        assertTrue(source.contains(".error(R.drawable.ic_play)"))
    }

    @Test
    fun tvHomeContinueCardUsesFirstAvailableRecentThumbnail() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("val continueCoverModel = recent.firstNotNullOfOrNull { it.thumbnailUri }"))
        assertTrue(source.contains(".load(continueCoverModel)"))
    }

    @Test
    fun tvHomeContinueCardUpdatesCoverDescriptionFromCurrentRecentTitle() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("continueCover.contentDescription = firstRecent?.title"))
        assertTrue(source.contains("?: getString(R.string.tv_home_continue)"))
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
        assertTrue(source.contains("val continueSummary = when {"))
        assertTrue(source.contains("continueDetail.text = continueSummary"))
        assertTrue(source.contains("getString(R.string.tv_home_continue_desc)"))
        assertTrue(source.contains("getString(R.string.tv_home_continue_recent_summary, firstRecent.title, recent.size)"))
    }

    @Test
    fun tvHomeContinueFocusableCardUsesRecentSummaryDescription() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("val continueCard = root.findViewById<View>(R.id.tv_card_continue)"))
        assertTrue(source.contains("val continueSummary = when {"))
        assertTrue(source.contains("continueDetail.text = continueSummary"))
        assertTrue(source.contains("continueCard.contentDescription = continueSummary"))
    }

    @Test
    fun tvHomeContinueCardShowsProgressSummaryWhenRecentBadgeExists() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val strings = loadText(Paths.get("src", "main", "res", "values", "strings.xml"))

        assertTrue(strings.contains("""name="tv_home_continue_progress_summary""""))
        assertTrue(source.contains("import kotlinx.coroutines.flow.combine"))
        assertTrue(source.contains("homeViewModel.recentContinueWatchingBadges"))
        assertTrue(source.contains("val firstRecentBadge = firstRecent?.let { badges[it.id] }"))
        assertTrue(source.contains("R.string.tv_home_continue_progress_summary"))
        assertTrue(source.contains("firstRecent.title,\n                            firstRecentBadge.progressLabel,\n                            recent.size"))
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
    fun tvHomeFoldersCardShowsFolderAndVideoCountSummary() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val strings = loadText(Paths.get("src", "main", "res", "values", "strings.xml"))

        assertTrue(layout.contains("""android:id="@+id/tv_home_folders_detail""""))
        assertTrue(layout.contains("""android:text="@string/tv_home_folders_desc""""))
        assertTrue(strings.contains("""name="tv_home_folders_summary""""))
        assertTrue(source.contains("bindFoldersSummary(view)"))
        assertTrue(source.contains("val foldersDetail = root.findViewById<TextView>(R.id.tv_home_folders_detail)"))
        assertTrue(source.contains("homeViewModel.folders.collect { folders ->"))
        assertTrue(source.contains("val videoCount = folders.sumOf { it.videoCount }"))
        assertTrue(source.contains("val foldersSummary = if (folders.isEmpty()) {"))
        assertTrue(source.contains("foldersDetail.text = foldersSummary"))
        assertTrue(source.contains("getString(R.string.tv_home_folders_desc)"))
        assertTrue(source.contains("getString(R.string.tv_home_folders_summary, folders.size, videoCount)"))
    }

    @Test
    fun tvHomeFoldersCardUpdatesIconDescriptionWithFolderSummary() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(layout.contains("""android:id="@+id/tv_home_folders_icon""""))
        assertTrue(source.contains("val foldersIcon = root.findViewById<ImageView>(R.id.tv_home_folders_icon)"))
        assertTrue(source.contains("val foldersSummary = if (folders.isEmpty()) {"))
        assertTrue(source.contains("foldersDetail.text = foldersSummary"))
        assertTrue(source.contains("foldersIcon.contentDescription = foldersSummary"))
    }

    @Test
    fun tvHomeFoldersFocusableCardUsesFolderSummaryDescription() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("val foldersCard = root.findViewById<View>(R.id.tv_card_folders)"))
        assertTrue(source.contains("foldersCard.contentDescription = foldersSummary"))
    }

    @Test
    fun tvHomeSourcesCardShowsSavedSourceCountSummary() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val strings = loadText(Paths.get("src", "main", "res", "values", "strings.xml"))

        assertTrue(layout.contains("""android:id="@+id/tv_home_sources_detail""""))
        assertTrue(layout.contains("""android:text="@string/tv_home_sources_desc""""))
        assertTrue(strings.contains("""name="tv_home_sources_summary""""))
        assertTrue(source.contains("import com.example.openvideo.data.repository.VideoRepository"))
        assertTrue(source.contains("@Inject lateinit var repository: VideoRepository"))
        assertTrue(source.contains("bindSourcesSummary(view)"))
        assertTrue(source.contains("val sourcesDetail = root.findViewById<TextView>(R.id.tv_home_sources_detail)"))
        assertTrue(source.contains("repository.getMediaSources().collect { sources ->"))
        assertTrue(source.contains("val sourcesSummary = if (sources.isEmpty()) {"))
        assertTrue(source.contains("sourcesDetail.text = sourcesSummary"))
        assertTrue(source.contains("getString(R.string.tv_home_sources_desc)"))
        assertTrue(source.contains("getString(R.string.tv_home_sources_summary, sources.size)"))
    }

    @Test
    fun tvHomeSourcesCardUpdatesIconDescriptionWithSavedSourceSummary() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(layout.contains("""android:id="@+id/tv_home_sources_icon""""))
        assertTrue(source.contains("val sourcesIcon = root.findViewById<ImageView>(R.id.tv_home_sources_icon)"))
        assertTrue(source.contains("val sourcesSummary = if (sources.isEmpty()) {"))
        assertTrue(source.contains("sourcesDetail.text = sourcesSummary"))
        assertTrue(source.contains("sourcesIcon.contentDescription = sourcesSummary"))
    }

    @Test
    fun tvHomeSourcesFocusableCardUsesSavedSourceSummaryDescription() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("val sourcesCard = root.findViewById<View>(R.id.tv_card_sources)"))
        assertTrue(source.contains("sourcesCard.contentDescription = sourcesSummary"))
    }

    @Test
    fun tvHomeSettingsCardShowsDefaultPlaybackSummary() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val strings = loadText(Paths.get("src", "main", "res", "values", "strings.xml"))

        assertTrue(layout.contains("""android:id="@+id/tv_home_settings_detail""""))
        assertTrue(layout.contains("""android:text="@string/tv_home_settings_desc""""))
        assertTrue(strings.contains("""name="tv_home_settings_summary""""))
        assertTrue(source.contains("import com.example.openvideo.ui.player.PlayerAspectRatioOptions"))
        assertTrue(source.contains("import com.example.openvideo.ui.settings.SettingsViewModel"))
        assertTrue(source.contains("private val settingsViewModel: SettingsViewModel by viewModels()"))
        assertTrue(source.contains("bindSettingsSummary(view)"))
        assertTrue(source.contains("val settingsDetail = root.findViewById<TextView>(R.id.tv_home_settings_detail)"))
        assertTrue(source.contains("val ratioLabel = PlayerAspectRatioOptions.entries.firstOrNull { it.ratio == settingsViewModel.defaultRatio }"))
        assertTrue(source.contains("val settingsSummary = getString("))
        assertTrue(source.contains("settingsDetail.text = settingsSummary"))
        assertTrue(source.contains("R.string.tv_home_settings_summary,"))
        assertTrue(source.contains("getString(ratioLabel?.labelRes ?: R.string.settings_ratio_default),"))
        assertTrue(source.contains("\"${'$'}{settingsViewModel.defaultSpeed}x\""))
    }

    @Test
    fun tvHomeSettingsCardUpdatesIconDescriptionWithPlaybackSummary() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(layout.contains("""android:id="@+id/tv_home_settings_icon""""))
        assertTrue(source.contains("val settingsIcon = root.findViewById<ImageView>(R.id.tv_home_settings_icon)"))
        assertTrue(source.contains("val settingsSummary = getString("))
        assertTrue(source.contains("settingsDetail.text = settingsSummary"))
        assertTrue(source.contains("settingsIcon.contentDescription = settingsSummary"))
    }

    @Test
    fun tvHomeSettingsFocusableCardUsesPlaybackSummaryDescription() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("val settingsCard = root.findViewById<View>(R.id.tv_card_settings)"))
        assertTrue(source.contains("settingsCard.contentDescription = settingsSummary"))
    }

    @Test
    fun tvHomeExplainsMediaPermissionBeforeLocalLibraryScan() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val permissionPanelBlock = cardBlock(layout, "tv_permission_panel")

        assertTrue(layout.contains("""android:id="@+id/tv_permission_panel""""))
        assertTrue(layout.contains("""@string/tv_permission_title"""))
        assertTrue(layout.contains("""@string/tv_permission_desc"""))
        assertTrue(layout.contains("""@string/tv_permission_action"""))
        assertTrue(permissionPanelBlock.contains("""android:focusable="true""""))
        assertTrue(permissionPanelBlock.contains("""android:foreground="@drawable/bg_focusable_card""""))
        assertTrue(permissionPanelBlock.contains("""android:nextFocusUp="@id/tv_permission_panel""""))
        assertTrue(permissionPanelBlock.contains("""android:nextFocusLeft="@id/tv_permission_panel""""))
        assertTrue(permissionPanelBlock.contains("""android:nextFocusRight="@id/tv_permission_panel""""))
        assertTrue(permissionPanelBlock.contains("""android:nextFocusDown="@id/tv_card_continue""""))

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

    @Test
    fun tvHomePermissionPanelHasStaticContentDescriptionFallback() {
        val layout = loadText(Paths.get("src", "main", "res", "layout", "fragment_tv_home.xml"))

        assertTrue(
            cardOpeningTag(layout, "tv_permission_panel")
                .contains("""android:contentDescription="@string/tv_permission_title"""")
        )
    }

    @Test
    fun tvHomePermissionPanelUsesExistingCopyAsFocusableDescription() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("permissionPanel.contentDescription = getString("))
        assertTrue(source.contains("R.string.tv_permission_title"))
        assertTrue(source.contains("R.string.tv_permission_desc"))
        assertTrue(source.contains("R.string.tv_permission_action"))
    }

    @Test
    fun tvHomePermissionDenialReturnsFocusToPermissionPanel() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("if (hasMediaReadAccess()) {"))
        assertTrue(source.contains("navigateTo(LocalFolderFragment())"))
        assertTrue(source.contains("} else {"))
        assertTrue(source.contains("view?.let(::requestInitialFocus)"))
    }

    @Test
    fun tvHomePermissionGrantReturnsToFoldersCardAfterLocalScan() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val grantBranch = source.substringAfter("if (hasMediaReadAccess()) {")
            .substringBefore("} else {")

        assertTrue(source.contains(") { _ ->"))
        assertTrue(source.contains("if (hasMediaReadAccess()) {"))
        assertTrue(grantBranch.contains("lastFocusedCardId = R.id.tv_card_folders"))
        assertTrue(grantBranch.contains("navigateTo(LocalFolderFragment())"))
    }

    @Test
    fun tvHomeRestoresLastFocusedCardWhenReturningFromChildScreen() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("private var lastFocusedCardId: Int = R.id.tv_card_continue"))
        assertTrue(source.contains("lastFocusedCardId = cardId"))
        assertTrue(source.contains("requestResumeFocus(root)"))
        assertTrue(source.contains("requestFocusAfterLayout(resumeTarget)"))
    }

    @Test
    fun tvHomePersistsLastFocusedCardAcrossConfigurationChanges() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("private const val KEY_LAST_FOCUSED_CARD_ID = \"tv_home_last_focused_card_id\""))
        assertTrue(source.contains("savedInstanceState?.getInt(KEY_LAST_FOCUSED_CARD_ID, R.id.tv_card_continue)"))
        assertTrue(source.contains("override fun onSaveInstanceState(outState: Bundle)"))
        assertTrue(source.contains("outState.putInt(KEY_LAST_FOCUSED_CARD_ID, lastFocusedCardId)"))
    }

    @Test
    fun tvHomeTracksFocusedCardBeforeClickingIt() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("setOnFocusChangeListener { _, hasFocus ->"))
        assertTrue(source.contains("if (hasFocus) {"))
        assertTrue(source.contains("lastFocusedCardId = cardId"))
    }

    @Test
    fun tvHomeDefersInitialAndResumeFocusUntilLayoutPass() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("private fun requestFocusAfterLayout(target: View)"))
        assertTrue(source.contains("target.post {"))
        assertTrue(source.contains("if (target.isShown) {"))
        assertTrue(source.contains("target.requestFocus()"))
        assertTrue(source.contains("requestFocusAfterLayout(permissionPanel)"))
        assertTrue(source.contains("requestFocusAfterLayout(root.findViewById(R.id.tv_card_continue))"))
        assertTrue(source.contains("requestFocusAfterLayout(resumeTarget)"))
    }

    @Test
    fun tvHomeSkipsDeferredFocusWhenTargetIsHidden() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )
        val focusHelper = source.substringAfter("private fun requestFocusAfterLayout(target: View)")
            .substringBefore("\n\n    private fun hasMediaReadAccess")

        assertTrue(focusHelper.contains("target.post {"))
        assertTrue(focusHelper.contains("if (target.isShown) {"))
        assertTrue(focusHelper.contains("target.requestFocus()"))
    }

    @Test
    fun tvHomeFallsBackToContinueWhenSavedFocusTargetIsMissing() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("val resumeTarget = root.findViewById<View>(lastFocusedCardId)"))
        assertTrue(source.contains("?: root.findViewById(R.id.tv_card_continue)"))
        assertTrue(source.contains("requestFocusAfterLayout(resumeTarget)"))
    }

    @Test
    fun tvHomeUsesResumeFocusWhenViewIsRecreatedWithSavedCard() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("if (savedInstanceState == null) {"))
        assertTrue(source.contains("requestInitialFocus(view)"))
        assertTrue(source.contains("} else {"))
        assertTrue(source.contains("requestResumeFocus(view)"))
    }

    @Test
    fun tvHomeSkipsDuplicateFocusRequestOnFirstResumeAfterViewCreated() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "tv", "TvHomeFragment.kt")
        )

        assertTrue(source.contains("private var skipNextResumeFocusRequest: Boolean = false"))
        assertTrue(source.contains("skipNextResumeFocusRequest = true"))
        assertTrue(source.contains("if (skipNextResumeFocusRequest) {"))
        assertTrue(source.contains("skipNextResumeFocusRequest = false"))
        assertTrue(source.contains("} else {"))
        assertTrue(source.contains("requestResumeFocus(root)"))
    }

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }

    private fun cardBlock(layout: String, id: String): String =
        layout.substringAfter("""android:id="@+id/$id"""")
            .substringBefore("</LinearLayout>")

    private fun cardOpeningTag(layout: String, id: String): String =
        layout.substringAfter("""android:id="@+id/$id"""")
            .substringBefore(">")
}
