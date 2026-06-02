package com.example.openvideo.core.mediaid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaIdentityMatcherTest {

    @Test
    fun matchesSameVideoIdWhenFingerprintShapeStillMatches() {
        val current = fingerprint(path = "/storage/new/E01.mkv")
        val candidate = candidate(
            identityId = 10L,
            currentVideoId = 42L,
            path = "/storage/old/E01.mkv"
        )

        val result = MediaIdentityMatcher.match(
            currentVideoId = 42L,
            current = current,
            candidates = listOf(candidate)
        )

        assertMatched(result, 10L, MediaIdentityMatchReason.SAME_VIDEO_ID)
    }

    @Test
    fun matchesSamePathWhenVideoIdChanges() {
        val current = fingerprint(path = "/storage/show/E01.mkv")
        val candidate = candidate(
            identityId = 11L,
            currentVideoId = 1L,
            path = "/storage/Show/E01.mkv"
        )

        val result = MediaIdentityMatcher.match(
            currentVideoId = 99L,
            current = current,
            candidates = listOf(candidate)
        )

        assertMatched(result, 11L, MediaIdentityMatchReason.SAME_PATH)
    }

    @Test
    fun matchesPathMigrationWhenFileNameMetricsAndTimestampAreClose() {
        val current = fingerprint(
            path = "/storage/new/E01.mkv",
            timestamp = 1_700_000_000_000L
        )
        val candidate = candidate(
            identityId = 12L,
            currentVideoId = 1L,
            path = "/storage/old/E01.mkv",
            timestamp = 1_700_000_100_000L
        )

        val result = MediaIdentityMatcher.match(
            currentVideoId = 99L,
            current = current,
            candidates = listOf(candidate)
        )

        assertMatched(result, 12L, MediaIdentityMatchReason.PATH_MIGRATION)
    }

    @Test
    fun matchesRenameCandidateWhenMetricsAndDimensionsMatch() {
        val current = fingerprint(path = "/storage/new/Episode 01.mkv", timestamp = 2_000L)
        val candidate = candidate(
            identityId = 13L,
            currentVideoId = 1L,
            path = "/storage/old/E01.mkv",
            timestamp = 1_000L
        )

        val result = MediaIdentityMatcher.match(
            currentVideoId = 99L,
            current = current,
            candidates = listOf(candidate)
        )

        assertMatched(result, 13L, MediaIdentityMatchReason.RENAME_CANDIDATE)
    }

    @Test
    fun returnsConflictWhenMultipleCandidatesMatchSameTier() {
        val current = fingerprint(path = "/storage/new/Episode 01.mkv")
        val first = candidate(identityId = 14L, currentVideoId = 1L, path = "/storage/a/E01.mkv")
        val second = candidate(identityId = 15L, currentVideoId = 2L, path = "/storage/b/E02.mkv")

        val result = MediaIdentityMatcher.match(
            currentVideoId = 99L,
            current = current,
            candidates = listOf(first, second)
        )

        assertTrue(result is MediaIdentityMatchDecision.Conflict)
        val conflict = result as MediaIdentityMatchDecision.Conflict
        assertEquals(MediaIdentityMatchReason.RENAME_CANDIDATE, conflict.reason)
        assertEquals(listOf(14L, 15L), conflict.candidateIdentityIds)
    }

    @Test
    fun returnsNoMatchWhenCandidateMetricsDiffer() {
        val current = fingerprint(path = "/storage/new/E01.mkv", size = 1_000L)
        val candidate = candidate(
            identityId = 16L,
            currentVideoId = 1L,
            path = "/storage/old/E01.mkv",
            size = 2_000L
        )

        val result = MediaIdentityMatcher.match(
            currentVideoId = 99L,
            current = current,
            candidates = listOf(candidate)
        )

        assertEquals(MediaIdentityMatchDecision.NoMatch, result)
    }

    private fun assertMatched(
        result: MediaIdentityMatchDecision,
        identityId: Long,
        reason: MediaIdentityMatchReason
    ) {
        assertTrue(result is MediaIdentityMatchDecision.Matched)
        val matched = result as MediaIdentityMatchDecision.Matched
        assertEquals(identityId, matched.identityId)
        assertEquals(reason, matched.reason)
    }

    private fun candidate(
        identityId: Long,
        currentVideoId: Long,
        path: String,
        size: Long = 1_000L,
        duration: Long = 10_000L,
        width: Int = 1920,
        height: Int = 1080,
        timestamp: Long = 1_700_000_000_000L
    ): MediaIdentityCandidate =
        MediaIdentityCandidate(
            identityId = identityId,
            currentVideoId = currentVideoId,
            fingerprint = fingerprint(
                path = path,
                size = size,
                duration = duration,
                width = width,
                height = height,
                timestamp = timestamp
            )
        )

    private fun fingerprint(
        path: String,
        size: Long = 1_000L,
        duration: Long = 10_000L,
        width: Int = 1920,
        height: Int = 1080,
        timestamp: Long = 1_700_000_000_000L
    ): MediaFingerprint =
        checkNotNull(
            MediaFingerprintPolicy.fromFields(
                title = path.substringAfterLast('/'),
                pathOrUri = path,
                sizeBytes = size,
                durationMs = duration,
                width = width,
                height = height,
                timestamp = timestamp
            )
        )
}
