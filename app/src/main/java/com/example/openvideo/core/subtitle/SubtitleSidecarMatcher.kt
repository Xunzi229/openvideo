package com.example.openvideo.core.subtitle

import com.example.openvideo.core.metadata.EpisodeMatch
import com.example.openvideo.core.metadata.EpisodeNameParser

object SubtitleSidecarMatcher {
    private val subtitleExtensions = setOf("srt", "ass", "ssa", "vtt")
    private val languageSuffixPattern = Regex("^[a-z]{2,3}([_-][a-z0-9]{2,8})*$", RegexOption.IGNORE_CASE)

    data class CandidatePath(
        val path: String,
        val inSubtitleDirectory: Boolean = false
    )

    fun isSupportedSubtitlePath(path: String): Boolean {
        val cleanPath = path.substringBefore('?').substringBefore('#').trimEnd('/')
        val extension = cleanPath.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return extension in subtitleExtensions
    }

    fun matchSameDirectory(
        videoBaseName: String,
        candidatePaths: List<String>
    ): List<SubtitleCandidate> =
        matchCandidates(
            videoBaseName = videoBaseName,
            candidates = candidatePaths.map { CandidatePath(it) }
        )

    fun matchCandidates(
        videoBaseName: String,
        candidates: List<CandidatePath>
    ): List<SubtitleCandidate> {
        val normalizedBase = videoBaseName.lowercase()
        val videoEpisode = EpisodeNameParser.parse(videoBaseName)
        return candidates
            .mapNotNull { item ->
                val path = item.path
                val cleanPath = path.substringBefore('?').substringBefore('#')
                if (cleanPath.endsWith('/')) return@mapNotNull null
                val fileName = cleanPath.substringAfterLast('/')
                val dotIndex = fileName.lastIndexOf('.')
                if (dotIndex <= 0 || dotIndex == fileName.lastIndex) return@mapNotNull null

                val extension = fileName.substring(dotIndex + 1).lowercase()
                if (extension !in subtitleExtensions) return@mapNotNull null

                val nameWithoutExtension = fileName.substring(0, dotIndex)
                val normalizedName = nameWithoutExtension.lowercase()
                val reason = when {
                    normalizedName == normalizedBase -> MatchReason.EXACT_BASENAME
                    normalizedName.startsWith("$normalizedBase.") -> {
                        val suffix = nameWithoutExtension.substring(videoBaseName.length + 1)
                        if (!languageSuffixPattern.matches(suffix)) return@mapNotNull null
                        MatchReason.LANGUAGE_SUFFIX
                    }
                    videoEpisode != null && isSameEpisode(videoEpisode, EpisodeNameParser.parse(nameWithoutExtension)) ->
                        MatchReason.EPISODE_MATCH
                    else -> return@mapNotNull null
                }
                rankedCandidate(path, reason, item.inSubtitleDirectory)
            }
            .sortedWith(compareBy<RankedCandidate> { it.score }.thenBy { it.candidate.path.lowercase() })
            .map { it.candidate }
    }

    private data class RankedCandidate(
        val score: Int,
        val candidate: SubtitleCandidate
    )

    private enum class MatchReason {
        EXACT_BASENAME,
        LANGUAGE_SUFFIX,
        EPISODE_MATCH
    }

    private fun rankedCandidate(
        path: String,
        reason: MatchReason,
        inSubtitleDirectory: Boolean
    ): RankedCandidate {
        if (inSubtitleDirectory) {
            return RankedCandidate(
                score = 3,
                candidate = SubtitleCandidate.subtitleDirectory(path)
            )
        }
        return when (reason) {
            MatchReason.EXACT_BASENAME -> RankedCandidate(
                score = 0,
                candidate = SubtitleCandidate.exactBaseName(path)
            )
            MatchReason.LANGUAGE_SUFFIX -> RankedCandidate(
                score = 1,
                candidate = SubtitleCandidate.languageSuffix(path)
            )
            MatchReason.EPISODE_MATCH -> RankedCandidate(
                score = 2,
                candidate = SubtitleCandidate.episodeMatch(path)
            )
        }
    }

    private fun isSameEpisode(video: EpisodeMatch, subtitle: EpisodeMatch?): Boolean {
        if (subtitle == null) return false
        if (!video.title.equals(subtitle.title, ignoreCase = true)) return false
        if (video.season != subtitle.season) return false
        return video.episodeStart == subtitle.episodeStart
    }
}
