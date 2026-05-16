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
        nowMs: Long,
        localFileExists: (String) -> Boolean
    ): List<HistoryContinueWatchingItem> {
        return history.map { entity ->
            val available = localFileExists(entity.path)
            HistoryContinueWatchingItem(
                entity = entity,
                watchedTimeLabel = relativeTimeLabel(nowMs, entity.timestamp),
                progressLabel = progressLabel(entity, available),
                isAvailable = available
            )
        }
    }

    private fun progressLabel(entity: HistoryEntity, available: Boolean): String {
        if (!available) return "Missing file"
        if (entity.lastPosition <= 0L || entity.duration <= 0L) return "Completed"
        val ratio = (entity.lastPosition.toDouble() / entity.duration.toDouble()).coerceIn(0.0, 1.0)
        return "${(ratio * 100).roundToInt()}%"
    }

    private fun relativeTimeLabel(nowMs: Long, timestampMs: Long): String {
        val delta = (nowMs - timestampMs).coerceAtLeast(0L)
        val minutes = delta / 60_000L
        val hours = delta / 3_600_000L
        val days = delta / 86_400_000L
        return when {
            minutes < 1L -> "Just now"
            minutes < 60L -> "$minutes min ago"
            hours < 24L -> "$hours hr ago"
            else -> "$days d ago"
        }
    }
}
