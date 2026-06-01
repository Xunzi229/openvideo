package com.example.openvideo.core.mediaid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaPathNormalizerTest {

    @Test
    fun normalizesAndroidFilePathForComparison() {
        val normalized = MediaPathNormalizer.normalize("/storage/emulated/0/Movies/Show/Episode 01.mkv")

        checkNotNull(normalized)
        assertEquals("/storage/emulated/0/Movies/Show/Episode 01.mkv", normalized.displayPath)
        assertEquals("/storage/emulated/0/movies/show/episode 01.mkv", normalized.comparisonKey)
        assertEquals("Episode 01.mkv", normalized.fileName)
        assertEquals("/storage/emulated/0/movies/show", normalized.parentKey)
    }

    @Test
    fun normalizesBackslashesRepeatedSeparatorsAndTrailingSeparator() {
        val normalized = MediaPathNormalizer.normalize("""C:\\Videos\\Show///Episode 02.mkv/""")

        checkNotNull(normalized)
        assertEquals("C:/Videos/Show/Episode 02.mkv", normalized.displayPath)
        assertEquals("c:/videos/show/episode 02.mkv", normalized.comparisonKey)
        assertEquals("Episode 02.mkv", normalized.fileName)
        assertEquals("c:/videos/show", normalized.parentKey)
    }

    @Test
    fun normalizesContentUriForComparison() {
        val normalized = MediaPathNormalizer.normalize("content://media/external/video/media/123")

        checkNotNull(normalized)
        assertEquals("content://media/external/video/media/123", normalized.displayPath)
        assertEquals("content://media/external/video/media/123", normalized.comparisonKey)
        assertEquals("123", normalized.fileName)
        assertEquals("content://media/external/video/media", normalized.parentKey)
    }

    @Test
    fun rejectsBlankPath() {
        assertNull(MediaPathNormalizer.normalize("   "))
    }
}
