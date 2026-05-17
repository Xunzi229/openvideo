package com.example.openvideo.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashRedactionPolicyTest {

    @Test
    fun externalStoragePathsAreReplacedKeepingExtension() {
        val redacted = CrashRedactionPolicy.redact("Failed: /storage/emulated/0/Movies/secret.mp4 missing")
        assertFalse(redacted.contains("secret"))
        assertFalse(redacted.contains("/storage/emulated/"))
        assertTrue(redacted.contains("<file>.mp4"))
    }

    @Test
    fun sdcardPathsAreReplacedKeepingExtension() {
        val redacted = CrashRedactionPolicy.redact("open /sdcard/Download/clip.mkv now")
        assertTrue(redacted.contains("<file>.mkv"))
        assertFalse(redacted.contains("Download"))
    }

    @Test
    fun fileUriIsRedacted() {
        val redacted = CrashRedactionPolicy.redact("uri=file:///storage/emulated/0/Movies/x.mp4 end")
        assertTrue(redacted.contains("<file>.mp4"))
        assertFalse(redacted.contains("file://"))
    }

    @Test
    fun mediaContentUriKeepsOnlyId() {
        val redacted = CrashRedactionPolicy.redact("uri=content://media/external/video/12345 end")
        assertTrue(redacted.contains("content_media_id_12345"))
        assertFalse(redacted.contains("external/video"))
    }

    @Test
    fun otherContentUrisCollapseToPlaceholder() {
        val redacted = CrashRedactionPolicy.redact("uri=content://com.example.provider/path/secret end")
        assertTrue(redacted.contains("<content_uri>"))
        assertFalse(redacted.contains("secret"))
    }

    @Test
    fun nonPathTextIsUntouched() {
        val original = "java.lang.NullPointerException at SomeClass.foo(SomeClass.java:42)"
        assertEquals(original, CrashRedactionPolicy.redact(original))
    }

    @Test
    fun pathWithoutExtensionRedactsToFile() {
        val redacted = CrashRedactionPolicy.redact("/storage/emulated/0/Movies/somefolder")
        assertTrue(redacted.contains("<file>"))
        assertFalse(redacted.contains("somefolder"))
    }
}
