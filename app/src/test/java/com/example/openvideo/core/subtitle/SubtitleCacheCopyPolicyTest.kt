package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SubtitleCacheCopyPolicyTest {

    @Test
    fun plansSafeCacheCopyNameInsideSubtitleCacheDirectory() {
        val target = SubtitleCacheCopyPolicy.cacheTarget(
            sourceName = "../Movie:01.ass",
            purpose = SubtitleCacheCopyPurpose.DELAY_CORRECTED,
            createdAtMs = 1_725_000_123_456L
        )

        assertEquals("subtitles", target.directoryName)
        assertEquals("Movie_01.delay-corrected.1725000123456.srt", target.fileName)
        assertFalse(target.fileName.contains(".."))
        assertFalse(target.fileName.contains("/"))
        assertFalse(target.fileName.contains("\\"))
        assertFalse(target.overwritesOriginal)
    }

    @Test
    fun plansRetentionByRemovingOldestCopiesPastLimit() {
        val copies = listOf(
            copy("old.srt", createdAtMs = 1_000L),
            copy("new.srt", createdAtMs = 3_000L),
            copy("middle.srt", createdAtMs = 2_000L)
        )

        val plan = SubtitleCacheCopyPolicy.planRetention(
            existingCopies = copies,
            maxCopies = 2
        )

        assertEquals(listOf(copy("new.srt", 3_000L), copy("middle.srt", 2_000L)), plan.keep)
        assertEquals(listOf(copy("old.srt", 1_000L)), plan.delete)
        assertEquals(2, plan.maxCopies)
    }

    @Test
    fun retentionNeverDeletesWhenWithinLimit() {
        val copies = listOf(copy("only.srt", createdAtMs = 1_000L))

        val plan = SubtitleCacheCopyPolicy.planRetention(
            existingCopies = copies,
            maxCopies = 3
        )

        assertEquals(copies, plan.keep)
        assertEquals(emptyList<SubtitleCacheCopy>(), plan.delete)
    }

    private fun copy(fileName: String, createdAtMs: Long): SubtitleCacheCopy =
        SubtitleCacheCopy(
            fileName = fileName,
            createdAtMs = createdAtMs,
            sizeBytes = 100L
        )
}
