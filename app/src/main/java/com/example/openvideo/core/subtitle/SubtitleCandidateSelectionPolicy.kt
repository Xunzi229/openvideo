package com.example.openvideo.core.subtitle

sealed interface SubtitleCandidateSelection {
    data class AutoApply(val candidate: SubtitleCandidate) : SubtitleCandidateSelection
    data class RequiresUserChoice(val candidates: List<SubtitleCandidate>) : SubtitleCandidateSelection
    data object None : SubtitleCandidateSelection
}

object SubtitleCandidateSelectionPolicy {
    fun select(
        candidates: List<SubtitleCandidate>,
        rememberedPath: String = "",
        languagePreference: SubtitleLanguagePreference = SubtitleLanguagePreference()
    ): SubtitleCandidateSelection {
        if (candidates.isEmpty()) return SubtitleCandidateSelection.None

        rememberedCandidate(candidates, rememberedPath)?.let {
            return SubtitleCandidateSelection.AutoApply(it)
        }

        val ranked = candidates
            .filter { it.confidence != SubtitleCandidateConfidence.LOW }
            .map { candidate -> priority(candidate) to candidate }
            .filter { it.first > 0 }
        if (ranked.isEmpty()) return SubtitleCandidateSelection.None

        val topPriority = ranked.minOf { it.first }
        val topCandidates = ranked
            .filter { it.first == topPriority }
            .map { it.second }
            .sortedBy { it.path.lowercase() }
        preferredCandidate(topCandidates, languagePreference)?.let {
            return SubtitleCandidateSelection.AutoApply(it)
        }

        return when (topCandidates.size) {
            0 -> SubtitleCandidateSelection.None
            1 -> SubtitleCandidateSelection.AutoApply(topCandidates.first())
            else -> SubtitleCandidateSelection.RequiresUserChoice(topCandidates)
        }
    }

    private fun priority(candidate: SubtitleCandidate): Int =
        when (candidate.reason) {
            SubtitleCandidateReason.EXACT_BASENAME -> 1
            SubtitleCandidateReason.LANGUAGE_SUFFIX -> 2
            SubtitleCandidateReason.EPISODE_MATCH -> 3
            SubtitleCandidateReason.SUBTITLE_DIRECTORY -> 4
            SubtitleCandidateReason.LOW_CONFIDENCE -> Int.MAX_VALUE
        }

    private fun rememberedCandidate(
        candidates: List<SubtitleCandidate>,
        rememberedPath: String
    ): SubtitleCandidate? {
        val normalized = rememberedPath.trim().lowercase()
        if (normalized.isBlank()) return null
        return candidates.firstOrNull {
            it.confidence != SubtitleCandidateConfidence.LOW &&
                it.path.lowercase() == normalized
        }
    }

    private fun preferredCandidate(
        candidates: List<SubtitleCandidate>,
        preference: SubtitleLanguagePreference
    ): SubtitleCandidate? {
        val preferredOrder = buildList {
            if (preference.preferBilingual) add(SubtitleLanguage.BILINGUAL)
            add(preference.primary)
            add(preference.secondary)
        }.filter { it != SubtitleLanguage.UNKNOWN }.distinct()

        for (language in preferredOrder) {
            val matches = candidates.filter { it.language == language }
            if (matches.size == 1) return matches.first()
            if (matches.size > 1) return null
        }
        return null
    }
}
