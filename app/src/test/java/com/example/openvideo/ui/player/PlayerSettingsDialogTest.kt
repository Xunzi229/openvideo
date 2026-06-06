package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSettingsDialogTest {

    @Test
    fun playerSettingsUseImmersiveInPlayerSheetInsteadOfSettingsPage() {
        val source = String(Files.readAllBytes(playerSettingsDialogSource()))

        assertTrue(source.contains(": Dialog(context)"))
        assertFalse(source.contains(": BottomSheetDialog(context)"))
        assertTrue(source.contains("setCanceledOnTouchOutside(true)"))
        assertTrue(source.contains("setupPrimaryGrid()"))
        assertTrue(source.contains("showDetailPage("))
        assertTrue(source.contains("showPrimaryPage()"))
        assertTrue(source.contains("PlayerSettingsSheetChrome.applyWindowLayout"))
        assertTrue(source.contains("PlayerSettingsSheetChrome.applyBackdrop"))
        assertTrue(source.contains("PlayerSettingsSheetChrome.applyPanelOpacity"))

        assertFalse(source.contains("PlayerDisplaySettingsActivity"))
        assertFalse(source.contains("PlayerPlaybackSettingsActivity"))
        assertFalse(source.contains("BaseSettingsSheet"))
    }

    @Test
    fun everyPrimarySettingsFeatureHasARealActionOrPage() {
        val source = String(Files.readAllBytes(playerSettingsDialogSource()))

        assertFalse(source.contains("showUnavailable"))
        assertFalse(source.contains("buildUnavailablePage"))
        assertFalse(source.contains("player_sheet_not_available"))

        listOf(
            "buildAudioPage()",
            "buildSubtitlePage()",
            "buildAspectPage()",
            "buildDisplayPage()",
            "buildPlaylistPage()",
            "buildStreamPage()",
            "buildInfoPage()",
            "shareVideoTitle()",
            "buildCutPage()",
            "buildBookmarkPage()",
            "buildTutorialPage()",
            "buildMorePage()"
        ).forEach { expected ->
            assertTrue("Missing real settings handler: $expected", source.contains(expected))
        }
    }

    @Test
    fun aspectPageHasOnlyOneFitStyleOption() {
        val optionsSource = String(Files.readAllBytes(playerAspectRatioOptionsSource()))

        assertFalse(optionsSource.contains("R.string.player_sheet_fit_screen"))
        assertTrue(optionsSource.contains("R.string.player_sheet_original_ratio"))
        assertTrue(optionsSource.contains("AspectRatio.FIT"))
    }

    @Test
    fun aspectPageUsesSharedPlayerAspectOptions() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val aspectPageBlock = dialogSource
            .substringAfter("private fun buildAspectPage()")
            .substringBefore("\n    private fun buildDisplayPage")

        assertTrue(aspectPageBlock.contains("PlayerAspectRatioOptions.entries"))
        assertFalse(aspectPageBlock.contains("addAspectRow(context.getString(R.string.player_sheet_fill_screen), AspectRatio.FILL)"))
    }

    @Test
    fun quickChoiceDialogsUseSharedGlassSheetHelper() {
        val quickDialogSource = String(Files.readAllBytes(sourceFile("PlayerQuickDialogController.kt")))

        assertTrue(quickDialogSource.contains("PlayerGlassSheetDialog.showSingleChoice"))
        assertFalse(quickDialogSource.contains("inflatePlayerGlassSheet("))
        assertFalse(quickDialogSource.contains("applyGlassSheetRowVisual("))
        assertFalse(quickDialogSource.contains("applyPlayerGlassSheetChrome("))
        assertFalse(quickDialogSource.contains("capPlayerGlassSheetScroll("))
    }

    @Test
    fun subtitleDelayAndNetworkStreamAreWiredToPlayback() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val viewModelSource = String(Files.readAllBytes(playerViewModelSource()))

        assertTrue(dialogSource.contains("playerPrefs.subtitleDelayMs = value"))
        assertTrue(viewModelSource.contains("+ playerPrefs.subtitleDelayMs"))
        assertTrue(dialogSource.contains("viewModel.playStream("))
    }

    @Test
    fun playbackPageExposesSeekThumbnailPreviewToggle() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val playbackPage = dialogSource
            .substringAfter("private fun buildPlaylistPage()")
            .substringBefore("\n    private fun buildStreamPage()")

        assertTrue(playbackPage.contains("title = context.getString(R.string.settings_seek_thumbnail_preview)"))
        assertTrue(playbackPage.contains("checked = playerPrefs.seekThumbnailEnabled"))
        assertTrue(playbackPage.contains("playerPrefs.seekThumbnailEnabled = checked"))
    }

    @Test
    fun subtitlePageUsesInlineColorSwatchesInsteadOfTextChoiceRow() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val rowBuilderSource = String(Files.readAllBytes(sourceFile("PlayerSettingsRowBuilder.kt")))
        val subtitlePage = dialogSource
            .substringAfter("private fun buildSubtitlePage()")
            .substringBefore("\n    private fun buildAspectPage()")

        assertTrue(subtitlePage.contains("rows.addSubtitleColorSwatchRow()"))
        assertFalse(subtitlePage.contains("title = context.getString(R.string.settings_subtitle_color)"))
        assertTrue(rowBuilderSource.contains("fun addSubtitleColorSwatchRow()"))
        assertTrue(rowBuilderSource.contains("PlayerSubtitleColorSwatchBinder.bindSwatches"))
    }

    @Test
    fun playlistAndShareActionsAreWiredToRealPlaybackData() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val viewModelSource = String(Files.readAllBytes(playerViewModelSource()))

        assertTrue(dialogSource.contains("viewModel.addCurrentVideoToDefaultPlaylist()"))
        assertTrue(viewModelSource.contains("repository.addToQuickPlaylist("))
        assertTrue(dialogSource.contains("viewModel.currentVideoShareText()"))
    }

    @Test
    fun playerSettingsLayoutHasGridAndDetailPages() {
        val layout = String(Files.readAllBytes(playerSettingsLayout()))

        assertTrue(layout.contains("@+id/settings_grid"))
        assertTrue(layout.contains("android:columnCount=\"4\""))
        assertTrue(layout.contains("@+id/settings_primary_page"))
        assertTrue(layout.contains("@+id/settings_detail_page"))
        assertTrue(layout.contains("@+id/settings_detail_back"))
        assertTrue(layout.contains("@+id/settings_detail_container"))
        assertTrue(layout.contains("@drawable/bg_player_settings_sheet"))
        assertFalse(layout.contains("@+id/nav_playback"))
    }

    @Test
    fun playerSettingsSheetUsesTransparentGlassOverVideo() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val chromeSource = String(Files.readAllBytes(playerSettingsSheetChromeSource()))
        val prefsSource = String(Files.readAllBytes(playerPrefsSource()))
        val portraitSheet = String(Files.readAllBytes(playerSettingsSheetDrawable()))
        val landscapeSheet = String(Files.readAllBytes(playerSettingsLandscapeSheetDrawable()))

        listOf(portraitSheet, landscapeSheet).forEach { sheet ->
            assertFalse(
                "Settings sheet should not use the old near-opaque black background.",
                sheet.contains("#F2000000", ignoreCase = true)
            )
            assertTrue(
                "Settings sheet should use an opaque black fill.",
                sheet.contains("<solid") && sheet.contains("#FF000000", ignoreCase = true)
            )
            assertTrue("Settings sheet keeps rounded corners.", sheet.contains("android:radius=\"28dp\""))
        }

        assertTrue(
            "Android 12+ blur radius is applied from prefs via shared sheet chrome.",
            dialogSource.contains("PlayerSettingsSheetChrome.applyBackdrop") &&
                chromeSource.contains("setBackgroundBlurRadius")
        )
        assertTrue(
            "Dim strength comes from prefs.",
            dialogSource.contains("settingsSheetBackdropDimPercent") &&
                chromeSource.contains("setDimAmount")
        )
        assertTrue(
            "Player settings panel should default to 60% opacity when the user has not changed it.",
            prefsSource.contains("else -> 60")
        )
    }

    @Test
    fun playerSettingsSheetLooksLikeAFloatingRightGlassPanel() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val chromeSource = String(Files.readAllBytes(playerSettingsSheetChromeSource()))
        val layout = String(Files.readAllBytes(playerSettingsLayout()))
        val landscapeSheet = String(Files.readAllBytes(playerSettingsLandscapeSheetDrawable()))

        assertTrue(chromeSource.contains("PlayerSettingsLayoutPolicy.landscapeMarginPx("))
        assertTrue(chromeSource.contains("PlayerSettingsLayoutPolicy.panelBounds"))
        assertTrue(chromeSource.contains("window.decorView.elevation = 20f * density"))
        assertTrue(dialogSource.contains("PlayerSettingsSheetChrome.applyWindowLayout"))

        assertTrue(layout.contains("android:elevation=\"20dp\""))
        assertTrue(layout.contains("android:scrollbarThumbVertical=\"@drawable/bg_player_settings_scrollbar_thumb\""))
        assertTrue(layout.contains("@+id/settings_primary_scroll"))
        assertTrue(layout.contains("@+id/settings_detail_scroll"))
        assertTrue(layout.contains("@drawable/bg_player_settings_bottom_fade"))

        assertTrue("Landscape settings panel should have large rounded corners.", landscapeSheet.contains("android:radius=\"28dp\""))
        assertTrue("Landscape settings panel should use opaque black.", landscapeSheet.contains("#FF000000"))
    }

    @Test
    fun playerSettingsGridUsesCompactSpacing() {
        val primaryGridSource = String(Files.readAllBytes(sourceFile("PlayerSettingsPrimaryGridBuilder.kt")))
        val layout = String(Files.readAllBytes(playerSettingsLayout()))

        assertTrue(layout.contains("android:paddingStart=\"12dp\""))
        assertTrue(layout.contains("android:paddingEnd=\"12dp\""))
        assertTrue(primaryGridSource.contains("minimumHeight = dp(74)"))
        assertTrue(primaryGridSource.contains("setPadding(1, 4, 1, 2)"))
        assertTrue(primaryGridSource.contains("setMargins(dp(1), dp(2), dp(1), dp(2))"))
    }

    @Test
    fun brightnessSettingUsesScreenBrightnessInsteadOfVideoEffects() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val brightnessBlock = dialogSource
            .substringAfter("title = context.getString(R.string.player_sheet_brightness)")
            .substringBefore("title = context.getString(R.string.player_sheet_contrast)")

        assertTrue(brightnessBlock.contains("onScreenBrightnessChanged(value)"))
        assertTrue(brightnessBlock.contains("min = 0"))
        assertTrue(brightnessBlock.contains("context.getString(R.string.settings_theme_system)"))
        assertFalse(
            "Dragging brightness should not repeatedly reconfigure Media3 video effects.",
            brightnessBlock.contains("applyVideoAdjustmentsFromPrefs()")
        )
    }

    @Test
    fun contrastAndSaturationApplyVideoEffectsOnlyWhenDraggingStops() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val contrastBlock = dialogSource
            .substringAfter("title = context.getString(R.string.player_sheet_contrast)")
            .substringBefore("title = context.getString(R.string.player_sheet_saturation)")
        val saturationBlock = dialogSource
            .substringAfter("title = context.getString(R.string.player_sheet_saturation)")
            .substringBefore("rows.addChoiceRow(\r\n            title = context.getString(R.string.settings_rotation)")

        listOf(contrastBlock, saturationBlock).forEach { block ->
            assertTrue(block.contains("commitOnStop = true"))
            assertTrue(block.contains("applyVideoAdjustmentsFromPrefs()"))
        }

        val rowBuilderSource = String(Files.readAllBytes(sourceFile("PlayerSettingsRowBuilder.kt")))
        val seekRow = rowBuilderSource
            .substringAfter("private fun addSeekRow(")
            .substringBefore("\n    private fun addDivider(parent: LinearLayout)")
        assertTrue(seekRow.contains("commitOnStop: Boolean = false"))
        assertTrue(seekRow.contains("pendingValue"))
        assertTrue(seekRow.contains("if (!commitOnStop) onChanged(next)"))
        assertTrue(seekRow.contains("override fun onStopTrackingTouch"))
        assertTrue(seekRow.contains("onChanged(pendingValue)"))
    }

    @Test
    fun playerSettingsRowsRefreshVisibleValuesImmediatelyAfterUserChanges() {
        val rowBuilderSource = String(Files.readAllBytes(sourceFile("PlayerSettingsRowBuilder.kt")))
        val choiceRow = rowBuilderSource
            .substringAfter("private fun addChoiceRow(")
            .substringBefore("\n    private fun addSeekRow(")
        val seekRow = rowBuilderSource
            .substringAfter("private fun addSeekRow(")
            .substringBefore("\n    private fun addDivider(parent: LinearLayout)")

        assertTrue(choiceRow.contains("onSelected(opt)"))
        assertTrue(choiceRow.contains("onNestedChoiceSelected()"))
        assertTrue(seekRow.contains("valueView.text = label(next)"))
        assertTrue(seekRow.indexOf("valueView.text = label(next)") < seekRow.indexOf("if (!commitOnStop) onChanged(next)"))
    }

    @Test
    fun preferenceBackedSeekRowsCommitOnStopUnlessTheyAreSafeWindowBrightness() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))

        listOf(
            "R.string.player_sheet_subtitle_delay",
            "R.string.settings_subtitle_size",
            "R.string.player_sheet_contrast",
            "R.string.player_sheet_saturation",
            "R.string.player_sheet_controls_opacity"
        ).forEach { title ->
            val block = dialogSource
                .substringAfter("title = context.getString($title)")
                .substringBefore(") { value ->")
            assertTrue(
                "$title should not write preferences continuously while the SeekBar is being dragged.",
                block.contains("commitOnStop = true")
            )
        }
    }

    @Test
    fun legacySettingsSheetsDoNotWritePreferencesContinuouslyWhileDragging() {
        listOf(
            legacySettingsSource("PlayerAudioSettingsSheet.kt"),
            legacySettingsSource("PlayerSubtitleSettingsSheet.kt"),
            legacySettingsSource("PlayerSubtitleSettingsActivity.kt")
        ).forEach { sourcePath ->
            val source = String(Files.readAllBytes(sourcePath))
            val seekListeners = source.split("setOnSeekBarChangeListener")
                .drop(1)
                .map { it.substringBefore("})") }
            seekListeners.forEach { listener ->
                assertTrue(
                    "${sourcePath.fileName} should update prefs from onStopTrackingTouch instead of every onProgressChanged tick.",
                    listener.contains("onStopTrackingTouch") &&
                        !listener.substringAfter("onProgressChanged").substringBefore("override fun onStartTrackingTouch")
                            .contains("playerPrefs.")
                )
            }
        }
    }

    @Test
    fun tutorialSettingsUseChineseTextAndNestedDetailPages() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val tutorialBlock = dialogSource
            .substringAfter("private fun buildTutorialPage()")
            .substringBefore("\n    private fun buildMorePage()")

        listOf(
            "Apply MX Player gestures",
            "Apply play/pause gestures",
            "Long press action",
            "Edge swipe back"
        ).forEach { english ->
            assertFalse("Tutorial page should not mix English UI text: $english", tutorialBlock.contains(english))
        }

        assertTrue(tutorialBlock.contains("R.string.player_sheet_tutorial_apply_mx"))
        assertTrue(tutorialBlock.contains("R.string.player_sheet_tutorial_apply_play_pause"))
        assertTrue(tutorialBlock.contains("addActionRows(tutorialActionSpecs())"))
        assertTrue(tutorialBlock.contains("addSwitchRows(tutorialSwitchSpecs(), detailContainer)"))
        assertTrue(dialogSource.contains("R.string.settings_long_press_action"))
        assertTrue(dialogSource.contains("R.string.settings_edge_swipe_back"))
        assertTrue(dialogSource.contains("onClick = ::showDoubleTapActionPage"))
        assertTrue(dialogSource.contains("onClick = ::showLongPressActionPage"))
    }

    @Test
    fun resetDefaultsInvokesCallbackSoActivityCanReapplyPrefs() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val moreBlock = dialogSource
            .substringAfter("private fun buildMorePage()")
            .substringBefore("\n    private fun ")

        assertTrue(moreBlock.contains("rows.addActionRows(moreActionSpecs())"))
        assertTrue(
            "More page should keep skip intro/outro before speed, and keep-screen-on after speed.",
            moreBlock.indexOf("moreIntroSwitchSpecs()") in 0 until moreBlock.indexOf("addPlaybackSpeedSeekRow()") &&
                moreBlock.indexOf("addPlaybackSpeedSeekRow()") in 0 until moreBlock.indexOf("moreScreenSwitchSpecs()")
        )
        assertTrue(dialogSource.contains("playerPrefs.resetToDefaults()"))
        assertTrue(dialogSource.contains("onPlayerPrefsReset()"))

        val quickDialogSource = String(Files.readAllBytes(sourceFile("PlayerQuickDialogController.kt")))
        val activitySource = String(Files.readAllBytes(playerActivitySource()))
        val displayControllerSource = String(Files.readAllBytes(sourceFile("PlayerDisplayController.kt")))
        assertTrue(quickDialogSource.contains("onPlayerPrefsReset = onApplyPlayerSettings"))
        assertTrue(
            "applyPlayerSettings should sync aspect + decode from prefs (e.g. after reset).",
            activitySource.contains("private fun applyPlayerSettings() = playerDisplay.applyPlayerSettings()") &&
                displayControllerSource.contains("viewModel.setAspectRatio(playerPrefs.aspectRatio)") &&
                displayControllerSource.contains("viewModel.setDecodeMode(")
        )
    }

    @Test
    fun aspectRatioSelectionInvokesCallbackSoActivityCanApplyResizeModeImmediately() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val aspectRowBlock = dialogSource
            .substringAfter("private fun addAspectRow(title: String, ratio: AspectRatio)")
            .substringBefore("\n    private fun rebuildCurrentDetail")

        assertTrue(dialogSource.contains("private val onAspectRatioChanged: () -> Unit = {}"))
        assertTrue(aspectRowBlock.contains("PlayerContentFrameSettingsPolicy.onAspectRatioSelected"))
        assertTrue(aspectRowBlock.contains("playerPrefs.aspectRatio = selection.aspectRatio"))
        assertTrue(aspectRowBlock.contains("selection.contentFrameOverride?.let { playerPrefs.contentFrameMode = it }"))
        assertTrue(aspectRowBlock.contains("viewModel.setAspectRatio(selection.aspectRatio)"))
        assertTrue(aspectRowBlock.contains("onAspectRatioChanged()"))

        val activitySource = String(Files.readAllBytes(playerActivitySource()))
        assertTrue(activitySource.contains("onAspectRatioChanged = {"))
        assertTrue(activitySource.contains("smartCrop.clearSession()"))
        assertTrue(activitySource.contains("applyDisplaySettings()"))
    }

    @Test
    fun tutorialChoicePagesStayInsidePlayerSettingsPanel() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val doubleTapPage = dialogSource
            .substringAfter("private fun showDoubleTapActionPage()")
            .substringBefore("\n    private fun showLongPressActionPage()")
        val longPressPage = dialogSource
            .substringAfter("private fun showLongPressActionPage()")
            .substringBefore("\n    private fun buildMorePage()")

        listOf(doubleTapPage, longPressPage).forEach { page ->
            assertTrue(page.contains("openNestedDetailScreen("))
            assertTrue(page.contains("addRadioRow("))
            assertTrue(page.contains("rebuildCurrentDetail(SettingsPage.TUTORIAL"))
            assertFalse(page.contains("showChoicePopup("))
            assertFalse(page.contains("BottomSheetDialog("))
        }
    }

    @Test
    fun playbackSpeedUsesSeekBarRangeInsteadOfChoiceList() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val formatterSource = String(Files.readAllBytes(sourceFile("PlayerSettingsFormatter.kt")))
        val speedRow = dialogSource
            .substringAfter("private fun addPlaybackSpeedSeekRow()")
            .substringBefore("\n    private fun setSeekIntervalFromChoiceLabel")

        assertTrue(dialogSource.contains("private fun addPlaybackSpeedSeekRow("))
        assertTrue(formatterSource.contains("SPEED_MIN = 0.5f"))
        assertTrue(formatterSource.contains("SPEED_MAX = 5.0f"))
        assertTrue(formatterSource.contains("SPEED_STEP = 0.25f"))
        assertTrue(speedRow.contains("formatter.speedToProgress(playerPrefs.speed)"))
        assertTrue(speedRow.contains("formatter.progressToSpeed(progress)"))
        assertTrue(
            "Speed changes should only apply when dragging stops to avoid rapid ExoPlayer reconfiguration.",
            speedRow.contains("commitOnStop = true")
        )

        listOf("private fun buildPlaylistPage()", "private fun buildMorePage()").forEach { marker ->
            val block = dialogSource.substringAfter(marker).substringBefore("\n    private fun ")
            assertTrue("$marker should expose playback speed as a seek row", block.contains("addPlaybackSpeedSeekRow()"))
        }

        assertFalse(dialogSource.contains("setPlaybackSpeedFromChoiceLabel"))
    }

    @Test
    fun playerSettingsPlaybackControlsEntryIsNotNamedPlaylist() {
        val strings = String(Files.readAllBytes(stringsXml()), UTF_8)
        val zhStrings = String(Files.readAllBytes(zhStringsXml()), UTF_8)

        assertTrue(strings.contains("""<string name="player_sheet_playlist">Playback</string>"""))
        assertTrue(zhStrings.contains("""<string name="player_sheet_playlist">播放控制</string>"""))
        assertFalse(zhStrings.contains("""<string name="player_sheet_playlist">播放列表</string>"""))
    }

    private fun playerSettingsDialogSource(): Path {
        return sourceFile("PlayerSettingsDialog.kt")
    }

    private fun sourceFile(fileName: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            fileName
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSettingsSheetChromeSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerSettingsSheetChrome.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun legacySettingsSource(fileName: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            fileName
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSettingsLayout(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "layout",
            "dialog_player_settings.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerActivitySource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerActivity.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerAspectRatioOptionsSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerAspectRatioOptions.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerViewModelSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerViewModel.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerPrefsSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "prefs",
            "PlayerPrefs.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSettingsSheetDrawable(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "drawable",
            "bg_player_settings_sheet.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSettingsLandscapeSheetDrawable(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "drawable-land",
            "bg_player_settings_sheet.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun stringsXml(): Path {
        val relativePath = Paths.get("src", "main", "res", "values", "strings.xml")
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun zhStringsXml(): Path {
        val relativePath = Paths.get("src", "main", "res", "values-zh-rCN", "strings.xml")
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
