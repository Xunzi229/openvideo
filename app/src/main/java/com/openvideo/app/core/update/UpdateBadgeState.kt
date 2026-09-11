package com.openvideo.app.core.update

import com.openvideo.app.core.prefs.AppPrefs
import java.net.URI

/** Reconciles persisted update hints before the UI observes them, even when offline. */
object UpdateBadgeState {
    fun restore(prefs: AppPrefs, installedVersionName: String): Boolean {
        val url = prefs.githubPendingDownloadUrl
        // Existing installations already cache the release URL, so no migration or network is needed.
        val tag = if (UpdateUrlPolicy.isTrustedReleasePage(url)) {
            URI(url).path.substringAfter("/releases/tag/")
        } else {
            ""
        }
        val visible = tag.isNotBlank() &&
            GitHubReleaseChecker.isRemoteNewer(tag, installedVersionName)
        if (!visible && (prefs.githubUpdateBadgeVisible || url.isNotBlank())) {
            prefs.githubPendingDownloadUrl = ""
            // An installed update or incomplete legacy cache must not suppress a fresh check.
            prefs.lastGitHubReleaseCheckMs = 0L
        }
        prefs.githubUpdateBadgeVisible = visible
        return visible
    }
}
