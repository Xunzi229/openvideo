package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerRemoteKeySourceTest {

    @Test
    fun playerActivityMapsRemotePlaybackKeysToExistingTransportActions() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(source.contains("import android.view.KeyEvent"))
        assertTrue(source.contains("override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean"))
        assertTrue(source.contains("KeyEvent.KEYCODE_DPAD_CENTER"))
        assertTrue(source.contains("KeyEvent.KEYCODE_ENTER"))
        assertTrue(source.contains("KeyEvent.KEYCODE_NUMPAD_ENTER"))
        assertTrue(source.contains("KeyEvent.KEYCODE_SPACE"))
        assertTrue(source.contains("KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE"))
        assertTrue(source.contains("togglePlayPauseAndSyncIcon()"))
        assertTrue(source.contains("KeyEvent.KEYCODE_DPAD_LEFT"))
        assertTrue(source.contains("KeyEvent.KEYCODE_MEDIA_REWIND"))
        assertTrue(source.contains("viewModel.seekBackward()"))
        assertTrue(source.contains("KeyEvent.KEYCODE_DPAD_RIGHT"))
        assertTrue(source.contains("KeyEvent.KEYCODE_MEDIA_FAST_FORWARD"))
        assertTrue(source.contains("viewModel.seekForward()"))
        assertTrue(source.contains("else -> super.onKeyDown(keyCode, event)"))
    }

    @Test
    fun playerRemoteKeysRevealControlsAndRespectScreenLock() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(source.contains("private fun runRemoteTransportAction(action: () -> Unit): Boolean"))
        assertTrue(source.contains("PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.TRANSPORT, isScreenLocked)"))
        assertTrue(source.contains("showLockedControls()"))
        assertTrue(source.contains("showControls()"))
    }

    @Test
    fun playerRemoteSeekKeysHandleLongPressRepeatKeyDowns() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val normalized = source.replace(Regex("\\s+"), " ")

        assertTrue(normalized.contains("KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> runRemoteSeekAction(event) { viewModel.seekBackward() }"))
        assertTrue(normalized.contains("KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> runRemoteSeekAction(event) { viewModel.seekForward() }"))
        assertTrue(source.contains("private fun runRemoteSeekAction(event: KeyEvent, action: () -> Unit): Boolean"))
        assertTrue(source.contains("event.repeatCount >= 0"))
        assertTrue(source.contains("return super.onKeyDown(event.keyCode, event)"))
    }

    @Test
    fun playerActivityMapsJklKeyboardShortcutsThroughShortcutGate() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val normalized = source.replace(Regex("\\s+"), " ")

        assertTrue(normalized.contains("KeyEvent.KEYCODE_K -> runKeyboardShortcutAction(keyCode, event) { togglePlayPauseAndSyncIcon() }"))
        assertTrue(normalized.contains("KeyEvent.KEYCODE_J -> runKeyboardShortcutAction(keyCode, event) { viewModel.seekBackward() }"))
        assertTrue(normalized.contains("KeyEvent.KEYCODE_L -> runKeyboardShortcutAction(keyCode, event) { viewModel.seekForward() }"))
        assertTrue(source.contains("private fun runKeyboardShortcutAction(keyCode: Int, event: KeyEvent, action: () -> Unit): Boolean"))
        assertTrue(source.contains("if (!playerPrefs.keyboardShortcuts) return super.onKeyDown(keyCode, event)"))
        assertTrue(source.contains("return runRemoteTransportAction(action)"))
    }

    @Test
    fun playerActivityMapsRemoteChromeVisibilityKeysThroughLockGate() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val normalized = source.replace(Regex("\\s+"), " ")

        assertTrue(normalized.contains("KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_MENU -> runRemoteChromeVisibilityAction { showControls() }"))
        assertTrue(normalized.contains("KeyEvent.KEYCODE_DPAD_DOWN -> runRemoteChromeVisibilityAction { hideControls() }"))
        assertTrue(source.contains("private fun runRemoteChromeVisibilityAction(action: () -> Unit): Boolean"))
        assertTrue(source.contains("PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.CHROME_TOGGLE, isScreenLocked)"))
        assertTrue(source.contains("showLockedControls()"))
    }

    @Test
    fun playerActivityMapsSubtitleAndAudioKeyboardShortcutsThroughSettingsGate() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val normalized = source.replace(Regex("\\s+"), " ")

        assertTrue(normalized.contains("KeyEvent.KEYCODE_S -> runKeyboardSettingsShortcutAction(keyCode, event) { quickDialogs.showSubtitleQuickDialog() }"))
        assertTrue(normalized.contains("KeyEvent.KEYCODE_A -> runKeyboardSettingsShortcutAction(keyCode, event) { quickDialogs.showAudioTrackQuickDialog() }"))
        assertTrue(source.contains("private fun runKeyboardSettingsShortcutAction(keyCode: Int, event: KeyEvent, action: () -> Unit): Boolean"))
        assertTrue(source.contains("if (!playerPrefs.keyboardShortcuts) return super.onKeyDown(keyCode, event)"))
        assertTrue(source.contains("PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SETTINGS, isScreenLocked)"))
        assertTrue(source.contains("showLockedControls()"))
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
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
