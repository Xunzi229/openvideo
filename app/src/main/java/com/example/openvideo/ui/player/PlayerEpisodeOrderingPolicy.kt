package com.example.openvideo.ui.player

import com.example.openvideo.data.model.VideoItem
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
        orderByEpisode(videos) { video ->
            PlayerEpisodeOrderingCandidate(
                id = video.id,
                title = video.title,
                path = video.path,
                dateAdded = video.dateAdded
            )
        }

    fun orderCandidates(candidates: List<PlayerEpisodeOrderingCandidate>): List<PlayerEpisodeOrderingCandidate> =
        orderByEpisode(candidates) { it }

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
