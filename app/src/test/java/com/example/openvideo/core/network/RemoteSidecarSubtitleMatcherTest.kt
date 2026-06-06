package com.example.openvideo.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteSidecarSubtitleMatcherTest {

    @Test
    fun exactRemoteSubtitleRanksAheadOfLanguageSuffix() {
        val video = item("Movie.mkv", "smb://nas/Movies/Movie.mkv", playable = true)
        val exact = item("Movie.srt", "smb://nas/Movies/Movie.srt")
        val localized = item("Movie.zh-CN.vtt", "smb://nas/Movies/Movie.zh-CN.vtt")

        assertEquals(
            exact,
            RemoteSidecarSubtitleMatcher.matchForVideo(
                video = video,
                candidates = listOf(localized, exact)
            )
        )
    }

    @Test
    fun matcherRejectsDirectoriesUnsupportedFormatsAndDifferentBaseNames() {
        val video = item("episode01.mp4", "smb://nas/Show/episode01.mp4", playable = true)
        val directory = item("episode01.srt", "smb://nas/Show/subs/", directory = true)
        val ass = item("episode01.ass", "smb://nas/Show/episode01.ass")
        val unrelated = item("episode02.srt", "smb://nas/Show/episode02.srt")

        assertNull(RemoteSidecarSubtitleMatcher.matchForVideo(video, listOf(directory, ass, unrelated)))
    }

    @Test
    fun matcherRejectsNonPlayableVideoEntries() {
        val video = item("Movie.mkv", "smb://nas/Movies/Movie.mkv", playable = false)
        val subtitle = item("Movie.srt", "smb://nas/Movies/Movie.srt")

        assertNull(RemoteSidecarSubtitleMatcher.matchForVideo(video, listOf(subtitle)))
    }

    private fun item(
        name: String,
        url: String,
        directory: Boolean = false,
        playable: Boolean = false
    ): RemoteSidecarSubtitleMatcher.Item =
        RemoteSidecarSubtitleMatcher.Item(
            name = name,
            url = url,
            isDirectory = directory,
            isPlayableVideo = playable
        )
}
