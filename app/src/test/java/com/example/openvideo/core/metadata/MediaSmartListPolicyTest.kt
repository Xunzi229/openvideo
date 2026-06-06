package com.example.openvideo.core.metadata

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSmartListPolicyTest {

    @Test
    fun recentlyAddedSortsByNewestDateAdded() {
        val result = MediaSmartListPolicy.itemsFor(
            type = MediaSmartListType.RECENTLY_ADDED,
            items = listOf(
                item(id = 1, dateAdded = 100),
                item(id = 2, dateAdded = 300),
                item(id = 3, dateAdded = 200)
            )
        )

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun inProgressExcludesUnwatchedAndCompletedItems() {
        val result = MediaSmartListPolicy.itemsFor(
            type = MediaSmartListType.IN_PROGRESS,
            items = listOf(
                item(id = 1, durationMs = 100_000, lastPositionMs = null),
                item(id = 2, durationMs = 100_000, lastPositionMs = 40_000),
                item(id = 3, durationMs = 100_000, lastPositionMs = 95_000)
            )
        )

        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun completedIncludesItemsInsideEndWindow() {
        val result = MediaSmartListPolicy.itemsFor(
            type = MediaSmartListType.COMPLETED,
            items = listOf(
                item(id = 1, durationMs = 120_000, lastPositionMs = 109_999),
                item(id = 2, durationMs = 120_000, lastPositionMs = 110_000),
                item(id = 3, durationMs = 0, lastPositionMs = 1)
            )
        )

        assertEquals(listOf(2L, 3L), result.map { it.id })
    }

    @Test
    fun largeFilesUseConfigurableThresholdAndSortLargestFirst() {
        val result = MediaSmartListPolicy.itemsFor(
            type = MediaSmartListType.LARGE_FILES,
            items = listOf(
                item(id = 1, sizeBytes = 700),
                item(id = 2, sizeBytes = 1_500),
                item(id = 3, sizeBytes = 1_100)
            ),
            largeFileThresholdBytes = 1_000
        )

        assertEquals(listOf(2L, 3L), result.map { it.id })
    }

    @Test
    fun qualityAndSubtitleListsFilterByItemCapabilities() {
        val items = listOf(
            item(id = 1, width = 3840, height = 1600),
            item(id = 2, width = 1920, height = 1080, isHdr = true),
            item(id = 3, width = 1280, height = 720, hasExternalSubtitle = true)
        )

        assertEquals(listOf(1L), MediaSmartListPolicy.itemsFor(MediaSmartListType.UHD, items).map { it.id })
        assertEquals(listOf(2L), MediaSmartListPolicy.itemsFor(MediaSmartListType.HDR, items).map { it.id })
        assertEquals(listOf(3L), MediaSmartListPolicy.itemsFor(MediaSmartListType.WITH_SUBTITLES, items).map { it.id })
    }

    @Test
    fun limitCapsResultSizeAfterSorting() {
        val result = MediaSmartListPolicy.itemsFor(
            type = MediaSmartListType.RECENTLY_ADDED,
            items = listOf(item(id = 1, dateAdded = 100), item(id = 2, dateAdded = 200)),
            limit = 1
        )

        assertEquals(listOf(2L), result.map { it.id })
    }

    private fun item(
        id: Long,
        title: String = "Video $id",
        path: String = "/Movies/video-$id.mkv",
        durationMs: Long = 100_000,
        sizeBytes: Long = 100,
        width: Int = 1920,
        height: Int = 1080,
        dateAdded: Long = 0,
        lastPositionMs: Long? = null,
        hasExternalSubtitle: Boolean = false,
        isHdr: Boolean = false
    ): MediaSmartListItem = MediaSmartListItem(
        id = id,
        title = title,
        path = path,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        dateAdded = dateAdded,
        lastPositionMs = lastPositionMs,
        hasExternalSubtitle = hasExternalSubtitle,
        isHdr = isHdr
    )
}
