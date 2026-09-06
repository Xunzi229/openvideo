package com.openvideo.app.ui.home

import android.app.Application
import android.net.Uri
import com.openvideo.app.data.model.VideoItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class MediaLibraryVideoMethodsTest {
    private val video = VideoItem(
        id = 42, title = "Movie", path = "content://media/video/42", uri = Uri.parse("content://media/video/42"),
        duration = 60_000, size = 1_000, width = 1920, height = 1080, dateAdded = 1_000_000,
        thumbnailUri = null, libraryPath = "/Movies/Trips/holiday.MP4"
    )

    @Test
    fun videoQueryUsesLibraryPathAndAcceptsBlankSearch() {
        for (query in listOf("", "  ", " Movie ", "TRIPS", "holiday")) {
            assertTrue(query, MediaLibrarySearchPolicy.matchesQuery(video, query))
        }
        assertFalse(MediaLibrarySearchPolicy.matchesQuery(video, "missing"))
        assertFalse(MediaLibrarySearchPolicy.matchesQuery(video, "content://"))
    }

    @Test
    fun videoAdvancedFiltersUseItsDurationDateAndLibraryExtension() {
        val filters = MediaLibraryAdvancedFilters(DurationFilter.SHORT, "mp4", DateFilter.TODAY)
        assertTrue(MediaLibrarySearchPolicy.matchesAdvanced(video, filters, 1_000_000))
        assertFalse(MediaLibrarySearchPolicy.matchesAdvanced(video, filters.copy(durationFilter = DurationFilter.LONG), 1_000_000))
        assertFalse(MediaLibrarySearchPolicy.matchesAdvanced(video, filters.copy(formatExtension = "mkv"), 1_000_000))
        assertFalse(MediaLibrarySearchPolicy.matchesAdvanced(video, filters, 2_000_000))
    }

    @Test
    fun videoLibraryMatchRequiresBothQueryAndAdvancedFilters() {
        val filters = MediaLibraryAdvancedFilters(formatExtension = "mp4")
        assertTrue(MediaLibrarySearchPolicy.matchesLibrary(video, "holiday", filters, 1_000_000))
        assertFalse(MediaLibrarySearchPolicy.matchesLibrary(video, "missing", filters, 1_000_000))
        assertFalse(MediaLibrarySearchPolicy.matchesLibrary(video, "holiday", filters.copy(formatExtension = "mkv"), 1_000_000))
    }
}
