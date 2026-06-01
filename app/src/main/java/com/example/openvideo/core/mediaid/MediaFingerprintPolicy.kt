package com.example.openvideo.core.mediaid

import java.util.Locale

data class MediaFingerprint(
    val normalizedPathKey: String,
    val normalizedTitleKey: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val timestamp: Long
)

object MediaFingerprintPolicy {

    fun fromFields(
        title: String,
        pathOrUri: String,
        sizeBytes: Long,
        durationMs: Long,
        width: Int,
        height: Int,
        timestamp: Long
    ): MediaFingerprint? {
        if (sizeBytes <= 0L || durationMs <= 0L) return null
        val path = MediaPathNormalizer.normalize(pathOrUri) ?: return null
        return MediaFingerprint(
            normalizedPathKey = path.comparisonKey,
            normalizedTitleKey = normalizeTitle(title),
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            width = width,
            height = height,
            timestamp = timestamp
        )
    }

    fun strongMatch(left: MediaFingerprint, right: MediaFingerprint): Boolean =
        left.normalizedPathKey == right.normalizedPathKey &&
            left.sizeBytes == right.sizeBytes &&
            left.durationMs == right.durationMs

    fun likelyRename(left: MediaFingerprint, right: MediaFingerprint): Boolean =
        left.normalizedPathKey != right.normalizedPathKey &&
            left.sizeBytes == right.sizeBytes &&
            left.durationMs == right.durationMs &&
            left.width == right.width &&
            left.height == right.height

    private fun normalizeTitle(title: String): String =
        title
            .replace(Regex("""[._-]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .lowercase(Locale.ROOT)
}
