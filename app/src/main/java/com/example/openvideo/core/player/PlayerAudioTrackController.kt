package com.example.openvideo.core.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
internal class PlayerAudioTrackController(
    private val playerProvider: () -> ExoPlayer?,
    private val onMuteChanged: (Boolean) -> Unit
) {
    fun currentAudioTracks(): List<PlayerAudioTrackInfo> {
        val player = playerProvider() ?: return emptyList()
        return player.currentTracks.groups
            .mapIndexedNotNull { groupIndex, group ->
                if (group.type != C.TRACK_TYPE_AUDIO) return@mapIndexedNotNull null
                (0 until group.length).map { trackIndex ->
                    val format = group.getTrackFormat(trackIndex)
                    PlayerAudioTrackInfo(
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        mimeType = format.sampleMimeType.orEmpty(),
                        language = format.language,
                        channelCount = format.channelCount,
                        sampleRate = format.sampleRate,
                        bitrate = format.bitrate,
                        selected = group.isTrackSelected(trackIndex),
                        supported = group.isTrackSupported(trackIndex)
                    )
                }
            }
            .flatten()
    }

    fun selectAudioTrack(groupIndex: Int, trackIndex: Int) {
        val player = playerProvider() ?: return
        val group = player.currentTracks.groups.getOrNull(groupIndex) ?: return
        if (group.type != C.TRACK_TYPE_AUDIO || trackIndex !in 0 until group.length) return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
            .build()
        onMuteChanged(false)
    }

    fun disableAudioTrack() {
        val player = playerProvider() ?: return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
        onMuteChanged(true)
    }
}
