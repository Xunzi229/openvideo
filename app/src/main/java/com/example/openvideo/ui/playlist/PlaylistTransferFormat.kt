package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity

object PlaylistTransferFormat {
    const val JSON_MIME_TYPE = "application/json"
    val SUPPORTED_IMPORT_MIME_TYPES = arrayOf(JSON_MIME_TYPE, "audio/x-mpegurl", "audio/mpegurl", "*/*")

    private const val SCHEMA_VERSION = 1

    data class ImportCandidate(
        val title: String,
        val path: String,
        val durationMs: Long,
        val position: Int
    )

    enum class FailureReason {
        INVALID_JSON,
        UNSUPPORTED_VERSION,
        EMPTY_PLAYLIST
    }

    sealed class ParseResult {
        data class Success(
            val playlistName: String?,
            val items: List<ImportCandidate>
        ) : ParseResult()

        data class Failure(val reason: FailureReason) : ParseResult()
    }

    fun suggestedJsonFileName(playlistName: String): String {
        val safeName = playlistName
            .trim()
            .replace(Regex("""[\\/:*?"<>|]+"""), "_")
            .ifBlank { "playlist" }
        return "$safeName.openvideo-playlist.json"
    }

    fun exportM3u(videos: List<PlaylistVideoEntity>): String =
        buildString {
            appendLine("#EXTM3U")
            videos.sortedBy { it.position }.forEach { video ->
                appendLine("#EXTINF:${video.videoDuration / 1000},${video.videoTitle}")
                appendLine(video.videoPath)
            }
        }.trimEnd()

    fun parseM3u(text: String): ParseResult {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        val items = mutableListOf<ImportCandidate>()
        var pendingTitle: String? = null
        var pendingDurationMs = 0L

        lines.forEach { line ->
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val payload = line.substringAfter(':')
                    pendingDurationMs = payload.substringBefore(',').toLongOrNull()?.times(1000L) ?: 0L
                    pendingTitle = payload.substringAfter(',', missingDelimiterValue = "").takeIf { it.isNotBlank() }
                }
                line.startsWith("#") -> Unit
                else -> {
                    val title = pendingTitle ?: line.substringAfterLast('/').ifBlank { line }
                    items += ImportCandidate(
                        title = title,
                        path = line,
                        durationMs = pendingDurationMs,
                        position = items.size
                    )
                    pendingTitle = null
                    pendingDurationMs = 0L
                }
            }
        }

        return if (items.isEmpty()) {
            ParseResult.Failure(FailureReason.EMPTY_PLAYLIST)
        } else {
            ParseResult.Success(playlistName = null, items = items)
        }
    }

    fun exportJson(playlistName: String, videos: List<PlaylistVideoEntity>): String {
        val videoJson = videos.sortedBy { it.position }.joinToString(separator = ",") { video ->
            """{"title":${quote(video.videoTitle)},"path":${quote(video.videoPath)},"durationMs":${video.videoDuration},"position":${video.position}}"""
        }
        return """{"schemaVersion":$SCHEMA_VERSION,"playlistName":${quote(playlistName)},"videos":[$videoJson]}"""
    }

    fun parseJson(text: String): ParseResult {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return ParseResult.Failure(FailureReason.INVALID_JSON)
        }

        val version = numberField(trimmed, "schemaVersion") ?: return ParseResult.Failure(FailureReason.INVALID_JSON)
        if (version != SCHEMA_VERSION.toLong()) {
            return ParseResult.Failure(FailureReason.UNSUPPORTED_VERSION)
        }

        val videosBody = arrayBody(trimmed, "videos") ?: return ParseResult.Failure(FailureReason.INVALID_JSON)
        val items = objectBodies(videosBody).mapNotNull { body ->
            val title = stringField(body, "title") ?: return@mapNotNull null
            val path = stringField(body, "path") ?: return@mapNotNull null
            ImportCandidate(
                title = title,
                path = path,
                durationMs = numberField(body, "durationMs") ?: 0L,
                position = numberField(body, "position")?.toInt() ?: 0
            )
        }.sortedBy { it.position }

        return if (items.isEmpty()) {
            ParseResult.Failure(FailureReason.EMPTY_PLAYLIST)
        } else {
            ParseResult.Success(
                playlistName = stringField(trimmed, "playlistName"),
                items = items.mapIndexed { index, item -> item.copy(position = index) }
            )
        }
    }

    private fun quote(raw: String): String = buildString {
        append('"')
        raw.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }

    private fun stringField(json: String, key: String): String? {
        val match = Regex(""""${Regex.escape(key)}"\s*:\s*"((?:\\.|[^"])*)"""")
            .find(json)
            ?: return null
        return unescape(match.groupValues[1])
    }

    private fun numberField(json: String, key: String): Long? =
        Regex(""""${Regex.escape(key)}"\s*:\s*(-?\d+)""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()

    private fun arrayBody(json: String, key: String): String? {
        val keyMatch = Regex(""""${Regex.escape(key)}"\s*:\s*\[""").find(json) ?: return null
        val start = keyMatch.range.last
        var depth = 1
        var inString = false
        var escaped = false
        for (index in start + 1 until json.length) {
            val char = json[index]
            when {
                escaped -> escaped = false
                inString && char == '\\' -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '[' -> depth++
                !inString && char == ']' -> {
                    depth--
                    if (depth == 0) return json.substring(start + 1, index)
                }
            }
        }
        return null
    }

    private fun objectBodies(arrayBody: String): List<String> {
        val bodies = mutableListOf<String>()
        var start = -1
        var depth = 0
        var inString = false
        var escaped = false
        arrayBody.forEachIndexed { index, char ->
            when {
                escaped -> escaped = false
                inString && char == '\\' -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> {
                    if (depth == 0) start = index
                    depth++
                }
                !inString && char == '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) bodies += arrayBody.substring(start, index + 1)
                }
            }
        }
        return bodies
    }

    private fun unescape(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            val char = value[index++]
            if (char != '\\' || index >= value.length) {
                append(char)
                continue
            }
            append(
                when (val escaped = value[index++]) {
                    '"' -> '"'
                    '\\' -> '\\'
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    else -> escaped
                }
            )
        }
    }
}
