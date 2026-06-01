package com.example.openvideo.core.mediaid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFingerprintPolicyTest {

    @Test
    fun createsFingerprintFromValidFields() {
        val fingerprint = MediaFingerprintPolicy.fromFields(
            title = "Show.Name.S01E01.mkv",
            pathOrUri = "/storage/emulated/0/Shows/Show.Name.S01E01.mkv",
            sizeBytes = 1_024L,
            durationMs = 60_000L,
            width = 1920,
            height = 1080,
            timestamp = 1_700_000_000L
        )

        checkNotNull(fingerprint)
        assertEquals("/storage/emulated/0/shows/show.name.s01e01.mkv", fingerprint.normalizedPathKey)
        assertEquals("show name s01e01 mkv", fingerprint.normalizedTitleKey)
        assertEquals(1_024L, fingerprint.sizeBytes)
        assertEquals(60_000L, fingerprint.durationMs)
        assertEquals(1920, fingerprint.width)
        assertEquals(1080, fingerprint.height)
        assertEquals(1_700_000_000L, fingerprint.timestamp)
    }

    @Test
    fun rejectsInvalidSizeOrDuration() {
        assertNull(
            MediaFingerprintPolicy.fromFields(
                title = "A",
                pathOrUri = "/storage/a.mp4",
                sizeBytes = 0L,
                durationMs = 1_000L,
                width = 1,
                height = 1,
                timestamp = 1L
            )
        )
        assertNull(
            MediaFingerprintPolicy.fromFields(
                title = "A",
                pathOrUri = "/storage/a.mp4",
                sizeBytes = 1L,
                durationMs = 0L,
                width = 1,
                height = 1,
                timestamp = 1L
            )
        )
    }

    @Test
    fun strongMatchRequiresSamePathSizeAndDuration() {
        val left = fingerprint(path = "/storage/Show/E01.mkv", size = 1_000L, duration = 10_000L)
        val right = fingerprint(path = "/storage/show/e01.mkv", size = 1_000L, duration = 10_000L)
        val differentSize = fingerprint(path = "/storage/show/e01.mkv", size = 2_000L, duration = 10_000L)

        assertTrue(MediaFingerprintPolicy.strongMatch(left, right))
        assertFalse(MediaFingerprintPolicy.strongMatch(left, differentSize))
    }

    @Test
    fun likelyRenameRequiresDifferentPathWithSameSizeDurationAndDimensions() {
        val left = fingerprint(path = "/storage/Old/E01.mkv", size = 1_000L, duration = 10_000L)
        val renamed = fingerprint(path = "/storage/New/Episode 01.mkv", size = 1_000L, duration = 10_000L)
        val samePath = fingerprint(path = "/storage/old/e01.mkv", size = 1_000L, duration = 10_000L)
        val differentDuration = fingerprint(path = "/storage/New/Episode 01.mkv", size = 1_000L, duration = 11_000L)

        assertTrue(MediaFingerprintPolicy.likelyRename(left, renamed))
        assertFalse(MediaFingerprintPolicy.likelyRename(left, samePath))
        assertFalse(MediaFingerprintPolicy.likelyRename(left, differentDuration))
    }

    private fun fingerprint(path: String, size: Long, duration: Long): MediaFingerprint =
        checkNotNull(
            MediaFingerprintPolicy.fromFields(
                title = path.substringAfterLast('/'),
                pathOrUri = path,
                sizeBytes = size,
                durationMs = duration,
                width = 1920,
                height = 1080,
                timestamp = 1L
            )
        )
}
