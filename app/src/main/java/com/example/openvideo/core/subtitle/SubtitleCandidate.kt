package com.example.openvideo.core.subtitle

data class SubtitleCandidate(
    val path: String,
    val language: SubtitleLanguage,
    val confidence: SubtitleCandidateConfidence,
    val reason: SubtitleCandidateReason
) {
    companion object {
        fun exactBaseName(path: String): SubtitleCandidate =
            candidate(path, SubtitleCandidateConfidence.HIGH, SubtitleCandidateReason.EXACT_BASENAME)

        fun languageSuffix(path: String): SubtitleCandidate =
            candidate(path, SubtitleCandidateConfidence.HIGH, SubtitleCandidateReason.LANGUAGE_SUFFIX)

        fun episodeMatch(path: String): SubtitleCandidate =
            candidate(path, SubtitleCandidateConfidence.MEDIUM, SubtitleCandidateReason.EPISODE_MATCH)

        fun subtitleDirectory(path: String): SubtitleCandidate =
            candidate(path, SubtitleCandidateConfidence.MEDIUM, SubtitleCandidateReason.SUBTITLE_DIRECTORY)

        fun lowConfidence(path: String): SubtitleCandidate =
            candidate(path, SubtitleCandidateConfidence.LOW, SubtitleCandidateReason.LOW_CONFIDENCE)

        fun inferLanguage(path: String): SubtitleLanguage {
            val name = path.substringAfterLast('/').substringBeforeLast('.').lowercase()
            if (bilingualTokens.any { name.contains(it) }) return SubtitleLanguage.BILINGUAL
            val tokens = languageTokens(name)
            if (tokens.any { it in bilingualTokens }) return SubtitleLanguage.BILINGUAL
            if (tokens.any { it in chineseTokens }) return SubtitleLanguage.CHINESE
            if (tokens.any { it in englishTokens }) return SubtitleLanguage.ENGLISH
            if (tokens.any { it in japaneseTokens }) return SubtitleLanguage.JAPANESE
            if (tokens.any { it in koreanTokens }) return SubtitleLanguage.KOREAN
            return SubtitleLanguage.UNKNOWN
        }

        private fun candidate(
            path: String,
            confidence: SubtitleCandidateConfidence,
            reason: SubtitleCandidateReason
        ): SubtitleCandidate =
            SubtitleCandidate(
                path = path,
                language = inferLanguage(path),
                confidence = confidence,
                reason = reason
            )

        private fun languageTokens(name: String): Set<String> =
            name.lowercase()
                .split(languageTokenSeparator)
                .filter { it.isNotBlank() }
                .toSet()

        private val languageTokenSeparator = Regex("[\\s._\\-]+")
        private val chineseTokens = setOf("zh", "chs", "cht", "cn", "sc", "tc", "\u7b80", "\u7e41")
        private val englishTokens = setOf("en", "eng", "english")
        private val japaneseTokens = setOf("ja", "jp", "jpn", "\u65e5")
        private val koreanTokens = setOf("ko", "kr", "kor", "\u97e9")
        private val bilingualTokens = setOf("bilingual", "\u53cc\u8bed", "\u7b80\u82f1", "chn-eng")
    }
}

enum class SubtitleLanguage {
    CHINESE,
    ENGLISH,
    JAPANESE,
    KOREAN,
    BILINGUAL,
    UNKNOWN
}

enum class SubtitleCandidateConfidence {
    HIGH,
    MEDIUM,
    LOW
}

enum class SubtitleCandidateReason {
    EXACT_BASENAME,
    LANGUAGE_SUFFIX,
    EPISODE_MATCH,
    SUBTITLE_DIRECTORY,
    LOW_CONFIDENCE
}
