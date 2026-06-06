package com.example.openvideo.core.network

import com.example.openvideo.core.subtitle.SubtitleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Test

class WebDavMemoryCacheTest {

    @Test
    fun directoryEntriesExpireAfterShortTtl() {
        val cache = WebDavMemoryCache()
        val key = cache.cacheKey(
            namespace = "directory",
            url = "https://example.com/dav/",
            requestHeaders = mapOf("Authorization" to "Basic secret")
        )
        val entries = listOf(entry("movie.mp4"))

        cache.putDirectory(key, entries, nowMs = 1_000L)

        assertEquals(entries, cache.getDirectory(key, nowMs = 1_000L + WebDavMemoryCache.DIRECTORY_TTL_MS))
        assertNull(cache.getDirectory(key, nowMs = 1_001L + WebDavMemoryCache.DIRECTORY_TTL_MS))
    }

    @Test
    fun subtitleEntriesExpireAfterShortTtl() {
        val cache = WebDavMemoryCache()
        val key = cache.cacheKey(
            namespace = "subtitle",
            url = "https://example.com/dav/movie.srt",
            requestHeaders = mapOf("Authorization" to "Basic secret")
        )
        val subtitles = listOf(SubtitleItem(1, 0L, 1_000L, "hello"))

        cache.putSubtitle(key, subtitles, nowMs = 2_000L)

        assertEquals(subtitles, cache.getSubtitle(key, nowMs = 2_000L + WebDavMemoryCache.SUBTITLE_TTL_MS))
        assertNull(cache.getSubtitle(key, nowMs = 2_001L + WebDavMemoryCache.SUBTITLE_TTL_MS))
    }

    @Test
    fun cacheKeysHashSensitiveHeadersInsteadOfStoringPlainValues() {
        val cache = WebDavMemoryCache()
        val key = cache.cacheKey(
            namespace = "subtitle",
            url = "https://example.com/dav/movie.srt",
            requestHeaders = mapOf("Authorization" to "Basic secret", "X-Trace" to "visible")
        )

        assertFalse(key.contains("Basic secret"))
        assertFalse(key.contains("Authorization"))
        assertFalse(key.contains("visible"))
        assertEquals(key, cache.cacheKey("subtitle", "https://example.com/dav/movie.srt", mapOf("X-Trace" to "visible", "Authorization" to "Basic secret")))
    }

    @Test
    fun clearDropsDirectoryAndSubtitleEntries() {
        val cache = WebDavMemoryCache()
        val directoryKey = cache.cacheKey("directory", "https://example.com/dav/", emptyMap())
        val subtitleKey = cache.cacheKey("subtitle", "https://example.com/dav/movie.srt", emptyMap())

        cache.putDirectory(directoryKey, listOf(entry("movie.mp4")), nowMs = 1L)
        cache.putSubtitle(subtitleKey, listOf(SubtitleItem(1, 0L, 1_000L, "hello")), nowMs = 1L)
        cache.clear()

        assertNull(cache.getDirectory(directoryKey, nowMs = 1L))
        assertNull(cache.getSubtitle(subtitleKey, nowMs = 1L))
    }

    private fun entry(name: String): WebDavDirectoryParser.Entry =
        WebDavDirectoryParser.Entry(
            name = name,
            url = "https://example.com/dav/$name",
            isDirectory = false,
            isPlayableVideo = true,
            sizeBytes = null
        )
}
