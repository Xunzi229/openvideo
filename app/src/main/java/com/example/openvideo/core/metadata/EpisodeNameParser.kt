package com.example.openvideo.core.metadata

data class EpisodeMatch(
    val title: String,
    val season: Int?,
    val episodeStart: Int,
    val episodeEnd: Int?,
    val confidence: EpisodeMatchConfidence,
    val rule: String
)

enum class EpisodeMatchConfidence {
    HIGH,
    MEDIUM,
    LOW
}

object EpisodeNameParser {

    private val seasonEpisodePattern =
        Regex("""(?i)\bS(\d{1,3})\s*E(\d{1,4})(?:\s*-\s*E?(\d{1,4}))?\b""")
    private val oneByEpisodePattern = Regex("""(?i)\b(\d{1,3})x(\d{1,4})\b""")
    private val chineseEpisodePattern = Regex("""\u7b2c\s*(\d{1,4})\s*[\u96c6\u8bdd\u8a71]""")
    private val episodeOnlyPattern = Regex("""(?i)\b(?:EP(?:ISODE)?|E)\.?\s*(\d{1,4})\b""")
    private val bracketTokenPattern = Regex("""\[([^\]]+)]""")
    private val bracketEpisodePattern = Regex("""^\d{1,4}$""")
    private val noiseTokenPattern = Regex(
        """(?i)^(?:\d{3,4}p|[xh]\.?26[45]|hevc|avc|web[- ]?dl|webrip|bluray|bdrip|hdtv|dvdrip|jpsc|chs|cht|gb|big5|aac|ddp?|flac)$"""
    )

    fun parse(fileName: String, parentFolderName: String? = null): EpisodeMatch? {
        val source = fileName.trim()
        if (source.isBlank()) return null

        seasonEpisodePattern.find(source)?.let { match ->
            val episodeStart = match.groupValues[2].toIntOrNull() ?: return null
            val episodeEnd = match.groupValues[3].takeIf { it.isNotBlank() }?.toIntOrNull()
            if (episodeEnd != null && episodeEnd < episodeStart) return null
            val title = cleanTitle(source.substring(0, match.range.first), parentFolderName) ?: return null
            return EpisodeMatch(
                title = title.value,
                season = match.groupValues[1].toIntOrNull() ?: return null,
                episodeStart = episodeStart,
                episodeEnd = episodeEnd,
                confidence = EpisodeMatchConfidence.HIGH,
                rule = "season_episode"
            )
        }

        oneByEpisodePattern.find(source)?.let { match ->
            val title = cleanTitle(source.substring(0, match.range.first), parentFolderName) ?: return null
            return EpisodeMatch(
                title = title.value,
                season = match.groupValues[1].toIntOrNull() ?: return null,
                episodeStart = match.groupValues[2].toIntOrNull() ?: return null,
                episodeEnd = null,
                confidence = EpisodeMatchConfidence.HIGH,
                rule = "one_by_episode"
            )
        }

        chineseEpisodePattern.find(source)?.let { match ->
            val title = cleanTitle(source.substring(0, match.range.first), parentFolderName) ?: return null
            return EpisodeMatch(
                title = title.value,
                season = null,
                episodeStart = match.groupValues[1].toIntOrNull() ?: return null,
                episodeEnd = null,
                confidence = confidenceFor(EpisodeMatchConfidence.MEDIUM, title.fromParent),
                rule = "chinese_episode"
            )
        }

        parseBracketEpisode(source)?.let { return it }

        episodeOnlyPattern.find(source)?.let { match ->
            val title = cleanTitle(source.substring(0, match.range.first), parentFolderName) ?: return null
            return EpisodeMatch(
                title = title.value,
                season = null,
                episodeStart = match.groupValues[1].toIntOrNull() ?: return null,
                episodeEnd = null,
                confidence = confidenceFor(EpisodeMatchConfidence.MEDIUM, title.fromParent),
                rule = "episode_only"
            )
        }

        return null
    }

    private fun parseBracketEpisode(source: String): EpisodeMatch? {
        val tokens = bracketTokenPattern.findAll(source).map { it.groupValues[1].trim() }.toList()
        tokens.forEachIndexed { index, token ->
            if (!bracketEpisodePattern.matches(token)) return@forEachIndexed
            val title = tokens.subList(0, index)
                .asReversed()
                .firstNotNullOfOrNull { tokenTitle(it) }
                ?: return@forEachIndexed
            return EpisodeMatch(
                title = title,
                season = null,
                episodeStart = token.toIntOrNull() ?: return@forEachIndexed,
                episodeEnd = null,
                confidence = EpisodeMatchConfidence.MEDIUM,
                rule = "bracket_episode"
            )
        }
        return null
    }

    private fun tokenTitle(token: String): String? =
        if (noiseTokenPattern.matches(token)) {
            null
        } else {
            normalizeTitle(token).takeIf { it.length > 1 }
        }

    private fun cleanTitle(raw: String, parentFolderName: String?): TitleCandidate? {
        val cleaned = normalizeTitle(raw)
        if (cleaned.length > 1) {
            return TitleCandidate(cleaned, fromParent = false)
        }
        val fallback = normalizeTitle(parentFolderName.orEmpty())
        if (fallback.length > 1) {
            return TitleCandidate(fallback, fromParent = true)
        }
        return null
    }

    private fun normalizeTitle(raw: String): String =
        raw
            .replace(Regex("""\[[^\]]+]"""), " ")
            .replace(Regex("""[._-]+"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() && !noiseTokenPattern.matches(it) }
            .joinToString(" ")
            .trim()

    private fun confidenceFor(base: EpisodeMatchConfidence, fromParent: Boolean): EpisodeMatchConfidence =
        if (!fromParent) base else EpisodeMatchConfidence.LOW

    private data class TitleCandidate(
        val value: String,
        val fromParent: Boolean
    )
}
