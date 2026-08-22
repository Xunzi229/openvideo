package com.example.openvideo.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubAbiSelectionTest {

    @Test
    fun selectApk_prefersMatchingAbi() {
        val assets = listOf(
            GitHubReleaseChecker.ReleaseAsset("openvideo-x86.apk", "https://x86"),
            GitHubReleaseChecker.ReleaseAsset("openvideo-arm64-v8a.apk", "https://arm64"),
            GitHubReleaseChecker.ReleaseAsset("openvideo-armeabi-v7a.apk", "https://arm32")
        )
        val picked = GitHubReleaseChecker.selectApkForAbi(
            assets,
            arrayOf("arm64-v8a", "armeabi-v7a")
        )
        assertEquals("openvideo-arm64-v8a.apk", picked?.name)
    }

    @Test
    fun selectApk_fallsBackToUniversalWhenNoAbiMatch() {
        val assets = listOf(
            GitHubReleaseChecker.ReleaseAsset("OpenVideo-universal.apk", "https://u")
        )
        val picked = GitHubReleaseChecker.selectApkForAbi(assets, arrayOf("arm64-v8a"))
        assertEquals("OpenVideo-universal.apk", picked?.name)
    }

    @Test
    fun selectApk_doesNotTreatX86_64AsX86() {
        val assets = listOf(
            GitHubReleaseChecker.ReleaseAsset("openvideo-x86_64.apk", "https://64"),
            GitHubReleaseChecker.ReleaseAsset("openvideo-x86.apk", "https://32")
        )
        val picked = GitHubReleaseChecker.selectApkForAbi(assets, arrayOf("x86"))
        assertEquals("openvideo-x86.apk", picked?.name)
    }

    @Test
    fun selectApk_matchesPreviewArtifactAbiMarker() {
        val assets = listOf(
            GitHubReleaseChecker.ReleaseAsset("openvideo-preview-abc-x86_64.apk", "https://64"),
            GitHubReleaseChecker.ReleaseAsset("openvideo-preview-abc-arm64-v8a.apk", "https://arm")
        )
        val picked = GitHubReleaseChecker.selectApkForAbi(assets, arrayOf("arm64-v8a", "armeabi-v7a"))
        assertEquals("openvideo-preview-abc-arm64-v8a.apk", picked?.name)
    }
}
