package com.example.openvideo.core.update

import org.junit.Assert.*
import org.junit.Test

class UpdateUrlBoundaryTest {
    @Test fun malformedAuthorityPortsAndFragmentsCannotBecomeTrustedReleaseLinks() {
        val path = "/Xunzi229/openvideo/releases/tag/v1"
        for (url in listOf("https://[invalid", "https:$path", "https://user@github.com$path", "https://github.com:444$path", "https://github.com$path#fragment")) {
            assertFalse(url, UpdateUrlPolicy.isTrustedReleasePage(url))
        }
        assertTrue(UpdateUrlPolicy.isTrustedReleasePage("https://github.com:443$path"))
    }
}
