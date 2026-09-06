package com.openvideo.app.core.mediaid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MediaIdentityBranchTest {
    private val current = MediaFingerprint("/new/movie.mp4", "movie", 1_000, 10_000, 1920, 1080, 500_000)

    @Test
    fun rejectsMissingSizeDurationOrPath() {
        for ((size, duration) in listOf(0L to 1L, -1L to 1L, 1L to 0L, 1L to -1L)) {
            assertNull(MediaFingerprintPolicy.fromFields("Movie", "/movie.mp4", size, duration, 1, 1, 0))
        }
        assertNull(MediaFingerprintPolicy.fromFields("Movie", " ", 1, 1, 1, 1, 0))
        val fingerprint = MediaFingerprintPolicy.fromFields("  My.Movie_-  Name ", "/MOVIE.mp4", 1, 1, 0, 0, 0)!!
        assertEquals("my movie name", fingerprint.normalizedTitleKey)
        assertEquals("/movie.mp4", fingerprint.normalizedPathKey)
    }

    @Test
    fun strongMatchRejectsEachIndependentMismatch() {
        listOf(
            current.copy(normalizedPathKey = "/other.mp4"),
            current.copy(sizeBytes = 2_000),
            current.copy(durationMs = 20_000)
        ).forEach { assertFalse(it.toString(), MediaFingerprintPolicy.strongMatch(current, it)) }
    }

    @Test
    fun renameAndSameIdMatchingRejectEveryShapeMismatch() {
        val mismatches = listOf(
            current.copy(sizeBytes = 2_000), current.copy(durationMs = 20_000),
            current.copy(width = 1280), current.copy(height = 720)
        )
        mismatches.forEach { other ->
            val moved = other.copy(normalizedPathKey = "/old/movie.mp4")
            assertFalse(MediaFingerprintPolicy.likelyRename(current, moved))
            assertEquals(
                MediaIdentityMatchDecision.NoMatch,
                MediaIdentityMatcher.match(1, current, listOf(MediaIdentityCandidate(10, 1, moved)))
            )
        }
    }

    @Test
    fun noCandidatesReturnNoMatch() {
        assertEquals(MediaIdentityMatchDecision.NoMatch, MediaIdentityMatcher.match(1, current, emptyList()))
    }

    @Test
    fun sameVideoIdWinsOverPathAndRenameCandidates() {
        val candidates = listOf(
            MediaIdentityCandidate(11, 2, current),
            MediaIdentityCandidate(12, 3, current.copy(normalizedPathKey = "/old/renamed.mp4")),
            MediaIdentityCandidate(10, 1, current.copy(normalizedPathKey = "/old/movie.mp4"))
        )
        assertEquals(
            MediaIdentityMatchDecision.Matched(10, MediaIdentityMatchReason.SAME_VIDEO_ID),
            MediaIdentityMatcher.match(1, current, candidates)
        )
    }

    @Test
    fun returnsConflictAtEachMatchingTierInsteadOfChoosingFirstCandidate() {
        val tiers = listOf(
            Triple(MediaIdentityMatchReason.SAME_VIDEO_ID, 1L, current),
            Triple(MediaIdentityMatchReason.SAME_PATH, 2L, current),
            Triple(MediaIdentityMatchReason.PATH_MIGRATION, 2L, current.copy(normalizedPathKey = "/old/movie.mp4")),
            Triple(MediaIdentityMatchReason.RENAME_CANDIDATE, 2L, current.copy(normalizedPathKey = "/old/renamed.mp4"))
        )
        tiers.forEach { (reason, videoId, fingerprint) ->
            assertEquals(
                MediaIdentityMatchDecision.Conflict(reason, listOf(10, 11)),
                MediaIdentityMatcher.match(1, current, listOf(
                    MediaIdentityCandidate(10, videoId, fingerprint),
                    MediaIdentityCandidate(11, videoId, fingerprint)
                ))
            )
        }
    }

    @Test
    fun migrationToleranceIsInclusiveInBothTimeDirections() {
        for (delta in listOf(-101L, -100L, 0L, 100L, 101L)) {
            val reason = if (delta in -100..100) MediaIdentityMatchReason.PATH_MIGRATION else MediaIdentityMatchReason.RENAME_CANDIDATE
            val previous = current.copy(normalizedPathKey = "/old/movie.mp4", timestamp = current.timestamp + delta)
            assertEquals(
                "delta=$delta", MediaIdentityMatchDecision.Matched(10, reason),
                MediaIdentityMatcher.match(1, current, listOf(MediaIdentityCandidate(10, 2, previous)), timestampToleranceMs = 100)
            )
        }
    }

    @Test
    fun reusedPathWithDifferentSizeDoesNotInheritAnUnrelatedIdentity() {
        assertEquals(
            MediaIdentityMatchDecision.NoMatch,
            MediaIdentityMatcher.match(1, current, listOf(MediaIdentityCandidate(10, 2, current.copy(sizeBytes = 5_000))))
        )
    }

    @Test
    fun normalizesRootRelativePathsAndUriSeparators() {
        assertEquals("/", MediaPathNormalizer.normalize("////")?.displayPath)
        assertEquals("", MediaPathNormalizer.normalize("movie.mp4")?.parentKey)
        assertEquals("content://media/video/1", MediaPathNormalizer.normalize("content://media///video/1//")?.displayPath)
        assertEquals("c:/Movies", MediaPathNormalizer.normalize("c://Movies")?.displayPath)
        assertNull(MediaPathNormalizer.normalize(""))
    }
}
