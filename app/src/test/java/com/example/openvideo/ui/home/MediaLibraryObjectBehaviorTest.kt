package com.example.openvideo.ui.home

import android.app.Application
import android.net.Uri
import com.example.openvideo.core.metadata.MediaSmartListType
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.local.VideoFolderGrouper
import com.example.openvideo.ui.player.PlayerEpisodeOrderingPolicy
import com.example.openvideo.ui.playlist.PlaylistInsertion
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class MediaLibraryObjectBehaviorTest {
    private fun video(id: Long, folder: String = "Movies") = VideoItem(id, "Episode $id", "content://media/$id", Uri.parse("content://media/$id"),
        100_000, 2_000_000_000, 3840, 2160, id * 100, null, "/$folder/$id.mp4", id * 200)

    @Test fun visibleVideosAndScanSignaturesUseLibraryPathsAndModificationTimes() {
        val first = video(1)
        val second = video(2, "Hidden")
        val third = video(3, "Other")
        val videos = listOf(first, second, third)
        assertEquals(listOf(first, third), MediaLibraryPolicy.visibleVideos(videos, listOf("/Hidden")))
        assertEquals(listOf(first), MediaLibraryPolicy.visibleVideos(videos, listOf("/Hidden"), "/Movies"))
        assertEquals(listOf("/Hidden/2.mp4" to 400L, "/Movies/1.mp4" to 200L, "/Other/3.mp4" to 600L), MediaScanSignature.fromVideos(videos).entries)
        assertFalse(MediaLibraryPolicy.shouldPublishScan(MediaScanSignature.fromVideos(videos), MediaScanSignature.fromVideos(videos.reversed())))
        assertTrue(MediaLibraryPolicy.shouldPublishScan(MediaScanSignature.fromVideos(videos), MediaScanSignature.fromVideos(listOf(first.copy(dateModified = 300)))))
        assertFalse(MediaLibraryPolicy.shouldExposeStoredFallback(" ", emptyList()) { fail("blank path reached filesystem"); true })
        assertFalse(MediaLibraryPolicy.shouldExposeStoredFallback("/missing", emptyList()) { false })
    }

    @Test fun folderGroupingAndPlaylistInsertionPreservePlaybackIdentity() {
        val videos = listOf(video(1), video(3), video(2, "Other"))
        val folders = VideoFolderGrouper.groupVideos(videos)
        assertEquals(listOf("Movies", "Other"), folders.map { it.name })
        assertEquals(listOf(2, 1), folders.map { it.videoCount })
        assertEquals(listOf(3L, 1L), folders.first().videos.map { it.id })
        assertEquals("/Movies", folders.first().key)
        assertFalse(folders.first().isPinned)
        assertEquals(VideoFolderGrouper.UNKNOWN_FOLDER_NAME, VideoFolderGrouper.folderName("/"))
        val row = PlaylistInsertion.createEntry(10, emptyList(), videos[0], 42)!!
        assertEquals("content://media/1", row.videoPath)
        assertEquals(42L, row.mediaIdentityId)
        assertEquals(100_000L, row.videoDuration)
        assertNull(PlaylistInsertion.createEntry(10, listOf(row), videos[0]))
    }

    @Test fun smartListsUseLatestHistoryAndReturnOriginalVideoObjects() {
        val videos = listOf(video(1), video(2), video(3))
        fun history(id: Long, position: Long, timestamp: Long, subtitle: String = "") = HistoryEntity(
            videoId = id, title = "title", path = "content://media/$id", duration = 100_000,
            lastPosition = position, timestamp = timestamp, externalSubtitleUri = subtitle)
        val sections = HomeSmartListBuilder.build(videos, listOf(history(1, 95_000, 1), history(1, 50_000, 2, "content://subtitle/1"), history(2, 95_000, 3)))
        assertEquals(listOf(3L, 2L, 1L), sections.first().videos.map { it.id })
        assertEquals(listOf(videos[0]), sections.single { it.type == MediaSmartListType.IN_PROGRESS }.videos)
        assertEquals(listOf(videos[1]), sections.single { it.type == MediaSmartListType.COMPLETED }.videos)
        assertEquals(listOf(videos[0]), sections.single { it.type == MediaSmartListType.WITH_SUBTITLES }.videos)
        assertTrue(HomeSmartListBuilder.build(videos, emptyList(), limit = 0).isEmpty())
        assertTrue(HomeSmartListBuilder.build(emptyList(), emptyList()).isEmpty())
    }

    @Test fun episodeOrderingUsesTitlesAndPreservesQueuesWithoutEpisodeSignal() {
        val ordered = listOf(video(1).copy(title = "Show S01E01"), video(2).copy(title = "Show S01E02"))
        assertEquals(ordered, PlayerEpisodeOrderingPolicy.orderSameFolderQueue(ordered.reversed()))
        val movies = listOf(video(2).copy(title = "Z movie"), video(1).copy(title = "A movie"))
        assertSame(movies, PlayerEpisodeOrderingPolicy.orderQueueIfEligible(movies))
        val paths = ordered.map { it.copy(title = "", libraryPath = "/Movies/Show S01E0${it.id}.mp4") }
        assertEquals(paths, PlayerEpisodeOrderingPolicy.orderQueueIfEligible(paths.reversed()))
    }
}
