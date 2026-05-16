package com.example.openvideo.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreDiffPolicyTest {

    @Test
    fun diffDetectsAddedRemovedAndChangedEntries() {
        val previous = mapOf(
            1L to entry(1L, "a.mp4", dateAdded = 10),
            2L to entry(2L, "b.mp4", dateAdded = 20),
            3L to entry(3L, "c.mp4", dateAdded = 30)
        )
        val current = mapOf(
            1L to entry(1L, "a.mp4", dateAdded = 10),
            2L to entry(2L, "b-renamed.mp4", dateAdded = 20),
            4L to entry(4L, "d.mp4", dateAdded = 40)
        )

        val diff = MediaStoreDiffPolicy.diff(previous, current)

        assertEquals(setOf(3L), diff.removedIds)
        assertEquals(setOf(4L), diff.addedIds)
        assertEquals(setOf(2L), diff.changedIds)
        assertEquals(3, diff.mutationCount)
    }

    @Test
    fun unchangedIndexProducesEmptyDiff() {
        val index = mapOf(1L to entry(1L, "a.mp4", dateAdded = 10))

        val diff = MediaStoreDiffPolicy.diff(index, index)

        assertEquals(0, diff.mutationCount)
    }

    private fun entry(
        id: Long,
        name: String,
        dateAdded: Long
    ) = MediaStoreIndexEntry(
        id = id,
        displayName = name,
        dateAdded = dateAdded,
        duration = 60_000,
        size = 1_000,
        width = 1920,
        height = 1080
    )
}
