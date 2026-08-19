package com.example.openvideo.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUrlPolicyTest {

    @Test
    fun trustsOnlyProjectReleasePagesAndAssets() {
        assertTrue(UpdateUrlPolicy.isTrustedReleasePage("https://github.com/Xunzi229/openvideo/releases/tag/v0.0.16"))
        assertTrue(
            UpdateUrlPolicy.isTrustedReleaseAsset(
                "https://github.com/Xunzi229/openvideo/releases/download/v0.0.16/openvideo.apk"
            )
        )
        assertFalse(UpdateUrlPolicy.isTrustedReleasePage("https://evil.example/releases/tag/v0.0.16"))
        assertFalse(UpdateUrlPolicy.isTrustedReleaseAsset("http://github.com/Xunzi229/openvideo/releases/download/v/a.apk"))
        assertFalse(UpdateUrlPolicy.isTrustedReleaseAsset("https://github.com/other/repo/releases/download/v/a.apk"))
        assertFalse(UpdateUrlPolicy.isTrustedReleaseAsset("https://user@github.com/Xunzi229/openvideo/releases/download/v/a.apk"))
    }

    @Test
    fun permitsOnlyHttpsGitHubAssetRedirectHosts() {
        assertTrue(UpdateUrlPolicy.isTrustedRedirect("https://release-assets.githubusercontent.com/file?token=opaque"))
        assertTrue(UpdateUrlPolicy.isTrustedRedirect("https://objects.githubusercontent.com/file"))
        assertFalse(UpdateUrlPolicy.isTrustedRedirect("http://release-assets.githubusercontent.com/file"))
        assertFalse(UpdateUrlPolicy.isTrustedRedirect("https://release-assets.githubusercontent.com.evil.example/file"))
    }
}
