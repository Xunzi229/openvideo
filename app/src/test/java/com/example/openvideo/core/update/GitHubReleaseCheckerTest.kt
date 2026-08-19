package com.example.openvideo.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseCheckerTest {

    @Test
    fun isRemoteNewer_comparesDotSeparatedParts() {
        assertTrue(GitHubReleaseChecker.isRemoteNewer("v1.0.1", "1.0.0"))
        assertFalse(GitHubReleaseChecker.isRemoteNewer("v1.0.0", "1.0.0"))
        assertFalse(GitHubReleaseChecker.isRemoteNewer("v0.9.9", "1.0.0"))
    }

    @Test
    fun parseLatestDropsUntrustedAssetsAndRejectsUntrustedReleasePages() {
        val parsed = GitHubReleaseChecker.parseLatest(
            """{
              "tag_name":"v0.0.16",
              "html_url":"https://github.com/Xunzi229/openvideo/releases/tag/v0.0.16",
              "assets":[
                {"name":"openvideo.apk","browser_download_url":"https://github.com/Xunzi229/openvideo/releases/download/v0.0.16/openvideo.apk"},
                {"name":"spoof.apk","browser_download_url":"https://evil.example/spoof.apk"}
              ]
            }"""
        )
        assertEquals(listOf("openvideo.apk"), parsed?.assets?.map { it.name })

        assertNull(
            GitHubReleaseChecker.parseLatest(
                """{"tag_name":"v9","html_url":"https://evil.example/release","assets":[]}"""
            )
        )
    }

    @Test
    fun shaNormalizationRejectsDecoratedOrMalformedValues() {
        val hash = "a".repeat(64)
        assertEquals(hash, GitHubReleaseChecker.normalizeSha256Hex(hash.uppercase()))
        assertNull(GitHubReleaseChecker.normalizeSha256Hex("sha256:$hash"))
        assertNull(GitHubReleaseChecker.normalizeSha256Hex("${hash}0"))
    }
}
