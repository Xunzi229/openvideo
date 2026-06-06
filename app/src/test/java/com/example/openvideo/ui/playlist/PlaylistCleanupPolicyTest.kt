package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistCleanupPolicyTest {

    @Test
    fun planKeepsEarliestDuplicateByMediaIdentityAndRemovesLaterRows() {
        val plan = PlaylistCleanupPolicy.plan(
            videos = listOf(
                video(videoId = 2, mediaIdentityId = 42, position = 2, path = "content://media/external/video/2"),
                video(videoId = 1, mediaIdentityId = 42, position = 0, path = "content://media/external/video/1"),
                video(videoId = 3, mediaIdentityId = 99, position = 1, path = "content://media/external/video/3")
            )
        )

        assertEquals(emptyList<Long>(), plan.missingVideoIds)
        assertEquals(listOf(2L), plan.duplicateVideoIds)
        assertEquals(listOf(2L), plan.removableVideoIds)
    }

    @Test
    fun planFallsBackToNormalizedPathWhenIdentityIsMissing() {
        val plan = PlaylistCleanupPolicy.plan(
            videos = listOf(
                video(videoId = 1, position = 0, path = "/Movies/Show.S01E01.mkv"),
                video(videoId = 2, position = 1, path = "\\Movies\\SHOW.S01E01.mkv/"),
                video(videoId = 3, position = 2, path = "/Movies/Show.S01E02.mkv")
            )
        )

        assertEquals(listOf(2L), plan.duplicateVideoIds)
    }

    @Test
    fun planIncludesMissingRowsFromAvailabilityPolicy() {
        val plan = PlaylistCleanupPolicy.plan(
            videos = listOf(
                video(videoId = 1, position = 0, path = "content://media/external/video/1"),
                video(videoId = 2, position = 1, path = "/definitely/missing/openvideo-${System.nanoTime()}.mkv")
            )
        )

        assertEquals(listOf(2L), plan.missingVideoIds)
        assertEquals(emptyList<Long>(), plan.duplicateVideoIds)
        assertEquals(listOf(2L), plan.removableVideoIds)
    }
}

private fun video(
    videoId: Long,
    mediaIdentityId: Long? = null,
    position: Int,
    path: String
): PlaylistVideoEntity = PlaylistVideoEntity(
    playlistId = 7,
    videoId = videoId,
    mediaIdentityId = mediaIdentityId,
    videoTitle = "Video $videoId",
    videoPath = path,
    videoDuration = 60_000,
    position = position
)
