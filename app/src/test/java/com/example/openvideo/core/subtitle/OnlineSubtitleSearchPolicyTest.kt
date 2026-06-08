package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineSubtitleSearchPolicyTest {

    @Test
    fun manualSearchRequestTrimsTitleAndKeepsEpisodeLanguage() {
        val request = OnlineSubtitleSearchRequest.manual(
            title = "  Demo Show  ",
            season = 1,
            episode = 2,
            language = SubtitleLanguage.CHINESE
        )

        assertEquals("Demo Show", request.title)
        assertEquals(1, request.season)
        assertEquals(2, request.episode)
        assertEquals(SubtitleLanguage.CHINESE, request.language)
        assertEquals(OnlineSubtitleSearchTrigger.MANUAL, request.trigger)
        assertFalse(request.includeFileHash)
    }

    @Test
    fun manualSearchRejectsBlankTitle() {
        assertThrows(IllegalArgumentException::class.java) {
            OnlineSubtitleSearchRequest.manual(title = "   ")
        }
    }

    @Test
    fun privacyPolicyAllowsOnlyManualSearchWithoutFileHashByDefault() {
        val request = OnlineSubtitleSearchRequest.manual(title = "Demo")

        assertEquals(
            OnlineSubtitlePrivacyDecision.Allowed,
            OnlineSubtitlePrivacyPolicy.evaluate(request)
        )
        assertTrue(
            OnlineSubtitlePrivacyPolicy.evaluate(
                request.copy(trigger = OnlineSubtitleSearchTrigger.AUTOMATIC_VIDEO_OPEN)
            ) is OnlineSubtitlePrivacyDecision.Blocked
        )
        assertTrue(
            OnlineSubtitlePrivacyPolicy.evaluate(request.copy(includeFileHash = true))
                is OnlineSubtitlePrivacyDecision.Blocked
        )
    }

    @Test
    fun firstUseNoticeExplainsManualSearchPayloadAndRequiresConfirmation() {
        val notice = OnlineSubtitlePrivacyPolicy.firstUseNotice()

        assertEquals("Online subtitle privacy", notice.title)
        assertTrue(notice.requiresUserConfirmation)
        assertFalse(notice.includesFileHash)
        assertTrue(notice.sentFields.contains("title"))
        assertTrue(notice.sentFields.contains("season"))
        assertTrue(notice.sentFields.contains("episode"))
        assertTrue(notice.sentFields.contains("language"))
    }

    @Test
    fun resultAndDownloadModelsCarryDisplayAndDownloadData() {
        val result = OnlineSubtitleSearchResult(
            id = "os-1",
            language = SubtitleLanguage.ENGLISH,
            fileName = "Demo.en.srt",
            releaseName = "Demo.2026.1080p",
            source = "OpenSubtitles.com",
            downloadCount = 42,
            rating = 8.5f,
            download = OnlineSubtitleDownloadRequest(resultId = "os-1", fileName = "Demo.en.srt")
        )

        assertEquals("OpenSubtitles.com", result.source)
        assertEquals("Demo.en.srt", result.download.fileName)
    }
}
