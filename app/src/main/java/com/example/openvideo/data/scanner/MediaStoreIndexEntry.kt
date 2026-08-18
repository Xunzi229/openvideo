package com.example.openvideo.data.scanner

import com.example.openvideo.data.model.VideoItem

data class MediaStoreIndexEntry(
    val id: Long,
    val displayName: String,
    val libraryPath: String,
    val dateAdded: Long,
    val dateModified: Long,
    val duration: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val orientationDegrees: Int
) {
    companion object {
        fun fromVideo(video: VideoItem): MediaStoreIndexEntry = MediaStoreIndexEntry(
            id = video.id,
            displayName = video.title,
            libraryPath = video.libraryPath,
            dateAdded = video.dateAdded,
            dateModified = video.dateModified,
            duration = video.duration,
            size = video.size,
            width = video.width,
            height = video.height,
            orientationDegrees = video.orientationDegrees
        )
    }
}
