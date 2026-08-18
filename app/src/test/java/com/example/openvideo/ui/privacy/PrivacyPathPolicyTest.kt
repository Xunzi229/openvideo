package com.example.openvideo.ui.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPathPolicyTest {

    @Test
    fun canonicalizesSeparatorsAndMatchesOnlyDirectoryBoundaries() {
        assertEquals(
            "/storage/emulated/0/Movies/Private",
            PrivacyPathPolicy.canonical(" /storage//emulated/0/Movies\\Private/ ")
        )
        assertTrue(
            PrivacyPathPolicy.isWithin(
                "/storage/emulated/0/Movies/Private/a.mp4",
                "/storage/emulated/0/Movies/Private"
            )
        )
        assertFalse(
            PrivacyPathPolicy.isWithin(
                "/storage/emulated/0/Movies/Private2/a.mp4",
                "/storage/emulated/0/Movies/Private"
            )
        )
    }

    @Test
    fun contentUrisCannotBeConfiguredAsDirectoryPrefixes() {
        assertFalse(
            PrivacyPathPolicy.isWithin(
                "content://media/external/video/12",
                "content://media/external/video"
            )
        )
    }
}
