package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.data.model.VideoItem

object PlaylistInsertion {

    data class VideoInput(
        val id: Long,
        val title: String,
        val uri: String,
        val duration: Long
    )

    fun createEntry(
        playlistId: Long,
        existing: List<PlaylistVideoEntity>,
        video: VideoItem,
        mediaIdentityId: Long? = null
    ): PlaylistVideoEntity? {
        return createEntry(
            playlistId = playlistId,
            existing = existing,
            video = VideoInput(
                id = video.id,
                title = video.title,
                uri = video.uri.toString(),
                duration = video.duration
            ),
            mediaIdentityId = mediaIdentityId
        )
    }

    fun createEntry(
        playlistId: Long,
        existing: List<PlaylistVideoEntity>,
        video: VideoInput,
        mediaIdentityId: Long? = null
    ): PlaylistVideoEntity? {
        if (existing.any { it.videoId == video.id }) return null

        return PlaylistVideoEntity(
            playlistId = playlistId,
            videoId = video.id,
            mediaIdentityId = mediaIdentityId,
            videoTitle = video.title,
            videoPath = video.uri,
            videoDuration = video.duration,
            position = existing.size
        )
    }
}
