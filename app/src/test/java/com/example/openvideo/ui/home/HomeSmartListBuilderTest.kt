package com.example.openvideo.ui.home

import com.example.openvideo.core.metadata.MediaSmartListType
import com.example.openvideo.data.local.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSmartListBuilderTest {

    @Test
    fun buildSectionsKeepsOnlyNonEmptySmartListsInStableOrder() {
        val videos = listOf(
            video(id = 1, dateAdded = 100, width = 3840, height = 2160, size = 2_000),
            video(id = 2, dateAdded = 300, duration = 100_000),
            video(id = 3, dateAdded = 200, duration = 100_000)
        )
        val history = listOf(
            history(videoId = 2, position = 40_000),
            history(videoId = 3, position = 95_000)
        )

        val sections = HomeSmartListBuilder.buildVideoSections(
            videos = videos,
            history = history,
            largeFileThresholdBytes = 1_000
        )

        assertEquals(
            listOf(
                MediaSmartListType.RECENTLY_ADDED,
                MediaSmartListType.IN_PROGRESS,
                MediaSmartListType.COMPLETED,
                MediaSmartListType.LARGE_FILES,
                MediaSmartListType.UHD
            ),
            sections.map { it.type }
        )
        assertEquals(listOf(2L, 3L, 1L), sections[0].videos.map { it.id })
        assertEquals(listOf(2L), sections[1].videos.map { it.id })
        assertEquals(listOf(3L), sections[2].videos.map { it.id })
        assertEquals(listOf(1L), sections[3].videos.map { it.id })
        assertEquals(listOf(1L), sections[4].videos.map { it.id })
    }

    private fun video(
        id: Long,
        dateAdded: Long = 0,
        duration: Long = 100_000,
        size: Long = 100,
        width: Int = 1920,
        height: Int = 1080
    ): HomeSmartListVideo = HomeSmartListVideo(
        id = id,
        title = "Video $id",
        path = "/Movies/video-$id.mkv",
        durationMs = duration,
        sizeBytes = size,
        width = width,
        height = height,
        dateAdded = dateAdded
    )

    private fun history(videoId: Long, position: Long): HistoryEntity = HistoryEntity(
        videoId = videoId,
        title = "Video $videoId",
        path = "/Movies/video-$videoId.mkv",
        duration = 100_000,
        lastPosition = position,
        timestamp = 1,
        speed = 1f,
        aspectRatioKey = "",
        contentFrameKey = "",
        externalSubtitleUri = "",
        subtitlesEnabled = true,
        audioTrackGroupIndex = -1,
        audioTrackIndex = -1,
        audioMuted = false
    )
}
