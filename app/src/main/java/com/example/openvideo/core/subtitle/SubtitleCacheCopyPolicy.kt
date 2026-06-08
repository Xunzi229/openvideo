package com.example.openvideo.core.subtitle

data class SubtitleCacheTarget(
    val directoryName: String,
    val fileName: String,
    val overwritesOriginal: Boolean = false
)

data class SubtitleCacheCopy(
    val fileName: String,
    val createdAtMs: Long,
    val sizeBytes: Long
)

data class SubtitleCacheRetentionPlan(
    val keep: List<SubtitleCacheCopy>,
    val delete: List<SubtitleCacheCopy>,
    val maxCopies: Int
)

enum class SubtitleCacheCopyPurpose(val suffix: String) {
    UTF8_EXPORT("utf8"),
    DELAY_CORRECTED("delay-corrected")
}

object SubtitleCacheCopyPolicy {

    const val DIRECTORY_NAME: String = "subtitles"
    const val DEFAULT_MAX_COPIES: Int = 10

    fun cacheTarget(
        sourceName: String,
        purpose: SubtitleCacheCopyPurpose,
        createdAtMs: Long
    ): SubtitleCacheTarget {
        val safeBaseName = sourceName
            .trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .substringBeforeLast('.', missingDelimiterValue = "")
            .sanitizeFilePart()
            .ifBlank { DEFAULT_BASE_NAME }
        val safeTimestamp = createdAtMs.coerceAtLeast(0L)
        return SubtitleCacheTarget(
            directoryName = DIRECTORY_NAME,
            fileName = "$safeBaseName.${purpose.suffix}.$safeTimestamp.srt"
        )
    }

    fun planRetention(
        existingCopies: List<SubtitleCacheCopy>,
        maxCopies: Int = DEFAULT_MAX_COPIES
    ): SubtitleCacheRetentionPlan {
        val safeMaxCopies = maxCopies.coerceAtLeast(0)
        val sorted = existingCopies.sortedByDescending { it.createdAtMs }
        return SubtitleCacheRetentionPlan(
            keep = sorted.take(safeMaxCopies),
            delete = sorted.drop(safeMaxCopies),
            maxCopies = safeMaxCopies
        )
    }

    private fun String.sanitizeFilePart(): String =
        replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_', '.', '-')

    private const val DEFAULT_BASE_NAME = "subtitle"
}
