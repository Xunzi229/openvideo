package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleCandidateSelectionPolicyTest {

    @Test
    fun autoAppliesUniqueHighestPriorityCandidate() {
        val exact = SubtitleCandidate.exactBaseName("/Movies/Demo.srt")
        val language = SubtitleCandidate.languageSuffix("/Movies/Demo.zh.srt")

        assertEquals(
            SubtitleCandidateSelection.AutoApply(exact),
            SubtitleCandidateSelectionPolicy.select(listOf(language, exact))
        )
    }

    @Test
    fun requiresChoiceWhenMultipleCandidatesShareTopPriority() {
        val chinese = SubtitleCandidate.languageSuffix("/Movies/Demo.zh.srt")
        val english = SubtitleCandidate.languageSuffix("/Movies/Demo.en.srt")
        val directory = SubtitleCandidate.subtitleDirectory("/Movies/Subtitles/Demo.srt")

        assertEquals(
            SubtitleCandidateSelection.RequiresUserChoice(listOf(english, chinese)),
            SubtitleCandidateSelectionPolicy.select(listOf(chinese, directory, english))
        )
    }

    @Test
    fun primaryLanguagePreferenceBreaksSamePriorityTie() {
        val chinese = SubtitleCandidate.languageSuffix("/Movies/Demo.zh.srt")
        val english = SubtitleCandidate.languageSuffix("/Movies/Demo.en.srt")

        assertEquals(
            SubtitleCandidateSelection.AutoApply(chinese),
            SubtitleCandidateSelectionPolicy.select(
                candidates = listOf(english, chinese),
                languagePreference = SubtitleLanguagePreference(
                    primary = SubtitleLanguage.CHINESE,
                    secondary = SubtitleLanguage.ENGLISH,
                    preferBilingual = false
                )
            )
        )
    }

    @Test
    fun secondaryLanguagePreferenceIsUsedWhenPrimaryIsMissing() {
        val english = SubtitleCandidate.languageSuffix("/Movies/Demo.en.srt")
        val japanese = SubtitleCandidate.languageSuffix("/Movies/Demo.jpn.srt")

        assertEquals(
            SubtitleCandidateSelection.AutoApply(english),
            SubtitleCandidateSelectionPolicy.select(
                candidates = listOf(japanese, english),
                languagePreference = SubtitleLanguagePreference(
                    primary = SubtitleLanguage.CHINESE,
                    secondary = SubtitleLanguage.ENGLISH,
                    preferBilingual = false
                )
            )
        )
    }

    @Test
    fun bilingualPreferenceWinsAmongSamePriorityCandidates() {
        val chinese = SubtitleCandidate.languageSuffix("/Movies/Demo.zh.srt")
        val bilingual = SubtitleCandidate.languageSuffix("/Movies/Demo.chn-eng.srt")

        assertEquals(
            SubtitleCandidateSelection.AutoApply(bilingual),
            SubtitleCandidateSelectionPolicy.select(
                candidates = listOf(chinese, bilingual),
                languagePreference = SubtitleLanguagePreference(
                    primary = SubtitleLanguage.CHINESE,
                    secondary = SubtitleLanguage.ENGLISH,
                    preferBilingual = true
                )
            )
        )
    }

    @Test
    fun rememberedCandidatePathAutoAppliesBeforeNormalRanking() {
        val exact = SubtitleCandidate.exactBaseName("/Movies/Demo.srt")
        val remembered = SubtitleCandidate.languageSuffix("/Movies/Demo.zh.srt")

        assertEquals(
            SubtitleCandidateSelection.AutoApply(remembered),
            SubtitleCandidateSelectionPolicy.select(
                candidates = listOf(exact, remembered),
                rememberedPath = "/Movies/Demo.zh.srt"
            )
        )
    }

    @Test
    fun missingRememberedCandidateFallsBackToNormalRanking() {
        val exact = SubtitleCandidate.exactBaseName("/Movies/Demo.srt")
        val language = SubtitleCandidate.languageSuffix("/Movies/Demo.zh.srt")

        assertEquals(
            SubtitleCandidateSelection.AutoApply(exact),
            SubtitleCandidateSelectionPolicy.select(
                candidates = listOf(language, exact),
                rememberedPath = "/Movies/Missing.srt"
            )
        )
    }

    @Test
    fun doesNotAutoApplyLowConfidenceCandidates() {
        assertEquals(
            SubtitleCandidateSelection.None,
            SubtitleCandidateSelectionPolicy.select(
                listOf(SubtitleCandidate.lowConfidence("/Downloads/Maybe.srt"))
            )
        )
    }

    @Test
    fun returnsNoneWhenThereAreNoCandidates() {
        assertEquals(
            SubtitleCandidateSelection.None,
            SubtitleCandidateSelectionPolicy.select(emptyList())
        )
    }
}
