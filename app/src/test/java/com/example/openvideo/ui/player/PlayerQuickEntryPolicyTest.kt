package com.example.openvideo.ui.player

import com.example.openvideo.core.player.PlayerAudioTrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQuickEntryPolicyTest {

    @Test
    fun audioEntryMarksSelectedTrackWhenAudioIsActive() {
        val state = PlayerQuickEntryPolicy.audioEntry(
            tracks = listOf(
                track(groupIndex = 0, trackIndex = 0, selected = true),
                track(groupIndex = 0, trackIndex = 1, selected = false)
            ),
            audioMuted = false,
            trackLabel = { "track-${it.trackIndex}" }
        )

        assertEquals(3, state.items.size)
        assertTrue(state.items[1].selected)
        assertFalse(state.items[0].selected)
    }

    @Test
    fun audioEntryMarksDisableRowSelectedWhenMuted() {
        val state = PlayerQuickEntryPolicy.audioEntry(
            tracks = listOf(track(selected = true)),
            audioMuted = true,
            trackLabel = { "track-${it.trackIndex}" }
        )

        assertTrue(state.items.first().selected)
        assertFalse(state.items[1].selected)
    }

    @Test
    fun unsupportedAudioTrackIsDisabled() {
        val state = PlayerQuickEntryPolicy.audioEntry(
            tracks = listOf(track(trackIndex = 1, supported = false)),
            audioMuted = false,
            trackLabel = { "track-${it.trackIndex}" }
        )

        assertFalse(state.items[1].enabled)
    }

    @Test
    fun audioEntryUsesEmptyStateWhenNoTracksExist() {
        val state = PlayerQuickEntryPolicy.audioEntry(
            tracks = emptyList(),
            audioMuted = false,
            trackLabel = { "unused" }
        )

        assertEquals(1, state.items.size)
        assertFalse(state.items.first().enabled)
        assertTrue(state.items.first().action is PlayerQuickEntryAction.None)
    }

    @Test
    fun subtitleEntryReflectsLoadedAndEnabledSelectionState() {
        val enabled = PlayerQuickEntryPolicy.subtitleEntry(
            hasLoadedSubtitles = true,
            subtitlesEnabled = true,
            subtitleDelayMs = 0
        )
        val disabled = PlayerQuickEntryPolicy.subtitleEntry(
            hasLoadedSubtitles = true,
            subtitlesEnabled = false,
            subtitleDelayMs = 0
        )

        assertTrue(enabled.items[0].selected)
        assertFalse(enabled.items[1].selected)
        assertFalse(disabled.items[0].selected)
        assertTrue(disabled.items[1].selected)
    }

    @Test
    fun subtitleEntryAddsDelayShortcutsWhenSubtitlesAreLoaded() {
        val state = PlayerQuickEntryPolicy.subtitleEntry(
            hasLoadedSubtitles = true,
            subtitlesEnabled = true,
            subtitleDelayMs = 500
        )

        assertTrue(state.items.any { it.action == PlayerQuickEntryAction.AdjustSubtitleDelay(-500) })
        assertTrue(state.items.any { it.action == PlayerQuickEntryAction.AdjustSubtitleDelay(500) })
        assertTrue(state.items.any { it.action == PlayerQuickEntryAction.ResetSubtitleDelay && it.selected })
    }

    @Test
    fun subtitleEntryHidesDelayResetSelectionWhenDelayIsZero() {
        val state = PlayerQuickEntryPolicy.subtitleEntry(
            hasLoadedSubtitles = true,
            subtitlesEnabled = true,
            subtitleDelayMs = 0
        )

        val reset = state.items.first { it.action == PlayerQuickEntryAction.ResetSubtitleDelay }
        assertFalse(reset.selected)
    }

    private fun track(
        groupIndex: Int = 0,
        trackIndex: Int = 0,
        selected: Boolean = false,
        supported: Boolean = true
    ) = PlayerAudioTrackInfo(
        groupIndex = groupIndex,
        trackIndex = trackIndex,
        mimeType = "audio/mp4a-latm",
        language = "en",
        channelCount = 2,
        sampleRate = 48000,
        bitrate = 128000,
        selected = selected,
        supported = supported
    )
}
