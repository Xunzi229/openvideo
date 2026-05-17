package com.example.openvideo.ui.player

object PlayerMediaTracksPolicy {
    fun hasVideoTrack(groupTypes: Iterable<Int>, videoTrackType: Int): Boolean =
        groupTypes.any { it == videoTrackType }
}
