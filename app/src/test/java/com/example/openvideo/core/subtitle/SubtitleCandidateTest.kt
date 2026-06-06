package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleCandidateTest {

    @Test
    fun exactBaseNameCandidateKeepsPathAndUsesHighConfidence() {
        val candidate = SubtitleCandidate.exactBaseName("/Movies/Demo.srt")

        assertEquals("/Movies/Demo.srt", candidate.path)
        assertEquals(SubtitleLanguage.UNKNOWN, candidate.language)
        assertEquals(SubtitleCandidateConfidence.HIGH, candidate.confidence)
        assertEquals(SubtitleCandidateReason.EXACT_BASENAME, candidate.reason)
    }

    @Test
    fun languageInferenceUsesCommonSubtitleSuffixesAndKeywords() {
        assertEquals(SubtitleLanguage.CHINESE, SubtitleCandidate.inferLanguage("/Movies/Demo.chs.ass"))
        assertEquals(SubtitleLanguage.CHINESE, SubtitleCandidate.inferLanguage("/Movies/Demo.zh-CN.srt"))
        assertEquals(SubtitleLanguage.ENGLISH, SubtitleCandidate.inferLanguage("/Movies/Demo.english.vtt"))
        assertEquals(SubtitleLanguage.JAPANESE, SubtitleCandidate.inferLanguage("/Movies/Demo.jpn.srt"))
        assertEquals(SubtitleLanguage.KOREAN, SubtitleCandidate.inferLanguage("/Movies/Demo.kor.srt"))
        assertEquals(SubtitleLanguage.BILINGUAL, SubtitleCandidate.inferLanguage("/Movies/Demo.chn-eng.srt"))
        assertEquals(SubtitleLanguage.BILINGUAL, SubtitleCandidate.inferLanguage("/Movies/Demo.\u7b80\u82f1.srt"))
        assertEquals(SubtitleLanguage.UNKNOWN, SubtitleCandidate.inferLanguage("/Movies/Demo.forced.srt"))
    }

    @Test
    fun matchReasonsMapToConservativeConfidenceLevels() {
        assertEquals(
            SubtitleCandidateConfidence.HIGH,
            SubtitleCandidate.languageSuffix("/Movies/Demo.zh.srt").confidence
        )
        assertEquals(
            SubtitleCandidateConfidence.MEDIUM,
            SubtitleCandidate.episodeMatch("/Movies/Subtitles/Demo.S01E02.srt").confidence
        )
        assertEquals(
            SubtitleCandidateConfidence.MEDIUM,
            SubtitleCandidate.subtitleDirectory("/Movies/Subtitles/Demo.srt").confidence
        )
        assertEquals(
            SubtitleCandidateConfidence.LOW,
            SubtitleCandidate.lowConfidence("/Downloads/Maybe.srt").confidence
        )
    }

    @Test
    fun factoryMethodsInferLanguageFromPath() {
        assertEquals(
            SubtitleLanguage.CHINESE,
            SubtitleCandidate.languageSuffix("/Movies/Demo.cht.srt").language
        )
        assertEquals(
            SubtitleLanguage.ENGLISH,
            SubtitleCandidate.episodeMatch("/Movies/Demo.S01E02.eng.srt").language
        )
    }
}
