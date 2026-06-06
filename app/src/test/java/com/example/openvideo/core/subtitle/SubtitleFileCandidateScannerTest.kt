package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class SubtitleFileCandidateScannerTest {

    @Test
    fun candidatesNearVideoIncludesSameDirectoryAndKnownSubtitleDirectoriesOnlyOneLevelDeep() {
        val root = Files.createTempDirectory("openvideo-subtitles").toFile()
        val video = root.resolve("Show.S01E02.mkv").apply { writeText("video") }
        val sameDirectory = root.resolve("Show.S01E02.en.srt").apply { writeText("same") }
        val subs = root.resolve("Subs").apply { mkdir() }
        val subtitles = root.resolve("Subtitles").apply { mkdir() }
        val chinese = root.resolve("\u5b57\u5e55").apply { mkdir() }
        val ignored = root.resolve("Extras").apply { mkdir() }
        val nested = subs.resolve("Nested").apply { mkdir() }
        val subsFile = subs.resolve("Show.S01E02.zh.srt").apply { writeText("subs") }
        val subtitlesFile = subtitles.resolve("Show.S01E02.ass").apply { writeText("subtitles") }
        val chineseFile = chinese.resolve("Show.S01E02.vtt").apply { writeText("chinese") }
        ignored.resolve("Show.S01E02.srt").writeText("ignored")
        nested.resolve("Show.S01E02.srt").writeText("nested")

        assertEquals(
            listOf(
                SubtitleFileCandidateScanner.Item(sameDirectory.absolutePath, inSubtitleDirectory = false),
                SubtitleFileCandidateScanner.Item(subsFile.absolutePath, inSubtitleDirectory = true),
                SubtitleFileCandidateScanner.Item(subtitlesFile.absolutePath, inSubtitleDirectory = true),
                SubtitleFileCandidateScanner.Item(chineseFile.absolutePath, inSubtitleDirectory = true)
            ),
            SubtitleFileCandidateScanner.candidatesNear(video.absolutePath)
        )
    }

    @Test
    fun candidatesNearVideoReturnsEmptyForMissingOrContentPaths() {
        assertEquals(emptyList<SubtitleFileCandidateScanner.Item>(), SubtitleFileCandidateScanner.candidatesNear(""))
        assertEquals(
            emptyList<SubtitleFileCandidateScanner.Item>(),
            SubtitleFileCandidateScanner.candidatesNear("content://media/external/video/1")
        )
    }
}
