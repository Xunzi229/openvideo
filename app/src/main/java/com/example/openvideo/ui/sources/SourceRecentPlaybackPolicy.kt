package com.example.openvideo.ui.sources

import android.content.Context
import com.example.openvideo.R
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.local.NetworkRecentItemEntity
import kotlin.math.roundToInt

enum class SourceRecentPlaybackType {
    LOCAL,
    NETWORK_URL
}

data class SourceRecentPlaybackItem(
    val stableId: String,
    val type: SourceRecentPlaybackType,
    val title: String,
    val playbackUri: String,
    val videoId: Long,
    val sourceLabel: String,
    val detailLabel: String,
    val durationMs: Long,
    val lastPlayedAt: Long,
    val isPlayable: Boolean
)

data class SourceRecentPlaybackLabels(
    val localSource: String,
    val networkUrlSource: String,
    val missingFile: String,
    val completed: String,
    val progressPercent: (Int) -> String
) {
    companion object {
        fun from(context: Context): SourceRecentPlaybackLabels {
            val resources = context.resources
            return SourceRecentPlaybackLabels(
                localSource = resources.getString(R.string.sources_recent_source_local),
                networkUrlSource = resources.getString(R.string.sources_recent_source_url),
                missingFile = resources.getString(R.string.history_continue_missing_file),
                completed = resources.getString(R.string.history_continue_completed),
                progressPercent = { percent ->
                    resources.getString(R.string.history_continue_progress_percent, percent)
                }
            )
        }

        fun englishDefaults(): SourceRecentPlaybackLabels = SourceRecentPlaybackLabels(
            localSource = "Local",
            networkUrlSource = "URL",
            missingFile = "Missing file",
            completed = "Completed",
            progressPercent = { percent -> "$percent%" }
        )
    }
}

object SourceRecentPlaybackPolicy {

    fun buildItems(
        history: List<HistoryEntity>,
        networkRecent: List<NetworkRecentItemEntity>,
        labels: SourceRecentPlaybackLabels,
        maxItems: Int,
        localFileExists: (String) -> Boolean
    ): List<SourceRecentPlaybackItem> {
        if (maxItems <= 0) return emptyList()
        val localItems = history.filterNot { entity -> isNetworkPlaybackUri(entity.path) }.map { entity ->
            val playable = localFileExists(entity.path)
            SourceRecentPlaybackItem(
                stableId = "local:${entity.videoId}",
                type = SourceRecentPlaybackType.LOCAL,
                title = entity.title,
                playbackUri = entity.path,
                videoId = entity.videoId,
                sourceLabel = labels.localSource,
                detailLabel = localDetailLabel(labels, entity, playable),
                durationMs = entity.duration,
                lastPlayedAt = entity.timestamp,
                isPlayable = playable
            )
        }
        val networkItems = networkRecent.map { entity ->
            SourceRecentPlaybackItem(
                stableId = "network:${entity.recentId}",
                type = SourceRecentPlaybackType.NETWORK_URL,
                title = entity.title,
                playbackUri = entity.normalizedUrl,
                videoId = entity.normalizedUrl.hashCode().toLong(),
                sourceLabel = labels.networkUrlSource,
                detailLabel = entity.displayUrl,
                durationMs = entity.durationMs,
                lastPlayedAt = entity.lastPlayedAt,
                isPlayable = true
            )
        }
        return (localItems + networkItems)
            .sortedByDescending { it.lastPlayedAt }
            .take(maxItems)
    }

    private fun localDetailLabel(
        labels: SourceRecentPlaybackLabels,
        entity: HistoryEntity,
        playable: Boolean
    ): String {
        if (!playable) return labels.missingFile
        if (entity.lastPosition <= 0L || entity.duration <= 0L) return labels.completed
        val ratio = (entity.lastPosition.toDouble() / entity.duration.toDouble()).coerceIn(0.0, 1.0)
        return labels.progressPercent((ratio * 100).roundToInt())
    }

    private fun isNetworkPlaybackUri(path: String): Boolean {
        val scheme = path.substringBefore(':', missingDelimiterValue = "").lowercase()
        return scheme == "http" || scheme == "https" || scheme == "rtsp"
    }
}
