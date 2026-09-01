package com.example.openvideo.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUrlPolicyTest {

    @Test
    fun trustsOnlyProjectReleaseTagPages() {
        assertTrue(UpdateUrlPolicy.isTrustedReleasePage("https://github.com/Xunzi229/openvideo/releases/tag/v0.0.16"))
        assertFalse(UpdateUrlPolicy.isTrustedReleasePage("https://evil.example/releases/tag/v0.0.16"))
        assertFalse(UpdateUrlPolicy.isTrustedReleasePage("http://github.com/Xunzi229/openvideo/releases/tag/v0.0.16"))
        assertFalse(UpdateUrlPolicy.isTrustedReleasePage("https://github.com/other/repo/releases/tag/v0.0.16"))
        assertFalse(UpdateUrlPolicy.isTrustedReleasePage("https://user@github.com/Xunzi229/openvideo/releases/tag/v0.0.16"))
        assertFalse(UpdateUrlPolicy.isTrustedReleasePage("https://github.com/Xunzi229/openvideo/releases/tag/"))
        assertFalse(UpdateUrlPolicy.isTrustedReleasePage("https://github.com/Xunzi229/openvideo/releases/tag/v0.0.16/extra"))
        assertFalse(
            UpdateUrlPolicy.isTrustedReleasePage(
                "https://github.com/Xunzi229/openvideo/releases/download/v0.0.16/openvideo.apk"
            )
        )
    }
}
