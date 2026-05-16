package com.example.openvideo.ui.history

import com.example.openvideo.data.local.HistoryEntity
import kotlin.math.roundToInt

data class HistoryContinueWatchingItem(
    val entity: HistoryEntity,
    val watchedTimeLabel: String,
    val progressLabel: String,
    val isAvailable: Boolean
)

object HistoryContinueWatchingPolicy {

    fun buildItems(
        history: List<HistoryEntity>,
        labels: HistoryContinueWatchingLabels,
        nowMs: Long,
        localFileExists: (String) -> Boolean
    ): List<HistoryContinueWatchingItem> {
        return history.map { entity ->
            val available = localFileExists(entity.path)
            HistoryContinueWatchingItem(
                entity = entity,
                watchedTimeLabel = relativeTimeLabel(labels, nowMs, entity.timestamp),
                progressLabel = progressLabel(labels, entity, available),
                isAvailable = available
            )
        }
    }

    private fun progressLabel(
        labels: HistoryContinueWatchingLabels,
        entity: HistoryEntity,
        available: Boolean
    ): String {
        if (!available) return labels.missingFile
        if (entity.lastPosition <= 0L || entity.duration <= 0L) return labels.completed
        val ratio = (entity.lastPosition.toDouble() / entity.duration.toDouble()).coerceIn(0.0, 1.0)
        return labels.progressPercent((ratio * 100).roundToInt())
    }

    private fun relativeTimeLabel(
        labels: HistoryContinueWatchingLabels,
        nowMs: Long,
        timestampMs: Long
    ): String {
        val delta = (nowMs - timestampMs).coerceAtLeast(0L)
        val minutes = delta / 60_000L
        val hours = delta / 3_600_000L
        val days = delta / 86_400_000L
        return when {
            minutes < 1L -> labels.justNow
            minutes < 60L -> labels.minutesAgo(minutes)
            hours < 24L -> labels.hoursAgo(hours)
            else -> labels.daysAgo(days)
        }
    }
}
