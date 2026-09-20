package com.openvideo.app.core.subtitle

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class SubtitleInputTest {
    @Test fun acceptsExactByteLimitAndRejectsOneExtraByte() {
        val bytes = ByteArray(SubtitleInput.MAX_BYTES)
        assertArrayEquals(bytes, SubtitleInput.readBounded(ByteArrayInputStream(bytes)))
        assertThrows(IOException::class.java) {
            SubtitleInput.readBounded(ByteArrayInputStream(ByteArray(SubtitleInput.MAX_BYTES + 1)))
        }
    }

    @Test fun emptyAndPartialReadsPreserveContent() {
        assertEquals(0, SubtitleInput.readBounded(ByteArrayInputStream(byteArrayOf())).size)
        val input = object : ByteArrayInputStream("hello".toByteArray()) {
            override fun read(buffer: ByteArray, off: Int, len: Int): Int = super.read(buffer, off, minOf(1, len))
        }
        assertEquals("hello", String(SubtitleInput.readBounded(input)))
    }
}
