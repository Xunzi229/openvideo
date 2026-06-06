package com.example.openvideo.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebDavSubtitleMatcherTest {

    @Test
    fun exactSubtitleNameRanksAheadOfLanguageSuffix() {
        val video = entry("Movie.mkv", "https://example.com/dav/Movie.mkv", playable = true)
        val exact = entry("Movie.srt", "https://example.com/dav/Movie.srt")
        val localized = entry("Movie.en.vtt", "https://example.com/dav/Movie.en.vtt")

        assertEquals(
            exact,
            WebDavSubtitleMatcher.matchForVideo(
                video = video,
                entries = listOf(localized, video, exact)
            )
        )
    }

    @Test
    fun remoteSubtitleMatcherOnlyAcceptsSrtAndVttSidecars() {
        val video = entry("episode01.mp4", "https://example.com/dav/episode01.mp4", playable = true)
        val ass = entry("episode01.ass", "https://example.com/dav/episode01.ass")
        val unrelated = entry("episode02.srt", "https://example.com/dav/episode02.srt")
        val directory = entry("episode01.srt", "https://example.com/dav/subs/", directory = true)

        assertNull(WebDavSubtitleMatcher.matchForVideo(video, listOf(ass, unrelated, directory)))
    }

    private fun entry(
        name: String,
        url: String,
        directory: Boolean = false,
        playable: Boolean = false
    ): WebDavDirectoryParser.Entry =
        WebDavDirectoryParser.Entry(
            name = name,
            url = url,
            isDirectory = directory,
            isPlayableVideo = playable,
            sizeBytes = null
        )
}
