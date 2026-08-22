package com.example.openvideo.core.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class OverlayInsetsSourceTest {

    @Test
    fun overlayWindowsBindSharedBottomInsets() {
        val files = listOf(
            source("ui", "home", "VideoLibraryFilterPopover.kt"),
            source("ui", "home", "VideoOptionsSheet.kt"),
            source("ui", "playlist", "PlaylistOptionsActionSheet.kt"),
            source("ui", "playlist", "PlaylistRenameActionSheet.kt"),
            source("ui", "settings", "SettingsConfirmationActionSheet.kt"),
            source("ui", "player", "PlayerGlassSheetDialog.kt"),
            source("ui", "player", "PlayerSettingsSheetChrome.kt"),
            source("ui", "player", "PlayerVideoListDialog.kt"),
            source("ui", "player", "PlayerSettingsDialog.kt"),
            source("ui", "player", "BaseSettingsSheet.kt"),
            source("ui", "player", "NetworkOpenUrlDialog.kt"),
            source("ui", "sources", "WebDavSourceDialog.kt")
        )
        files.forEach { path ->
            val text = String(Files.readAllBytes(path))
            assertTrue("$path must use OverlayWindowInsets", text.contains("OverlayWindowInsets"))
        }
    }

    @Test
    fun filterPopoverKeepsActionsAboveSystemBars() {
        val popover = String(Files.readAllBytes(source("ui", "home", "VideoLibraryFilterPopover.kt")))
        val layout = String(Files.readAllBytes(res("layout", "view_video_library_filter_popover.xml")))
        val theme = String(Files.readAllBytes(res("values", "themes.xml")))

        assertTrue(popover.contains("overlayMaxHeight"))
        assertTrue(popover.contains("filter_popover_scroll"))
        assertTrue(layout.contains("@+id/filter_popover_scroll"))
        assertTrue(layout.contains("@+id/filter_popover_actions"))
        assertTrue(theme.contains("windowOptOutEdgeToEdgeEnforcement"))
    }

    private fun source(vararg parts: String): Path =
        modulePath("src", "main", "java", "com", "example", "openvideo", *parts)

    private fun res(vararg parts: String): Path =
        modulePath("src", "main", "res", *parts)

    private fun modulePath(vararg parts: String): Path {
        val relative = Paths.get(parts.first(), *parts.drop(1).toTypedArray())
        return sequenceOf(relative, Paths.get("app").resolve(relative)).first(Files::exists)
    }
}
