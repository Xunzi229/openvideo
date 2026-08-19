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
    fun releasePolicyDropsUntrustedAssetsAndRejectsUntrustedReleasePages() {
        val parsed = GitHubReleaseChecker.trustedReleaseOrNull(
            tagName = "v0.0.16",
            releaseHtmlUrl = "https://github.com/Xunzi229/openvideo/releases/tag/v0.0.16",
            assets = listOf(
                GitHubReleaseChecker.ReleaseAsset(
                    "openvideo.apk",
                    "https://github.com/Xunzi229/openvideo/releases/download/v0.0.16/openvideo.apk"
                ),
                GitHubReleaseChecker.ReleaseAsset("spoof.apk", "https://evil.example/spoof.apk")
            )
        )
        assertEquals(listOf("openvideo.apk"), parsed?.assets?.map { it.name })

        assertNull(
            GitHubReleaseChecker.trustedReleaseOrNull(
                tagName = "v9",
                releaseHtmlUrl = "https://evil.example/release",
                assets = emptyList()
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
