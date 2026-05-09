package com.example.openvideo.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseCheckerTest {

    @Test
    fun isRemoteNewer_comparesDotSeparatedParts() {
        assertTrue(GitHubReleaseChecker.isRemoteNewer("v1.0.1", "1.0.0"))
        assertFalse(GitHubReleaseChecker.isRemoteNewer("v1.0.0", "1.0.0"))
        assertFalse(GitHubReleaseChecker.isRemoteNewer("v0.9.9", "1.0.0"))
    }
}
