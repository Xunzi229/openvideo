package com.example.openvideo.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AppleStyleAndSizeSourceTest {

    @Test
    fun lightTokensUseAppleGroupedSurfaceAndSystemBlue() {
        val tokens = rootFile("app", "src", "main", "res", "values", "design_tokens.xml").readText()

        assertTrue(tokens.contains("#FFF2F2F7"))
        assertTrue(tokens.contains("#FF007AFF"))
        assertTrue(tokens.contains("ov_text_large_title"))
        assertTrue(tokens.contains("ov_row_height"))
        assertTrue(tokens.contains("ov_icon_button_size"))
        assertTrue(tokens.contains("<dimen name=\"ov_row_height\">48dp</dimen>"))
    }

    @Test
    fun sizeTokensScaleBySmallestWidthAndShortHeight() {
        val sw360 = rootFile("app", "src", "main", "res", "values-sw360dp", "design_tokens.xml").readText()
        val sw600 = rootFile("app", "src", "main", "res", "values-sw600dp", "design_tokens.xml").readText()
        val sw840 = rootFile("app", "src", "main", "res", "values-sw840dp", "design_tokens.xml").readText()
        val land = rootFile("app", "src", "main", "res", "values-land", "design_tokens.xml").readText()
        val sw360Land = rootFile("app", "src", "main", "res", "values-sw360dp-land", "design_tokens.xml").readText()

        assertTrue(sw360.contains("ov_space_page"))
        assertTrue(sw600.contains("ov_space_page"))
        assertTrue(sw840.contains("ov_space_page"))
        assertTrue(land.contains("<dimen name=\"ov_bottom_nav_height\">64dp</dimen>"))
        assertTrue(sw360Land.contains("<dimen name=\"ov_bottom_nav_height\">64dp</dimen>"))
    }

    @Test
    fun mainActivityAppliesSystemBarAndCutoutInsets() {
        val source = rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "MainActivity.kt"
        ).readText()
        val layout = rootFile("app", "src", "main", "res", "layout", "activity_main.xml").readText()

        assertTrue(source.contains("WindowCompat.setDecorFitsSystemWindows(window, false)"))
        assertTrue(source.contains("SystemBarInsetsPolicy.union("))
        assertTrue(source.contains("WindowInsetsCompat.Type.displayCutout()"))
        assertTrue(layout.contains("""android:id="@+id/main_root""""))
        assertTrue(layout.contains("@drawable/bg_tab_bar"))
        assertTrue(layout.contains("app:itemPaddingTop"))
        assertTrue(layout.contains("app:itemPaddingBottom"))
        assertTrue(layout.contains("@dimen/ov_bottom_nav_height"))
        val tokens = rootFile("app", "src", "main", "res", "values", "design_tokens.xml").readText()
        assertTrue(tokens.contains("<dimen name=\"ov_bottom_nav_height\">64dp</dimen>"))
        assertFalse(tokens.contains("<dimen name=\"ov_bottom_nav_height\">49dp</dimen>"))
        assertFalse(source.contains("gh release create"))
    }

    @Test
    fun settingsAndSourcesUseGroupedInsetCards() {
        val settings = rootFile("app", "src", "main", "res", "layout", "fragment_settings.xml").readText()
        val theme = rootFile("app", "src", "main", "res", "values", "themes.xml").readText()

        assertTrue(theme.contains("Widget.OpenVideo.GroupedSection"))
        assertTrue(theme.contains("Widget.OpenVideo.Chevron"))
        assertTrue(theme.contains("@drawable/ic_chevron_right"))
        assertTrue(settings.contains("@style/Widget.OpenVideo.GroupedSection"))
        assertTrue(settings.contains("@style/Widget.OpenVideo.Chevron"))
        val sources = rootFile("app", "src", "main", "res", "layout", "fragment_sources.xml").readText()
        assertTrue(sources.contains("@style/Widget.OpenVideo.GroupedSection"))
        assertTrue(sources.contains("@style/Widget.OpenVideo.Chevron"))
    }

    @Test
    fun batchTwoUsesGroupedListsAndHeaderEditMode() {
        val home = rootFile("app", "src", "main", "res", "layout", "fragment_home.xml").readText()
        val playlistItem = rootFile("app", "src", "main", "res", "layout", "item_playlist.xml").readText()
        val video = rootFile("app", "src", "main", "res", "layout", "item_video.xml").readText()
        val grid = rootFile("app", "src", "main", "res", "layout", "item_video_grid.xml").readText()
        val series = rootFile("app", "src", "main", "res", "layout", "item_series.xml").readText()
        val adapter = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "home", "VideoGridAdapter.kt"
        ).readText()
        val homeSource = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "home", "HomeFragment.kt"
        ).readText()
        val chrome = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "GroupedListChrome.kt"
        ).readText()

        assertTrue(chrome.contains("fun bind("))
        assertTrue(chrome.contains("bg_grouped_row_top"))
        assertTrue(home.contains("""android:id="@+id/btn_select""""))
        assertTrue(home.contains("""android:id="@+id/edit_actions""""))
        assertTrue(home.contains("@string/action_select"))
        assertTrue(homeSource.contains("R.string.action_done"))
        assertTrue(home.contains("@+id/empty_state"))
        assertTrue(!homeSource.contains("startSupportActionMode"))
        assertTrue(homeSource.contains("private fun startMultiSelectMode(category: HomeCategory)"))
        assertTrue(!playlistItem.contains("MaterialCardView"))
        assertTrue(playlistItem.contains("@+id/row_hairline"))
        assertTrue(!video.contains("MaterialCardView"))
        assertTrue(!video.contains("CheckBox"))
        assertTrue(video.contains("@drawable/bg_thumb_clip"))
        assertTrue(video.contains("@+id/cb_select"))
        assertTrue(!grid.contains("MaterialCardView"))
        assertTrue(grid.contains("@drawable/bg_thumb_clip"))
        assertTrue(series.contains("@drawable/ic_chevron_right"))
        assertTrue(!series.contains("@drawable/ic_arrow_up"))
        assertTrue(adapter.contains("ImageView?"))
        assertTrue(adapter.contains("bindSelectMark"))
    }

    @Test
    fun batchOneReplacesMaterialChromeWithAppleControls() {
        val home = rootFile("app", "src", "main", "res", "layout", "fragment_home.xml").readText()
        val playlist = rootFile("app", "src", "main", "res", "layout", "fragment_playlist.xml").readText()
        val privacy = rootFile("app", "src", "main", "res", "layout", "fragment_privacy.xml").readText()
        val local = rootFile("app", "src", "main", "res", "layout", "fragment_local_folders.xml").readText()
        val video = rootFile("app", "src", "main", "res", "layout", "item_video.xml").readText()
        val folder = rootFile("app", "src", "main", "res", "layout", "fragment_folder_videos.xml").readText()
        val hud = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "AppleHud.kt"
        ).readText()

        assertTrue(home.contains("bg_segmented_track"))
        assertTrue(home.contains("""android:id="@+id/chip_all""""))
        assertTrue(!home.contains("Widget.Material3.Chip.Filter"))
        assertTrue(playlist.contains("""android:id="@+id/btn_add""""))
        assertTrue(!playlist.contains("FloatingActionButton"))
        assertTrue(privacy.contains("""android:id="@+id/btn_add""""))
        assertTrue(!privacy.contains("FloatingActionButton"))
        assertTrue(local.contains("""android:id="@+id/row_continue_playback""""))
        assertTrue(!local.contains("FloatingActionButton"))
        assertTrue(video.contains("@drawable/ic_more_horiz"))
        assertTrue(!video.contains("@drawable/ic_more_vert"))
        assertTrue(folder.contains("include_apple_nav_back"))
        assertTrue(folder.contains("@drawable/ic_chevron_left") || folder.contains("include_apple_nav_back"))
        assertTrue(hud.contains("fun show("))
        assertTrue(hud.contains("PopupWindow"))
        assertTrue(!hud.contains("Snackbar"))
    }

    @Test
    fun batchThreeSettingsSubpagesUseActionSheetsAndGreenSwitch() {
        val tokens = rootFile("app", "src", "main", "res", "values", "design_tokens.xml").readText()
        val night = rootFile("app", "src", "main", "res", "values-night", "design_tokens.xml").readText()
        val theme = rootFile("app", "src", "main", "res", "values", "themes.xml").readText()
        val settings = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "settings", "SettingsFragment.kt"
        ).readText()
        val notification = rootFile(
            "app", "src", "main", "res", "layout", "activity_notification_settings.xml"
        ).readText()
        val notificationBinder = rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "settings",
            "NotificationSettingsBinder.kt"
        ).readText()
        val audioActivity = rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerAudioSettingsActivity.kt"
        ).readText()
        val subtitleActivity = rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerSubtitleSettingsActivity.kt"
        ).readText()

        assertTrue(tokens.contains("ov_switch_on"))
        assertTrue(tokens.contains("#FF34C759"))
        assertTrue(night.contains("#FF30D158"))
        assertTrue(theme.contains("Widget.OpenVideo.Switch"))
        assertTrue(theme.contains("@color/ov_switch_track"))
        assertTrue(notification.contains("include_apple_nav_back"))
        assertTrue(notification.contains("@style/Widget.OpenVideo.GroupedSection"))
        assertTrue(notification.contains("@style/Widget.OpenVideo.Switch"))
        assertTrue(notification.contains("""android:id="@+id/sw_allow_system_notifications""""))
        assertTrue(notification.contains("""android:id="@+id/sw_bg_notification""""))
        assertFalse(notification.contains("MaterialToolbar"))
        assertFalse(notification.contains("ov_accent_blue"))
        assertTrue(notificationBinder.contains("R.id.btn_back"))
        assertFalse(notificationBinder.contains("MaterialToolbar"))
        assertFalse(settings.contains("modes[next]"))
        assertFalse(settings.contains("langs[next]"))
        assertTrue(settings.contains("showThemeSheet(tvTheme)"))
        assertTrue(settings.contains("AppleActionSheet.show"))
        assertTrue(settings.contains("viewModel.setThemeMode(mode)"))
        assertTrue(settings.contains("viewModel.setLanguage(lang)"))
        assertTrue(audioActivity.contains("AppleActionSheet.show"))
        assertFalse(audioActivity.contains("AudioChannel.STEREO -> com.example.openvideo.core.prefs.AudioChannel.LEFT"))
        assertTrue(subtitleActivity.contains("AppleActionSheet.show"))
        assertFalse(subtitleActivity.contains("% subtitleBgStyles.size"))
        assertFalse(subtitleActivity.contains("% encodings.size"))
    }

    @Test
    fun batchFourTabBarUsesOutlineFilledIconsBlurAndPlayerHud() {
        val menu = rootFile("app", "src", "main", "res", "menu", "bottom_nav_menu.xml").readText()
        val homeSelector = rootFile("app", "src", "main", "res", "drawable", "ic_nav_home.xml").readText()
        val homeOutline = rootFile("app", "src", "main", "res", "drawable", "ic_nav_home_outline.xml").readText()
        val tabBar = rootFile("app", "src", "main", "res", "drawable", "bg_tab_bar.xml").readText()
        val v31 = rootFile("app", "src", "main", "res", "values-v31", "design_tokens.xml").readText()
        val nightV31 = rootFile("app", "src", "main", "res", "values-night-v31", "design_tokens.xml").readText()
        val blur = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "TabBarBlur.kt"
        ).readText()
        val haptics = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "AppleHaptics.kt"
        ).readText()
        val main = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "MainActivity.kt"
        ).readText()
        val overlay = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "AppleOverlayChrome.kt"
        ).readText()
        val subtitle = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerSubtitleSettingsSheet.kt"
        ).readText()
        val smartCrop = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerSmartCropController.kt"
        ).readText()
        val playerControls = rootFile("app", "src", "main", "res", "layout", "player_controls.xml").readText()
        val theme = rootFile("app", "src", "main", "res", "values", "themes.xml").readText()
        val dialog = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerQuickDialogController.kt"
        ).readText()

        assertTrue(menu.contains("@drawable/ic_nav_home"))
        assertTrue(menu.contains("@drawable/ic_nav_video"))
        assertTrue(menu.contains("@drawable/ic_nav_sources"))
        assertTrue(menu.contains("@drawable/ic_nav_playlist"))
        assertTrue(menu.contains("@drawable/ic_nav_mine"))
        assertTrue(homeSelector.contains("@drawable/ic_nav_home_filled"))
        assertTrue(homeSelector.contains("@drawable/ic_nav_home_outline"))
        assertTrue(homeOutline.contains("android:strokeWidth=\"2\""))
        assertTrue(tabBar.contains("android:width=\"0.5dp\""))
        assertTrue(tabBar.contains("@color/ov_tab_bar_stroke"))
        assertTrue(v31.contains("#B3FFFFFF"))
        assertTrue(nightV31.contains("#B31C1C1E"))
        assertTrue(blur.contains("VERSION_CODES.S"))
        assertTrue(blur.contains("ov_tab_bar_fill"))
        assertTrue(!blur.contains("view.setBackgroundBlurRadius"))
        assertTrue(haptics.contains("HapticFeedbackConstants.CONTEXT_CLICK"))
        assertTrue(main.contains("TabBarBlur.bind(bottomNav)"))
        assertTrue(main.contains("AppleHaptics.light(bottomNav)"))
        assertTrue(overlay.contains("AppleActionStyle.DESTRUCTIVE"))
        assertTrue(overlay.contains("AppleHaptics.light(it)"))
        assertTrue(subtitle.contains("AppleHud.show("))
        assertTrue(!subtitle.contains("Toast.makeText"))
        assertTrue(smartCrop.contains("AppleHud.show(activity, messageRes)"))
        assertTrue(smartCrop.contains("AppleHud.dismiss()"))
        assertTrue(!smartCrop.contains("Toast.makeText"))
        assertTrue(theme.contains("materialSwitchStyle\">@style/Widget.OpenVideo.Switch"))
        assertTrue(playerControls.contains("@drawable/ic_arrow_back"))
        assertTrue(playerControls.contains("@drawable/ic_more_vert"))
        assertTrue(dialog.contains("PlayerGlassSheetChrome.PLAYER_BOTTOM"))
        assertTrue(dialog.contains("PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL"))
    }

    @Test
    fun batchFiveSettingsPickersUseActionSheetCheckmarks() {
        val action = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "AppleAction.kt"
        ).readText()
        val overlay = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "AppleOverlayChrome.kt"
        ).readText()
        val settings = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "settings", "SettingsFragment.kt"
        ).readText()
        val home = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "home", "HomeFragment.kt"
        ).readText()
        val homeViewModel = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "home", "HomeViewModel.kt"
        ).readText()
        val dialog = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerQuickDialogController.kt"
        ).readText()

        assertTrue(action.contains("val selected: Boolean?"))
        assertTrue(overlay.contains("R.drawable.ic_check"))
        assertTrue(overlay.contains("action.selected == null"))
        assertTrue(settings.contains("showDefaultRatioDialog(tvRatio)"))
        assertTrue(settings.contains("AppleActionSheet.show("))
        assertTrue(!settings.contains("PlayerGlassSheetDialog"))
        assertTrue(settings.contains("selected = option.ratio == viewModel.defaultRatio"))
        assertTrue(settings.contains("selected = speed == viewModel.defaultSpeed"))
        assertTrue(settings.contains("selected = mode == viewModel.themeMode"))
        assertTrue(home.contains("viewModel.setSortField(field)"))
        assertTrue(home.contains("AppleActionSheet.show("))
        assertTrue(!home.contains("cycleSortField"))
        assertTrue(homeViewModel.contains("fun setSortField(field: SortField)"))
        assertTrue(!homeViewModel.contains("fun cycleSortField()"))
        assertTrue(dialog.contains("PlayerGlassSheetChrome.PLAYER_BOTTOM"))
        assertTrue(dialog.contains("PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL"))
    }

    @Test
    fun batchSixSettingsPickersUseThemedActionSheets() {
        val colors = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "AppleOverlayColors.kt"
        ).readText()
        val actionSheet = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", "AppleActionSheet.kt"
        ).readText()
        val playbackActivity = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerPlaybackSettingsActivity.kt"
        ).readText()
        val playbackSheet = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerPlaybackSettingsSheet.kt"
        ).readText()
        val playbackLayout = rootFile(
            "app", "src", "main", "res", "layout", "activity_player_playback_settings.xml"
        ).readText()
        val subtitleSheet = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerSubtitleSettingsSheet.kt"
        ).readText()
        val displaySheet = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerDisplaySettingsSheet.kt"
        ).readText()
        val displayActivity = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerDisplaySettingsActivity.kt"
        ).readText()
        val contentFrame = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerDisplayContentFrameControls.kt"
        ).readText()
        val audioSheet = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerAudioSettingsSheet.kt"
        ).readText()
        val gestureSheet = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerGestureSettingsSheet.kt"
        ).readText()
        val speedOptions = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerPlaybackSpeedOptions.kt"
        ).readText()
        val dialog = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerQuickDialogController.kt"
        ).readText()

        assertTrue(colors.contains("createConfigurationContext"))
        assertTrue(colors.contains("R.color.player_bg"))
        assertTrue(colors.contains("UI_MODE_NIGHT_YES"))
        assertTrue(actionSheet.contains("fun <T> showPicker"))
        assertTrue(actionSheet.contains("defaultFocusCancel = false"))
        assertTrue(playbackLayout.contains("@+id/tv_speed_value"))
        assertTrue(!playbackLayout.contains("rg_speed_settings"))
        assertTrue(playbackActivity.contains("AppleActionSheet.showPicker"))
        assertTrue(!playbackActivity.contains("AlertDialog.Builder"))
        assertTrue(playbackSheet.contains("override fun settingsSheetDefaultFocusId(): Int = R.id.tv_speed_value"))
        assertTrue(playbackSheet.contains("AppleActionSheet.showPicker"))
        assertTrue(!playbackSheet.contains("AlertDialog.Builder"))
        assertTrue(subtitleSheet.contains("AppleActionSheet.showPicker"))
        assertTrue(!subtitleSheet.contains("nextLanguageKey"))
        assertTrue(!subtitleSheet.contains("% encodings.size"))
        assertTrue(!subtitleSheet.contains("% subtitleBgStyles.size"))
        assertTrue(displaySheet.contains("AppleActionSheet.showPicker"))
        assertTrue(!displaySheet.contains("aspectIndex + 1"))
        assertTrue(displayActivity.contains("AppleActionSheet.showPicker"))
        assertTrue(contentFrame.contains("AppleActionSheet.showPicker"))
        assertTrue(!contentFrame.contains("% modes.size"))
        assertTrue(audioSheet.contains("AppleActionSheet.showPicker"))
        assertTrue(!audioSheet.contains("% channels.size"))
        assertTrue(gestureSheet.contains("AppleActionSheet.showPicker"))
        assertTrue(!gestureSheet.contains("% gestureActions.size"))
        assertTrue(speedOptions.contains("0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f"))
        assertTrue(dialog.contains("PlayerGlassSheetChrome.PLAYER_BOTTOM"))
        assertTrue(dialog.contains("PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL"))
    }

    private fun Path.readText(): String = String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
