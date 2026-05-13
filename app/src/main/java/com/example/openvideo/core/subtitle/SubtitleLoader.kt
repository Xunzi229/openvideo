package com.example.openvideo.core.subtitle

import android.content.Context
import android.net.Uri
import com.example.openvideo.core.prefs.PlayerPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val playerPrefs: PlayerPrefs
) {

    fun loadFromFile(file: File): List<SubtitleItem> {
        if (!file.exists()) return emptyList()

        val charset = charsetForPreference(file)
        val content = file.readText(charset)

        return when (file.extension.lowercase()) {
            "srt" -> SrtParser.parse(content)
            "ass", "ssa" -> AssParser.parse(content)
            "vtt" -> VttParser.parse(content)
            else -> emptyList()
        }
    }

    fun loadFromUri(uri: Uri): List<SubtitleItem> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val charset = charsetForPreference(null)
            val content = inputStream.bufferedReader(charset).readText()
            inputStream.close()

            val ext = getExtensionFromUri(uri)
            when (ext) {
                "srt" -> SrtParser.parse(content)
                "ass", "ssa" -> AssParser.parse(content)
                "vtt" -> VttParser.parse(content)
                else -> SrtParser.parse(content) // Default to SRT
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun findSubtitleFiles(videoPath: String): List<File> {
        val videoFile = File(videoPath)
        if (!videoFile.exists()) return emptyList()

        val dir = videoFile.parentFile ?: return emptyList()
        val baseName = videoFile.nameWithoutExtension

        return dir.listFiles { file ->
            file.isFile && file.nameWithoutExtension.equals(baseName, ignoreCase = true) &&
                file.extension.lowercase() in listOf("srt", "ass", "ssa", "vtt")
        }?.sortedBy { it.name } ?: emptyList()
    }

    private fun getExtensionFromUri(uri: Uri): String {
        val path = uri.path ?: return ""
        val dotIndex = path.lastIndexOf('.')
        return if (dotIndex >= 0) path.substring(dotIndex + 1).lowercase() else ""
    }

    private fun charsetForPreference(file: File?): Charset {
        val value = playerPrefs.subtitleEncoding
        if (value == "auto") {
            return file?.let(CharsetDetector::detect) ?: Charsets.UTF_8
        }
        return runCatching { Charset.forName(value) }.getOrElse {
            file?.let(CharsetDetector::detect) ?: Charsets.UTF_8
        }
    }
}
