package com.openvideo.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryFilterBoundaryTest {
    @Test
    fun durationRangesHaveNoGapsOrOverlapAtFiveAndSixtyMinutes() {
        val durations = listOf(-1L, 0L, 1L, 300_000L, 300_001L, 3_600_000L, 3_600_001L)
        val expectations = mapOf(
            DurationFilter.ANY to listOf(true, true, true, true, true, true, true),
            DurationFilter.SHORT to listOf(false, false, true, true, false, false, false),
            DurationFilter.MEDIUM to listOf(false, false, false, false, true, true, false),
            DurationFilter.LONG to listOf(false, false, false, false, false, false, true)
        )
        expectations.forEach { (filter, expected) ->
            durations.forEachIndexed { index, duration ->
                assertEquals("$filter at $duration", expected[index], matches(duration = duration, filters = MediaLibraryAdvancedFilters(durationFilter = filter)))
            }
        }
    }

    @Test
    fun dateFiltersIncludeTheirBoundaryAndTreatFutureMediaAsToday() {
        val day = 86_400L
        val ages = listOf(-1L, 0L, day, day + 1, 7 * day, 7 * day + 1, 30 * day, 30 * day + 1)
        val expectations = mapOf(
            DateFilter.ANY to listOf(true, true, true, true, true, true, true, true),
            DateFilter.TODAY to listOf(true, true, true, false, false, false, false, false),
            DateFilter.LAST_7_DAYS to listOf(true, true, true, true, true, false, false, false),
            DateFilter.LAST_30_DAYS to listOf(true, true, true, true, true, true, true, false),
            DateFilter.OLDER_THAN_30_DAYS to listOf(false, false, false, false, false, false, false, true)
        )
        expectations.forEach { (filter, expected) ->
            ages.forEachIndexed { index, age ->
                assertEquals("$filter at $age", expected[index], matches(age = age, filters = MediaLibraryAdvancedFilters(dateFilter = filter)))
            }
        }
    }

    @Test
    fun formatFilterAcceptsBlankAndIgnoresCase() {
        for (extension in listOf(null, "", " ", "MP4", "mp4")) {
            assertTrue(matches(filters = MediaLibraryAdvancedFilters(formatExtension = extension)))
        }
        assertFalse(matches(filters = MediaLibraryAdvancedFilters(formatExtension = "mkv")))
        assertEquals("", MediaLibrarySearchPolicy.fileExtension("/Movies/README"))
        assertEquals("mkv", MediaLibrarySearchPolicy.fileExtension("/Movies/MOVIE.MKV"))
    }

    @Test
    fun filterDraftRoundTripPreservesSelectionsAndResetClearsAllFilters() {
        val filters = MediaLibraryAdvancedFilters(DurationFilter.LONG, "mkv", DateFilter.LAST_30_DAYS)
        assertEquals(filters, VideoLibraryFilterUiState.from(filters).toAdvancedFilters())
        assertTrue(filters.isActive())
        assertFalse(VideoLibraryFilterUiState.default().toAdvancedFilters().isActive())
        assertFalse(MediaLibraryAdvancedFilters(formatExtension = " ").isActive())
        assertTrue(MediaLibraryAdvancedFilters(formatExtension = "mp4").isActive())
        assertTrue(MediaLibraryAdvancedFilters(dateFilter = DateFilter.TODAY).isActive())
    }

    private fun matches(duration: Long = 1_000, age: Long = 0, filters: MediaLibraryAdvancedFilters): Boolean =
        MediaLibrarySearchPolicy.matchesAdvanced("/Movies/movie.mp4", duration, 10_000_000 - age, filters, 10_000_000)
}
