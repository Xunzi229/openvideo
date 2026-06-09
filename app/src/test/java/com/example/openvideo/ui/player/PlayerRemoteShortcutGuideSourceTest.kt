package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerRemoteShortcutGuideSourceTest {

    @Test
    fun remoteShortcutGuideDocumentsCurrentPlayerKeyMap() {
        val docPath = rootFile("docs", "roadmap", "tv-remote-keyboard-shortcuts.md")

        assertTrue("Remote shortcut guide should exist", Files.exists(docPath))

        val doc = String(Files.readAllBytes(docPath))
        assertTrue(doc.contains("P5-REMOTE-KEYS-001"))
        assertTrue(doc.contains("Remote and Keyboard Shortcuts"))
        assertTrue(doc.contains("OK / Enter / Numpad Enter / Space / Media Play-Pause"))
        assertTrue(doc.contains("D-pad Left / Media Rewind"))
        assertTrue(doc.contains("D-pad Right / Media Fast-Forward"))
        assertTrue(doc.contains("D-pad Up / Menu"))
        assertTrue(doc.contains("D-pad Down"))
        assertTrue(doc.contains("Back"))
        assertTrue(doc.contains("J / K / L"))
        assertTrue(doc.contains("S / A"))
        assertTrue(doc.contains("playerPrefs.keyboardShortcuts"))
        assertTrue(doc.contains("PlayerLockedInteraction.TRANSPORT"))
        assertTrue(doc.contains("PlayerLockedInteraction.SETTINGS"))
        assertTrue(doc.contains("PlayerLockedInteraction.CHROME_TOGGLE"))
        assertTrue(doc.contains("runRemoteSeekAction(event)"))
    }

    @Test
    fun remoteShortcutGuideStaysAlignedWithPlayerActivityKeyConstants() {
        val docPath = rootFile("docs", "roadmap", "tv-remote-keyboard-shortcuts.md")
        val player = rootText(
            "app",
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

        assertTrue("Remote shortcut guide should exist", Files.exists(docPath))

        val doc = String(Files.readAllBytes(docPath))
        listOf(
            "KEYCODE_DPAD_CENTER",
            "KEYCODE_ENTER",
            "KEYCODE_NUMPAD_ENTER",
            "KEYCODE_SPACE",
            "KEYCODE_MEDIA_PLAY_PAUSE",
            "KEYCODE_DPAD_LEFT",
            "KEYCODE_MEDIA_REWIND",
            "KEYCODE_DPAD_RIGHT",
            "KEYCODE_MEDIA_FAST_FORWARD",
            "KEYCODE_DPAD_UP",
            "KEYCODE_MENU",
            "KEYCODE_DPAD_DOWN",
            "KEYCODE_J",
            "KEYCODE_K",
            "KEYCODE_L",
            "KEYCODE_S",
            "KEYCODE_A"
        ).forEach { key ->
            assertTrue("PlayerActivity should contain $key", player.contains(key))
            assertTrue("Remote shortcut guide should contain $key", doc.contains(key))
        }
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).firstOrNull(Files::exists)
                    ?: relative
            }
}
