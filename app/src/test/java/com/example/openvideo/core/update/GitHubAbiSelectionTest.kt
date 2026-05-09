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
}
