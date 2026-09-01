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
    fun releasePolicyAcceptsOnlyTrustedTagPages() {
        val releasePage = "https://github.com/Xunzi229/openvideo/releases/tag/v0.0.16"
        val parsed = GitHubReleaseChecker.trustedReleaseOrNull(
            tagName = "v0.0.16",
            releaseHtmlUrl = releasePage
        )
        assertEquals(releasePage, parsed?.releaseHtmlUrl)

        assertNull(
            GitHubReleaseChecker.trustedReleaseOrNull(
                tagName = "v9",
                releaseHtmlUrl = "https://evil.example/release"
            )
        )
        assertNull(
            GitHubReleaseChecker.trustedReleaseOrNull(
                tagName = "v9",
                releaseHtmlUrl = "https://github.com/Xunzi229/openvideo/releases/download/v9/openvideo.apk"
            )
        )
    }
}
