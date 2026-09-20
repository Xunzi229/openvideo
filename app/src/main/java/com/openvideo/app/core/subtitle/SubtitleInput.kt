package com.openvideo.app.core.subtitle

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object SubtitleInput {
    const val MAX_BYTES = 4 * 1024 * 1024

    fun readBounded(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            if (total > MAX_BYTES) throw IOException("Subtitle exceeds size limit")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
