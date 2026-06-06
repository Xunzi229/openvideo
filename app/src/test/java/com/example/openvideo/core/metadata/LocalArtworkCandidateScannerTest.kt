package com.example.openvideo.core.metadata

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class LocalArtworkCandidateScannerTest {

    @Test
    fun candidatesNearVideoReturnsOnlySiblingArtworkFiles() {
        val dir = Files.createTempDirectory("openvideo-artwork")
        val nested = Files.createDirectory(dir.resolve("nested"))
        val video = Files.createFile(dir.resolve("Show.S01E01.mkv"))
        val poster = Files.createFile(dir.resolve("poster.jpg"))
        val cover = Files.createFile(dir.resolve("cover.png"))
        Files.createFile(dir.resolve("notes.txt"))
        Files.createFile(nested.resolve("folder.jpg"))

        val candidates = LocalArtworkCandidateScanner.candidatesNear(video.toString())

        assertEquals(listOf(cover.toString(), poster.toString()), candidates.sorted())
    }

    @Test
    fun candidatesNearVideoReturnsEmptyForMissingOrContentPaths() {
        assertEquals(emptyList<String>(), LocalArtworkCandidateScanner.candidatesNear("content://media/external/video/1"))
        assertEquals(emptyList<String>(), LocalArtworkCandidateScanner.candidatesNear("/missing/show/S01E01.mkv"))
    }
}
