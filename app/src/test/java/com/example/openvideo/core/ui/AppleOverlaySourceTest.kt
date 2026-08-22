package com.example.openvideo.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AppleOverlaySourceTest {

    @Test
    fun overlayTokensDefineAppleAlertAndSheetSurfaces() {
        val light = rootFile("app", "src", "main", "res", "values", "design_tokens.xml").readText()
        val dark = rootFile("app", "src", "main", "res", "values-night", "design_tokens.xml").readText()

        assertTrue(light.contains("ov_overlay_card"))
        assertTrue(light.contains("ov_overlay_alert"))
        assertTrue(light.contains("ov_danger"))
        assertTrue(light.contains("<dimen name=\"ov_alert_width\">270dp</dimen>"))
        assertTrue(light.contains("ov_overlay_sheet_max_width"))
        assertTrue(light.contains("ov_hud_bg"))
        assertTrue(light.contains("ov_segment_selected"))
        assertTrue(light.contains("#FFFF3B30"))
        assertTrue(dark.contains("#FFFF453A"))
        assertTrue(dark.contains("#FF0A84FF"))
    }

    @Test
    fun threeChromeTemplatesShareOverlayInsetsAndTokens() {
        val actionSheet = source("AppleActionSheet.kt")
        val alert = source("AppleAlertDialog.kt")
        val formSheet = source("AppleFormSheet.kt")

        assertTrue(actionSheet.contains("Gravity.BOTTOM"))
        assertTrue(actionSheet.contains("AppleOverlayChrome.configureBottomWindow"))
        assertTrue(actionSheet.contains("OverlayWindowInsets.bind"))
        assertTrue(actionSheet.contains("defaultFocusCancel"))
        assertFalse(actionSheet.contains("MaterialAlertDialogBuilder"))

        assertTrue(alert.contains("Gravity.CENTER"))
        assertTrue(alert.contains("AppleOverlayChrome.configureCenterWindow"))
        assertTrue(alert.contains("AppleOverlayChrome.enterCenter"))
        assertTrue(alert.contains("OverlayWindowInsets.bind"))
        assertFalse(alert.contains("MaterialAlertDialogBuilder"))
        assertFalse(alert.contains("setBackgroundBlurRadius"))

        assertTrue(formSheet.contains("Gravity.BOTTOM"))
        assertTrue(formSheet.contains("overlayMaxHeight"))
        assertTrue(formSheet.contains("OverlayWindowInsets.bind"))
        assertFalse(formSheet.contains("MaterialAlertDialogBuilder"))
    }

    @Test
    fun videoOptionsAndDeletesUseAppleChrome() {
        val options = uiSource("home", "VideoOptionsSheet.kt")
        val home = uiSource("home", "HomeFragment.kt")
        val folder = uiSource("local", "FolderVideosFragment.kt")
        val filter = uiSource("home", "VideoLibraryFilterPopover.kt")

        assertTrue(options.contains("AppleActionSheet.show"))
        assertTrue(options.contains("AppleAlertDialog.show"))
        assertTrue(options.contains("defaultFocusCancel = false"))
        assertTrue(options.contains("AppleActionStyle.DESTRUCTIVE"))
        assertFalse(options.contains("BottomSheetDialog"))
        assertFalse(options.contains("MaterialAlertDialogBuilder"))

        val homeDelete = home.substringAfter("private fun confirmDelete(video: VideoItem)")
            .substringBefore("\n    private fun startMultiSelectMode(")
        val homeBatch = home.substringAfter("private fun confirmDeleteSelected()")
            .substringBefore("\n    private fun deleteVideosWithSystemRequest(")
        val folderDelete = folder.substringAfter("private fun confirmDelete(video: VideoItem)")
            .substringBefore("\n    private fun deleteVideosWithSystemRequest(")

        listOf(homeDelete, homeBatch, folderDelete).forEach { block ->
            assertTrue(block.contains("AppleAlertDialog.show"))
            assertTrue(block.contains("AppleActionStyle.CANCEL"))
            assertTrue(block.contains("AppleActionStyle.DESTRUCTIVE"))
            assertFalse(block.contains("MaterialAlertDialogBuilder"))
        }

        assertTrue(filter.contains("AppleFormSheet.show"))
        assertTrue(filter.contains("view_video_library_filter_popover"))
        assertFalse(filter.contains("PopupWindow"))
    }

    @Test
    fun remainingAppDialogsUseAppleChrome() {
        val home = uiSource("home", "HomeFragment.kt")
        val folder = uiSource("local", "FolderVideosFragment.kt")
        val playlist = uiSource("playlist", "PlaylistFragment.kt")
        val playlistDetail = uiSource("playlist", "PlaylistDetailFragment.kt")
        val privacy = uiSource("privacy", "PrivacyFragment.kt")
        val url = uiSource("player", "NetworkOpenUrlDialog.kt")
        val webdav = uiSource("sources", "WebDavSourceDialog.kt")
        val subtitle = uiSource("player", "PlayerSubtitleController.kt")
        val subtitleSheet = uiSource("player", "PlayerSubtitleSettingsSheet.kt")
        val glass = uiSource("player", "PlayerGlassSheetDialog.kt")
        val settings = uiSource("settings", "SettingsFragment.kt")

        assertTrue(home.contains("AppleActionSheet.show"))
        assertTrue(home.contains("AppleAlertDialog.show"))
        assertFalse(home.contains("MaterialAlertDialogBuilder"))
        assertTrue(folder.contains("AppleActionSheet.show"))
        assertFalse(folder.contains("MaterialAlertDialogBuilder"))
        assertTrue(playlist.contains("AppleAlertDialog.show"))
        assertFalse(playlist.contains("MaterialAlertDialogBuilder"))
        assertTrue(playlistDetail.contains("AppleAlertDialog.show"))
        assertFalse(playlistDetail.contains("MaterialAlertDialogBuilder"))
        assertTrue(privacy.contains("AppleAlertDialog.show"))
        assertFalse(privacy.contains("MaterialAlertDialogBuilder"))
        assertTrue(url.contains("AppleAlertDialog.show"))
        assertTrue(webdav.contains("AppleFormSheet.show"))
        assertTrue(subtitle.contains("AppleActionSheet.show"))
        assertTrue(subtitleSheet.contains("AppleAlertDialog.show"))
        assertFalse(glass.contains("MaterialAlertDialogBuilder"))
        assertTrue(glass.contains("R.color.ov_accent_blue"))
        assertTrue(glass.contains("applyBottomRowVisual("))
        assertTrue(settings.contains("AppleActionSheet.show"))
        assertFalse(settings.contains("MaterialAlertDialogBuilder"))
    }

    @Test
    fun libraryFeedbackUsesAppleHudInsteadOfToastOrSnackbar() {
        val hud = source("AppleHud.kt")
        val playlistDetail = uiSource("playlist", "PlaylistDetailFragment.kt")
        val settings = uiSource("settings", "SettingsFragment.kt")

        assertTrue(hud.contains("SHORT_MS"))
        assertTrue(hud.contains("Gravity.CENTER"))
        assertTrue(playlistDetail.contains("AppleHud.show"))
        assertTrue(!playlistDetail.contains("Snackbar"))
        assertTrue(settings.contains("AppleHud.show"))
        assertTrue(!settings.contains("Toast.makeText"))
    }

    private fun source(name: String): String = String(
        Files.readAllBytes(rootFile("app", "src", "main", "java", "com", "example", "openvideo", "core", "ui", name))
    )

    private fun uiSource(vararg parts: String): String = String(
        Files.readAllBytes(rootFile("app", "src", "main", "java", "com", "example", "openvideo", "ui", *parts))
    )

    private fun Path.readText(): String = String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
