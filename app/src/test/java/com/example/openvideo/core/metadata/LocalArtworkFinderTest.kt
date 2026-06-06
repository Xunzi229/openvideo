package com.example.openvideo.core.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalArtworkFinderTest {

    @Test
    fun prefersPosterFolderCoverThenSameBaseNameArtworkInVideoDirectory() {
        val artwork = LocalArtworkFinder.find(
            videoPath = "/storage/emulated/0/Movies/Show/Episode 01.mkv",
            candidatePaths = listOf(
                "/storage/emulated/0/Movies/Show/Episode 01.jpg",
                "/storage/emulated/0/Movies/Show/cover.png",
                "/storage/emulated/0/Movies/Show/folder.jpg",
                "/storage/emulated/0/Movies/Show/poster.jpg"
            )
        )

        assertEquals("/storage/emulated/0/Movies/Show/poster.jpg", artwork)
    }

    @Test
    fun fallsBackThroughNamedArtworkPriority() {
        val artwork = LocalArtworkFinder.find(
            videoPath = "/storage/emulated/0/Movies/Show/Episode 01.mkv",
            candidatePaths = listOf(
                "/storage/emulated/0/Movies/Show/Episode 01.webp",
                "/storage/emulated/0/Movies/Show/cover.png"
            )
        )

        assertEquals("/storage/emulated/0/Movies/Show/cover.png", artwork)
    }

    @Test
    fun usesSameBaseNameArtworkWhenNoFolderPosterExists() {
        val artwork = LocalArtworkFinder.find(
            videoPath = "/storage/emulated/0/Movies/Show/Episode 01.mkv",
            candidatePaths = listOf(
                "/storage/emulated/0/Movies/Show/Episode 02.jpg",
                "/storage/emulated/0/Movies/Show/Episode 01.png"
            )
        )

        assertEquals("/storage/emulated/0/Movies/Show/Episode 01.png", artwork)
    }

    @Test
    fun normalizesCaseAndSeparatorsForComparisonButReturnsOriginalPath() {
        val artwork = LocalArtworkFinder.find(
            videoPath = """C:\Videos\Show\Episode 01.mkv""",
            candidatePaths = listOf("""C:\Videos\Show\POSTER.PNG""")
        )

        assertEquals("""C:\Videos\Show\POSTER.PNG""", artwork)
    }

    @Test
    fun ignoresArtworkOutsideVideoDirectoryAndUnsupportedExtensions() {
        val artwork = LocalArtworkFinder.find(
            videoPath = "/storage/emulated/0/Movies/Show/Episode 01.mkv",
            candidatePaths = listOf(
                "/storage/emulated/0/Movies/poster.jpg",
                "/storage/emulated/0/Movies/Show/poster.txt"
            )
        )

        assertNull(artwork)
    }
}
