package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.Charset

class CharsetDetectorTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun detectsUtfBomsBeforeTryingLegacyDecoders() {
        val cases = listOf(
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) to Charsets.UTF_8,
            byteArrayOf(0xFF.toByte(), 0xFE.toByte()) to Charsets.UTF_16LE,
            byteArrayOf(0xFE.toByte(), 0xFF.toByte()) to Charsets.UTF_16BE
        )
        cases.forEach { (bytes, expected) -> assertEquals(expected, detect(bytes)) }
    }

    @Test
    fun treatsEmptyAsciiAndBomlessUnicodeAsUtf8() {
        listOf("", "Hello", "\u4e2d\u6587\u65e5\u672c\u8a9e").forEach { text ->
            assertEquals(Charsets.UTF_8, detect(text.toByteArray(Charsets.UTF_8)))
        }
    }

    @Test
    fun detectsGbkWhenUtf8CannotRoundTripTheFile() {
        val gbk = Charset.forName("GBK")
        assertEquals(gbk, detect("\u4e2d\u6587".toByteArray(gbk)))
    }

    @Test
    fun detectsShiftJisHalfWidthCharacterWhenGbkCannotDecodeIt() {
        assertEquals(Charset.forName("Shift_JIS"), detect(byteArrayOf(0xA6.toByte())))
    }

    @Test
    fun fallsBackToUtf8ForUndecodableBytes() {
        assertEquals(Charsets.UTF_8, detect(byteArrayOf(0xFF.toByte())))
    }

    @Test
    fun reportsMissingFileToCaller() {
        assertThrows(FileNotFoundException::class.java) {
            CharsetDetector.detect(File(temporaryFolder.root, "missing.srt"))
        }
    }

    private fun detect(bytes: ByteArray): Charset = temporaryFolder.newFile().let {
        it.writeBytes(bytes)
        CharsetDetector.detect(it)
    }
}
