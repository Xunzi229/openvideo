package com.example.openvideo.ui.player

import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.local.VideoFolderGrouper
import java.util.Locale

object PlayerEpisodeOrderingPolicy {

    private val seasonEpisodePatterns = listOf(
        Regex("""(?i)\bS(\d{1,3})\s*E(\d{1,4})\b"""),
        Regex("""(?i)\b(\d{1,3})x(\d{1,4})\b""")
    )
    private val episodeOnlyPatterns = listOf(
        Regex("""第\s*(\d{1,4})\s*[集话話]"""),
        Regex("""(?i)\bEP(?:ISODE)?\.?\s*(\d{1,4})\b"""),
        Regex("""(?i)\bE(\d{1,4})\b""")
    )

    fun orderSameFolderQueue(videos: List<VideoItem>): List<VideoItem> =
        orderQueueIfEligible(videos)

    fun orderQueueIfEligible(videos: List<VideoItem>): List<VideoItem> {
        val candidates = videos.map(::candidateFor)
        if (!shouldOrderQueue(candidates)) return videos
        return orderByEpisode(videos) { candidateFor(it) }
    }

    fun shouldOrderQueue(candidates: List<PlayerEpisodeOrderingCandidate>): Boolean {
        if (candidates.size <= 1) return false
        if (isSameFolder(candidates)) return true
        return hasStrongEpisodeSignal(candidates)
    }

    fun orderCandidates(candidates: List<PlayerEpisodeOrderingCandidate>): List<PlayerEpisodeOrderingCandidate> =
        orderByEpisode(candidates) { it }

    private fun candidateFor(video: VideoItem): PlayerEpisodeOrderingCandidate =
        PlayerEpisodeOrderingCandidate(
            id = video.id,
            title = video.title,
            path = video.path,
            dateAdded = video.dateAdded
        )

    private fun isSameFolder(candidates: List<PlayerEpisodeOrderingCandidate>): Boolean {
        val folderKeys = candidates
            .map { VideoFolderGrouper.folderKey(it.path) }
            .filter { it != VideoFolderGrouper.UNKNOWN_FOLDER_KEY }
            .distinct()
        return folderKeys.size == 1
    }

    private fun hasStrongEpisodeSignal(candidates: List<PlayerEpisodeOrderingCandidate>): Boolean {
        val recognized = candidates.count { episodeKey(it) != null }
        return recognized >= 2 && recognized * 2 >= candidates.size
    }

    private fun <T> orderByEpisode(items: List<T>, candidate: (T) -> PlayerEpisodeOrderingCandidate): List<T> =
        items.sortedWith(
            compareBy<T>(
                { episodeKey(candidate(it))?.season ?: Int.MAX_VALUE },
                { episodeKey(candidate(it))?.episode ?: Int.MAX_VALUE },
                { fallbackTitle(candidate(it)) },
                { candidate(it).dateAdded },
                { candidate(it).id }
            )
        )

    private fun episodeKey(candidate: PlayerEpisodeOrderingCandidate): EpisodeKey? {
        val source = sequenceOf(candidate.title, candidate.path.substringAfterLast('/'))
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        seasonEpisodePatterns.forEach { pattern ->
            pattern.find(source)?.let { match ->
                return EpisodeKey(
                    season = match.groupValues[1].toIntOrNull() ?: 0,
                    episode = match.groupValues[2].toIntOrNull() ?: return@let
                )
            }
        }

        episodeOnlyPatterns.forEach { pattern ->
            pattern.find(source)?.let { match ->
                return EpisodeKey(
                    season = 0,
                    episode = match.groupValues[1].toIntOrNull() ?: return@let
                )
            }
        }

        return null
    }

    private fun fallbackTitle(candidate: PlayerEpisodeOrderingCandidate): String =
        candidate.title.ifBlank { candidate.path.substringAfterLast('/') }
            .lowercase(Locale.ROOT)

    private data class EpisodeKey(
        val season: Int,
        val episode: Int
    )
}

data class PlayerEpisodeOrderingCandidate(
    val id: Long,
    val title: String,
    val path: String,
    val dateAdded: Long
)
