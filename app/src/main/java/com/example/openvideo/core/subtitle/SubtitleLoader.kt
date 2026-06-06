package com.example.openvideo.core.subtitle

import android.content.Context
import android.net.Uri
import com.example.openvideo.core.network.WebDavMemoryCache
import com.example.openvideo.core.prefs.PlayerPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val playerPrefs: PlayerPrefs,
    private val okHttpClient: OkHttpClient,
    private val webDavMemoryCache: WebDavMemoryCache
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
            parseSubtitleContent(content, ext)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadFromNetworkUrl(url: String, requestHeaders: Map<String, String> = emptyMap()): List<SubtitleItem> {
        return try {
            val cacheKey = webDavMemoryCache.cacheKey(
                namespace = "subtitle",
                url = url,
                requestHeaders = requestHeaders
            )
            webDavMemoryCache.getSubtitle(cacheKey)?.let { return it }
            val builder = Request.Builder().url(url)
            requestHeaders.forEach { (name, value) ->
                if (name.isNotBlank() && value.isNotBlank()) {
                    builder.header(name, value)
                }
            }
            val request = builder.build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val content = response.body.string()
                val subtitles = parseSubtitleContent(content, extensionFromPathOrUrl(url))
                webDavMemoryCache.putSubtitle(cacheKey, subtitles)
                subtitles
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun findSubtitleFiles(videoPath: String): List<File> {
        return findSubtitleCandidates(videoPath).map { File(it.path) }
    }

    fun findSubtitleCandidates(videoPath: String): List<SubtitleCandidate> {
        val videoFile = File(videoPath)
        if (!videoFile.exists()) return emptyList()

        val baseName = videoFile.nameWithoutExtension
        val filesByPath = SubtitleFileCandidateScanner.candidatesNear(videoPath)
            .associateBy { it.path }

        return SubtitleSidecarMatcher.matchCandidates(
            videoBaseName = baseName,
            candidates = filesByPath.values.map {
                SubtitleSidecarMatcher.CandidatePath(
                    path = it.path,
                    inSubtitleDirectory = it.inSubtitleDirectory
                )
            }
        )
    }

    private fun getExtensionFromUri(uri: Uri): String {
        val path = uri.path ?: return ""
        val dotIndex = path.lastIndexOf('.')
        return if (dotIndex >= 0) path.substring(dotIndex + 1).lowercase() else ""
    }

    private fun extensionFromPathOrUrl(value: String): String {
        val path = value.substringBefore('?').substringBefore('#')
        val dotIndex = path.lastIndexOf('.')
        return if (dotIndex >= 0) path.substring(dotIndex + 1).lowercase() else ""
    }

    private fun parseSubtitleContent(content: String, extension: String): List<SubtitleItem> =
        when (extension.lowercase()) {
            "srt" -> SrtParser.parse(content)
            "ass", "ssa" -> AssParser.parse(content)
            "vtt" -> VttParser.parse(content)
            else -> SrtParser.parse(content)
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
