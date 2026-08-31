package com.example.openvideo.ui.player

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

internal data class PlayerSessionQueueRecord(
    val id: Long,
    val title: String,
    val path: String,
    val uri: String,
    val duration: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val thumbnailUri: String?,
    val libraryPath: String,
    val dateModified: Long,
    val orientationDegrees: Int
)

internal object PlayerSessionQueueCodec {
    private const val MAGIC = 0x4F565151
    private const val VERSION = 1
    private const val MAX_ITEMS = 1_000_000
    private const val MAX_STRING_BYTES = 4 * 1024 * 1024

    fun write(records: List<PlayerSessionQueueRecord>, output: OutputStream) {
        write(records.asSequence(), records.size, output)
    }

    fun write(records: Sequence<PlayerSessionQueueRecord>, count: Int, output: OutputStream) {
        require(count in 0..MAX_ITEMS) { "Session queue is too large" }
        DataOutputStream(output.buffered()).use { data ->
            data.writeInt(MAGIC)
            data.writeInt(VERSION)
            data.writeInt(count)
            var writtenCount = 0
            records.forEach { record ->
                require(writtenCount < count) { "Session queue count does not match its records" }
                data.writeLong(record.id)
                data.writeString(record.title)
                data.writeString(record.path)
                data.writeString(record.uri)
                data.writeLong(record.duration)
                data.writeLong(record.size)
                data.writeInt(record.width)
                data.writeInt(record.height)
                data.writeLong(record.dateAdded)
                data.writeNullableString(record.thumbnailUri)
                data.writeString(record.libraryPath)
                data.writeLong(record.dateModified)
                data.writeInt(record.orientationDegrees)
                writtenCount += 1
            }
            require(writtenCount == count) { "Session queue count does not match its records" }
        }
    }

    fun read(input: InputStream): List<PlayerSessionQueueRecord> = read(input) { it }

    fun <T> read(input: InputStream, transform: (PlayerSessionQueueRecord) -> T): List<T> =
        DataInputStream(input.buffered()).use { data ->
            require(data.readInt() == MAGIC) { "Invalid session queue cache" }
            require(data.readInt() == VERSION) { "Unsupported session queue cache version" }
            val count = data.readInt()
            require(count in 0..MAX_ITEMS) { "Invalid session queue item count" }
            List(count) {
                transform(
                    PlayerSessionQueueRecord(
                        id = data.readLong(),
                        title = data.readString(),
                        path = data.readString(),
                        uri = data.readString(),
                        duration = data.readLong(),
                        size = data.readLong(),
                        width = data.readInt(),
                        height = data.readInt(),
                        dateAdded = data.readLong(),
                        thumbnailUri = data.readNullableString(),
                        libraryPath = data.readString(),
                        dateModified = data.readLong(),
                        orientationDegrees = data.readInt()
                    )
                )
            }
        }

    private fun DataOutputStream.writeString(value: String) {
        writeNullableString(value)
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        if (value == null) {
            writeInt(-1)
            return
        }
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_STRING_BYTES) { "Session queue value is too large" }
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readString(): String =
        requireNotNull(readNullableString()) { "Missing session queue value" }

    private fun DataInputStream.readNullableString(): String? {
        val size = readInt()
        if (size == -1) return null
        require(size in 0..MAX_STRING_BYTES) { "Invalid session queue value size" }
        val bytes = ByteArray(size)
        readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
