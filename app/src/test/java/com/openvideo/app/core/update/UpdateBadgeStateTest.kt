package com.openvideo.app.core.update

import android.app.Application
import com.openvideo.app.core.prefs.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class UpdateBadgeStateTest {
    private fun cachedUpdate(url: String = RELEASE_URL): AppPrefs =
        AppPrefs(RuntimeEnvironment.getApplication()).apply {
            githubUpdateBadgeVisible = true
            githubPendingDownloadUrl = url
            lastGitHubReleaseCheckMs = System.currentTimeMillis()
        }

    @Test
    fun installingPendingVersionClearsPersistedBadgeWithoutNetworkOrWaiting24Hours() {
        val prefs = cachedUpdate()
        assertFalse(GitHubReleaseChecker.shouldRunPeriodicCheck(prefs.lastGitHubReleaseCheckMs))

        assertFalse(UpdateBadgeState.restore(prefs, "0.0.24"))

        val reopened = AppPrefs(RuntimeEnvironment.getApplication())
        assertFalse(reopened.githubUpdateBadgeVisible)
        assertEquals("", reopened.githubPendingDownloadUrl)
        assertTrue(GitHubReleaseChecker.shouldRunPeriodicCheck(reopened.lastGitHubReleaseCheckMs))
        assertFalse(UpdateBadgeState.restore(reopened, "0.0.24"))
    }

    @Test
    fun installingVersionBeyondPendingReleaseAlsoClearsBadge() {
        val prefs = cachedUpdate()
        assertFalse(UpdateBadgeState.restore(prefs, "0.0.25"))
        assertEquals("", prefs.githubPendingDownloadUrl)
    }

    @Test
    fun remainingOnOlderVersionPreservesBadgeAndCheckInterval() {
        val prefs = cachedUpdate()
        val checkedAt = prefs.lastGitHubReleaseCheckMs
        assertTrue(UpdateBadgeState.restore(prefs, "0.0.23"))
        assertTrue(prefs.githubUpdateBadgeVisible)
        assertEquals(RELEASE_URL, prefs.githubPendingDownloadUrl)
        assertEquals(checkedAt, prefs.lastGitHubReleaseCheckMs)
    }

    @Test
    fun incompleteLegacyCacheClearsStaleBadgeAndAllowsRetry() {
        val prefs = cachedUpdate("")
        assertFalse(UpdateBadgeState.restore(prefs, "0.0.24"))
        assertEquals(0L, prefs.lastGitHubReleaseCheckMs)
    }

    @Test
    fun untrustedCachedUrlCannotKeepBadgeVisible() {
        val prefs = cachedUpdate("https://example.com/releases/tag/v99.0.0")
        assertFalse(UpdateBadgeState.restore(prefs, "0.0.24"))
        assertEquals("", prefs.githubPendingDownloadUrl)
    }

    @Test
    fun encodedReleaseTagIsComparedAsDecodedVersion() {
        val prefs = cachedUpdate(RELEASE_URL.replace("v0", "%760"))
        assertFalse(UpdateBadgeState.restore(prefs, "0.0.24"))
    }

    @Test
    fun noPendingUpdatePreservesSuccessfulCheckInterval() {
        val prefs = cachedUpdate("").apply { githubUpdateBadgeVisible = false }
        val checkedAt = prefs.lastGitHubReleaseCheckMs
        assertFalse(UpdateBadgeState.restore(prefs, "0.0.24"))
        assertEquals(checkedAt, prefs.lastGitHubReleaseCheckMs)
    }

    companion object {
        private const val RELEASE_URL = "https://github.com/Xunzi229/openvideo/releases/tag/v0.0.24"
    }
}
