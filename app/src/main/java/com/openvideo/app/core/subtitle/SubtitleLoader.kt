package com.openvideo.app.core.subtitle

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.openvideo.app.core.network.WebDavMemoryCache
import com.openvideo.app.core.prefs.PlayerPrefs
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

        if (file.extension.lowercase() !in setOf("srt", "ass", "ssa", "vtt")) return emptyList()
        return try {
            val bytes = file.inputStream().use(SubtitleInput::readBounded)
            parseSubtitleContent(String(bytes, charsetForPreference(bytes)), file.extension)
        } catch (_: java.io.IOException) {
            emptyList()
        }
    }

    fun loadFromUri(uri: Uri): List<SubtitleItem> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use(SubtitleInput::readBounded)
                ?: return emptyList()
            val content = String(bytes, charsetForPreference(bytes)).removePrefix("\uFEFF")
            parseSubtitleContent(content, getExtensionFromUri(uri))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadFromNetworkUrl(url: String, requestHeaders: Map<String, String> = emptyMap()): List<SubtitleItem> {
        return try {
            val encoding = playerPrefs.subtitleEncoding
            val cacheKey = webDavMemoryCache.cacheKey(
                namespace = "subtitle:$encoding",
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
                val bytes = response.body.byteStream().use(SubtitleInput::readBounded)
                val content = String(bytes, charsetForPreference(bytes, encoding)).removePrefix("\uFEFF")
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
        if (uri.scheme == "content") {
            val displayName = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            }.getOrNull()
            displayName?.substringAfterLast('.', "")?.lowercase()?.let {
                if (it in setOf("srt", "ass", "ssa", "vtt")) return it
            }
            when (context.contentResolver.getType(uri)?.lowercase()) {
                "text/vtt" -> return "vtt"
                "text/x-ssa", "text/x-ass", "application/x-ass", "application/x-ssa" -> return "ass"
                "application/x-subrip" -> return "srt"
            }
        }
        return extensionFromPathOrUrl(uri.path.orEmpty())
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
            else -> when {
                content.trimStart().startsWith("WEBVTT") -> VttParser.parse(content)
                content.contains("[Events]", ignoreCase = true) -> AssParser.parse(content)
                else -> SrtParser.parse(content)
            }
        }

    private fun charsetForPreference(bytes: ByteArray, value: String = playerPrefs.subtitleEncoding): Charset {
        if (value == "auto") return CharsetDetector.detect(bytes)
        return runCatching { Charset.forName(value) }.getOrElse { CharsetDetector.detect(bytes) }
    }
}
