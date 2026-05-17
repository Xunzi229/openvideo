package com.example.openvideo.ui.player

import com.example.openvideo.core.player.PlayerAudioTrackInfo

sealed interface PlayerQuickEntryAction {
    data class SelectAudioTrack(val groupIndex: Int, val trackIndex: Int) : PlayerQuickEntryAction
    data object DisableAudio : PlayerQuickEntryAction
    data class SetSubtitlesEnabled(val enabled: Boolean) : PlayerQuickEntryAction
    data class SubtitleDelayStatus(val delayMs: Int) : PlayerQuickEntryAction
    data class AdjustSubtitleDelay(val deltaMs: Int) : PlayerQuickEntryAction
    data object ResetSubtitleDelay : PlayerQuickEntryAction
    data object PickSubtitleFile : PlayerQuickEntryAction
    data object OpenSubtitleSettings : PlayerQuickEntryAction
    data object None : PlayerQuickEntryAction
}

data class PlayerQuickEntryItem(
    val label: String,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val action: PlayerQuickEntryAction
)

data class PlayerQuickEntryState(
    val items: List<PlayerQuickEntryItem>
)

object PlayerQuickEntryPolicy {

    fun subtitleDelayAdjustIsDecrease(deltaMs: Int): Boolean = deltaMs < 0

    fun audioEntry(
        tracks: List<PlayerAudioTrackInfo>,
        audioMuted: Boolean,
        trackLabel: (PlayerAudioTrackInfo) -> String
    ): PlayerQuickEntryState {
        if (tracks.isEmpty()) {
            return PlayerQuickEntryState(
                items = listOf(
                    PlayerQuickEntryItem(
                        label = "empty",
                        enabled = false,
                        action = PlayerQuickEntryAction.None
                    )
                )
            )
        }

        val items = buildList {
            add(
                PlayerQuickEntryItem(
                    label = "disable",
                    selected = audioMuted,
                    action = PlayerQuickEntryAction.DisableAudio
                )
            )
            tracks.forEach { track ->
                add(
                    PlayerQuickEntryItem(
                        label = trackLabel(track),
                        selected = !audioMuted && track.selected,
                        enabled = track.supported,
                        action = PlayerQuickEntryAction.SelectAudioTrack(track.groupIndex, track.trackIndex)
                    )
                )
            }
        }
        return PlayerQuickEntryState(items)
    }

    fun subtitleEntry(
        hasLoadedSubtitles: Boolean,
        subtitlesEnabled: Boolean,
        subtitleDelayMs: Int
    ): PlayerQuickEntryState {
        val items = mutableListOf<PlayerQuickEntryItem>()
        if (hasLoadedSubtitles) {
            items += PlayerQuickEntryItem(
                label = "subtitle_on",
                selected = subtitlesEnabled,
                action = PlayerQuickEntryAction.SetSubtitlesEnabled(true)
            )
            items += PlayerQuickEntryItem(
                label = "subtitle_off",
                selected = !subtitlesEnabled,
                action = PlayerQuickEntryAction.SetSubtitlesEnabled(false)
            )
            items += PlayerQuickEntryItem(
                label = "subtitle_delay_current",
                enabled = false,
                action = PlayerQuickEntryAction.SubtitleDelayStatus(subtitleDelayMs)
            )
            items += PlayerQuickEntryItem(
                label = "subtitle_delay_minus",
                action = PlayerQuickEntryAction.AdjustSubtitleDelay(-500)
            )
            items += PlayerQuickEntryItem(
                label = "subtitle_delay_plus",
                action = PlayerQuickEntryAction.AdjustSubtitleDelay(500)
            )
            items += PlayerQuickEntryItem(
                label = "subtitle_delay_reset",
                selected = subtitleDelayMs != 0,
                action = PlayerQuickEntryAction.ResetSubtitleDelay
            )
        } else {
            items += PlayerQuickEntryItem(
                label = "subtitle_empty",
                enabled = false,
                action = PlayerQuickEntryAction.None
            )
        }
        items += PlayerQuickEntryItem(
            label = "pick_subtitle",
            action = PlayerQuickEntryAction.PickSubtitleFile
        )
        items += PlayerQuickEntryItem(
            label = "more_subtitle_settings",
            action = PlayerQuickEntryAction.OpenSubtitleSettings
        )
        return PlayerQuickEntryState(items)
    }
}
