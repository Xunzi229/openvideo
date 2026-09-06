package com.example.openvideo.ui.player

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class PlayerSessionQueueCodecBoundaryTest {
    private val record = PlayerSessionQueueRecord(1, "title", "/a", "content://media/1", 1, 2, 3, 4, 5, null, "/a", 6, 0)
    private fun bytes(block: DataOutputStream.() -> Unit) = ByteArrayOutputStream().also { out -> DataOutputStream(out).use(block) }.toByteArray()

    @Test fun writerRejectsOutOfRangeAndMismatchingCounts() {
        for (count in listOf(-1, 1_000_001)) {
            assertThrows(IllegalArgumentException::class.java) { PlayerSessionQueueCodec.write(emptySequence(), count, ByteArrayOutputStream()) }
        }
        assertThrows(IllegalArgumentException::class.java) { PlayerSessionQueueCodec.write(sequenceOf(record), 0, ByteArrayOutputStream()) }
        assertThrows(IllegalArgumentException::class.java) { PlayerSessionQueueCodec.write(emptySequence(), 1, ByteArrayOutputStream()) }
        assertThrows(IllegalArgumentException::class.java) { PlayerSessionQueueCodec.write(listOf(record.copy(title = "x".repeat(4 * 1024 * 1024 + 1))), ByteArrayOutputStream()) }
        val empty = ByteArrayOutputStream()
        PlayerSessionQueueCodec.write(emptyList(), empty)
        assertTrue(PlayerSessionQueueCodec.read(empty.toByteArray().inputStream()).isEmpty())
    }

    @Test fun readerRejectsUnknownVersionInvalidCountsAndStringLengths() {
        val cases = listOf(bytes { writeInt(0x4F565151); writeInt(2) }) +
            listOf(-1, 1_000_001).map { count -> bytes { writeInt(0x4F565151); writeInt(1); writeInt(count) } } +
            listOf(-2, -1, 4 * 1024 * 1024 + 1).map { size -> bytes {
                writeInt(0x4F565151); writeInt(1); writeInt(1); writeLong(1); writeInt(size)
            } }
        cases.forEach { assertThrows(IllegalArgumentException::class.java) { PlayerSessionQueueCodec.read(it.inputStream()) } }
        val output = ByteArrayOutputStream()
        PlayerSessionQueueCodec.write(listOf(record.copy(title = "")), output)
        assertEquals(listOf(""), PlayerSessionQueueCodec.read(output.toByteArray().inputStream()) { it.title })
    }
}
