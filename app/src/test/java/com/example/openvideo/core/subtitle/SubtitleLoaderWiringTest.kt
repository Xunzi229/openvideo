package com.example.openvideo.core.subtitle

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class SubtitleLoaderWiringTest {

    @Test
    fun playerSubtitleEncodingPreferenceIsUsedWhenLoadingFiles() {
        val source = String(
            Files.readAllBytes(
                sequenceOf(
                    Paths.get(
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    ),
                    Paths.get(
                        "app",
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    )
                ).first(Files::exists)
            )
        )

        assertTrue(source.contains("private val playerPrefs: PlayerPrefs"))
        assertTrue(source.contains("playerPrefs.subtitleEncoding"))
        assertTrue(source.contains("charsetForPreference("))
    }

    @Test
    fun sidecarDiscoveryUsesCoreSubtitleSidecarMatcher() {
        val source = String(
            Files.readAllBytes(
                sequenceOf(
                    Paths.get(
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    ),
                    Paths.get(
                        "app",
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    )
                ).first(Files::exists)
            )
        )

        assertTrue(source.contains("SubtitleFileCandidateScanner.candidatesNear(videoPath)"))
        assertTrue(source.contains("SubtitleSidecarMatcher.matchCandidates("))
    }

    @Test
    fun networkSubtitleLoadingUsesOkHttpAndRequestHeaders() {
        val source = String(
            Files.readAllBytes(
                sequenceOf(
                    Paths.get(
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    ),
                    Paths.get(
                        "app",
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    )
                ).first(Files::exists)
            )
        )

        assertTrue(source.contains("private val okHttpClient: OkHttpClient"))
        assertTrue(source.contains("fun loadFromNetworkUrl(url: String, requestHeaders: Map<String, String> = emptyMap())"))
        assertTrue(source.contains("requestHeaders.forEach { (name, value) ->"))
        assertTrue(source.contains("builder.header(name, value)"))
        assertTrue(source.contains("okHttpClient.newCall(request).execute()"))
        assertTrue(source.contains("parseSubtitleContent("))
    }

    @Test
    fun networkSubtitleLoadingUsesWebDavMemoryCache() {
        val source = String(
            Files.readAllBytes(
                sequenceOf(
                    Paths.get(
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    ),
                    Paths.get(
                        "app",
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    )
                ).first(Files::exists)
            )
        )

        assertTrue(source.contains("private val webDavMemoryCache: WebDavMemoryCache"))
        assertTrue(source.contains("namespace = \"subtitle\""))
        assertTrue(source.contains("webDavMemoryCache.getSubtitle(cacheKey)?.let"))
        assertTrue(source.contains("webDavMemoryCache.putSubtitle(cacheKey, subtitles)"))
    }
}
