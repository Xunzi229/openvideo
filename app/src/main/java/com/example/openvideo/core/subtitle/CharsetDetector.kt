package com.example.openvideo.core.subtitle

import java.io.File
import java.nio.charset.Charset

object CharsetDetector {

    fun detect(file: File): Charset {
        val bytes = file.readBytes()

        // Check BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return Charsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return Charsets.UTF_16LE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return Charsets.UTF_16BE
        }

        // Try UTF-8 first
        try {
            val text = String(bytes, Charsets.UTF_8)
            if (text.toByteArray(Charsets.UTF_8).contentEquals(bytes)) {
                return Charsets.UTF_8
            }
        } catch (_: Exception) {}

        // Try GBK for Chinese content
        try {
            val text = String(bytes, Charset.forName("GBK"))
            if (text.toByteArray(Charset.forName("GBK")).contentEquals(bytes)) {
                return Charset.forName("GBK")
            }
        } catch (_: Exception) {}

        // Try Shift_JIS for Japanese
        try {
            val text = String(bytes, Charset.forName("Shift_JIS"))
            if (text.toByteArray(Charset.forName("Shift_JIS")).contentEquals(bytes)) {
                return Charset.forName("Shift_JIS")
            }
        } catch (_: Exception) {}

        // Try EUC-KR for Korean
        try {
            val text = String(bytes, Charset.forName("EUC-KR"))
            if (text.toByteArray(Charset.forName("EUC-KR")).contentEquals(bytes)) {
                return Charset.forName("EUC-KR")
            }
        } catch (_: Exception) {}

        // Default to UTF-8
        return Charsets.UTF_8
    }
}
