package com.example.openvideo.core.mediaid

data class MediaIdentityCandidate(
    val identityId: Long,
    val currentVideoId: Long,
    val fingerprint: MediaFingerprint
)

enum class MediaIdentityMatchReason {
    SAME_VIDEO_ID,
    SAME_PATH,
    PATH_MIGRATION,
    RENAME_CANDIDATE
}

sealed class MediaIdentityMatchDecision {
    data class Matched(
        val identityId: Long,
        val reason: MediaIdentityMatchReason
    ) : MediaIdentityMatchDecision()

    data class Conflict(
        val reason: MediaIdentityMatchReason,
        val candidateIdentityIds: List<Long>
    ) : MediaIdentityMatchDecision()

    object NoMatch : MediaIdentityMatchDecision()
}

object MediaIdentityMatcher {

    private const val DEFAULT_TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000L

    fun match(
        currentVideoId: Long,
        current: MediaFingerprint,
        candidates: List<MediaIdentityCandidate>,
        timestampToleranceMs: Long = DEFAULT_TIMESTAMP_TOLERANCE_MS
    ): MediaIdentityMatchDecision =
        resolve(
            reason = MediaIdentityMatchReason.SAME_VIDEO_ID,
            candidates = candidates.filter {
                it.currentVideoId == currentVideoId && sameShape(it.fingerprint, current)
            }
        ) ?: resolve(
            reason = MediaIdentityMatchReason.SAME_PATH,
            candidates = candidates.filter {
                MediaFingerprintPolicy.strongMatch(it.fingerprint, current)
            }
        ) ?: resolve(
            reason = MediaIdentityMatchReason.PATH_MIGRATION,
            candidates = candidates.filter {
                pathMigrationMatch(
                    previous = it.fingerprint,
                    current = current,
                    timestampToleranceMs = timestampToleranceMs
                )
            }
        ) ?: resolve(
            reason = MediaIdentityMatchReason.RENAME_CANDIDATE,
            candidates = candidates.filter {
                MediaFingerprintPolicy.likelyRename(it.fingerprint, current)
            }
        ) ?: MediaIdentityMatchDecision.NoMatch

    private fun resolve(
        reason: MediaIdentityMatchReason,
        candidates: List<MediaIdentityCandidate>
    ): MediaIdentityMatchDecision? =
        when (candidates.size) {
            0 -> null
            1 -> MediaIdentityMatchDecision.Matched(
                identityId = candidates.first().identityId,
                reason = reason
            )
            else -> MediaIdentityMatchDecision.Conflict(
                reason = reason,
                candidateIdentityIds = candidates.map { it.identityId }
            )
        }

    private fun pathMigrationMatch(
        previous: MediaFingerprint,
        current: MediaFingerprint,
        timestampToleranceMs: Long
    ): Boolean =
        previous.normalizedPathKey != current.normalizedPathKey &&
            fileNameKey(previous) == fileNameKey(current) &&
            sameShape(previous, current) &&
            timestampDifference(previous.timestamp, current.timestamp) <= timestampToleranceMs

    private fun sameShape(left: MediaFingerprint, right: MediaFingerprint): Boolean =
        left.sizeBytes == right.sizeBytes &&
            left.durationMs == right.durationMs &&
            left.width == right.width &&
            left.height == right.height

    private fun fileNameKey(fingerprint: MediaFingerprint): String =
        fingerprint.normalizedPathKey.substringAfterLast('/')

    private fun timestampDifference(left: Long, right: Long): Long =
        if (left >= right) left - right else right - left
}
