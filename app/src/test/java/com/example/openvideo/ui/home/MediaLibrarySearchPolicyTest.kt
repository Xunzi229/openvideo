package com.example.openvideo.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibrarySearchPolicyTest {

    @Test
    fun matchesTitlePathAndFolderName() {
        val path = "/storage/Movies/English/lesson.mp4"

        assertTrue(MediaLibrarySearchPolicy.matchesQuery("lesson.mp4", path, "english"))
        assertTrue(MediaLibrarySearchPolicy.matchesQuery("lesson.mp4", path, "Movies"))
        assertTrue(MediaLibrarySearchPolicy.matchesQuery("lesson.mp4", path, "lesson"))
    }

    @Test
    fun matchesDurationFormatAndDateFilters() {
        val now = 1_700_000_000L
        val filters = MediaLibraryAdvancedFilters(
            durationFilter = DurationFilter.MEDIUM,
            formatExtension = "mp4",
            dateFilter = DateFilter.LAST_7_DAYS
        )

        assertTrue(
            MediaLibrarySearchPolicy.matchesAdvanced(
                path = "/storage/recent.mp4",
                durationMs = 600_000,
                dateAddedSec = now - 86_400,
                filters = filters,
                nowEpochSec = now
            )
        )
        assertFalse(
            MediaLibrarySearchPolicy.matchesAdvanced(
                path = "/storage/archive/old.mkv",
                durationMs = 3_600_000,
                dateAddedSec = now - 40 * 86_400,
                filters = filters,
                nowEpochSec = now
            )
        )
    }

    @Test
    fun combinedSearchAndAdvancedFiltersRequireBoth() {
        val now = 1_700_000_000L
        val path = "/storage/Downloads/clip.mp4"
        val filters = MediaLibraryAdvancedFilters(durationFilter = DurationFilter.SHORT)

        assertTrue(
            MediaLibrarySearchPolicy.matchesLibrary(
                title = "clip.mp4",
                path = path,
                durationMs = 30_000,
                dateAddedSec = now,
                query = "downloads",
                filters = filters,
                nowEpochSec = now
            )
        )
        assertFalse(
            MediaLibrarySearchPolicy.matchesLibrary(
                title = "clip.mp4",
                path = path,
                durationMs = 30_000,
                dateAddedSec = now,
                query = "missing",
                filters = filters,
                nowEpochSec = now
            )
        )
    }

    @Test
    fun fileExtensionIsLowercaseWithoutDot() {
        assertTrue(MediaLibrarySearchPolicy.fileExtension("/storage/a.MP4") == "mp4")
    }
}
