package com.example.openvideo.core.subtitle

data class SubtitleUtf8ExportPlan(
    val content: String,
    val bytes: ByteArray,
    val suggestedCopyName: String,
    val charsetName: String = "UTF-8",
    val lineCount: Int,
    val overwritesOriginal: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubtitleUtf8ExportPlan) return false
        return content == other.content &&
            bytes.contentEquals(other.bytes) &&
            suggestedCopyName == other.suggestedCopyName &&
            charsetName == other.charsetName &&
            lineCount == other.lineCount &&
            overwritesOriginal == other.overwritesOriginal
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + suggestedCopyName.hashCode()
        result = 31 * result + charsetName.hashCode()
        result = 31 * result + lineCount
        result = 31 * result + overwritesOriginal.hashCode()
        return result
    }
}

object SubtitleUtf8ExportPolicy {

    fun planSrtCopy(
        items: List<SubtitleItem>,
        sourceName: String
    ): SubtitleUtf8ExportPlan {
        val content = items
            .sortedWith(compareBy<SubtitleItem> { it.startTimeMs }.thenBy { it.endTimeMs })
            .mapIndexed { index, item ->
                buildString {
                    append(index + 1)
                    append('\n')
                    append(formatSrtTime(item.startTimeMs))
                    append(" --> ")
                    append(formatSrtTime(item.endTimeMs))
                    append('\n')
                    append(item.text)
                }
            }
            .joinToString(separator = "\n\n", postfix = "\n")

        return SubtitleUtf8ExportPlan(
            content = content,
            bytes = content.toByteArray(Charsets.UTF_8),
            suggestedCopyName = suggestedCopyName(sourceName),
            lineCount = items.size
        )
    }

    fun targetsOriginalSubtitle(
        targetUri: String,
        originalSubtitleUri: String
    ): Boolean {
        val target = targetUri.trim()
        val original = originalSubtitleUri.trim()
        if (target.isBlank() || original.isBlank()) return false
        if (target == original) return true

        val targetPath = normalizedLocalPath(target) ?: return false
        val originalPath = normalizedLocalPath(original) ?: return false
        return targetPath == originalPath
    }

    private fun formatSrtTime(timeMs: Long): String {
        val safeMs = timeMs.coerceAtLeast(0L)
        val hours = safeMs / 3_600_000
        val minutes = (safeMs % 3_600_000) / 60_000
        val seconds = (safeMs % 60_000) / 1_000
        val millis = safeMs % 1_000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    private fun suggestedCopyName(sourceName: String): String {
        val cleanName = sourceName.trim().substringAfterLast('/').substringAfterLast('\\')
        val dotIndex = cleanName.lastIndexOf('.')
        val baseName = cleanName
            .takeIf { it.isNotBlank() && dotIndex > 0 }
            ?.substring(0, dotIndex)
            ?: DEFAULT_BASE_NAME
        return "$baseName.utf8.srt"
    }

    private fun normalizedLocalPath(value: String): String? {
        var path = value.trim()
            .substringBefore('?')
            .substringBefore('#')
        if (path.startsWith(FILE_URI_PREFIX, ignoreCase = true)) {
            path = path.drop(FILE_URI_PREFIX.length)
        }
        if (path.length >= 3 && path[0] == '/' && path[2] == ':') {
            path = path.drop(1)
        }
        val isLocalPath = path.startsWith("/") || WINDOWS_DRIVE_PATH.matches(path)
        if (!isLocalPath) return null
        return path
            .replace('\\', '/')
            .replace(Regex("/{2,}"), "/")
    }

    private const val DEFAULT_BASE_NAME = "subtitle"
    private const val FILE_URI_PREFIX = "file://"
    private val WINDOWS_DRIVE_PATH = Regex("^[A-Za-z]:[\\\\/].+")
}
