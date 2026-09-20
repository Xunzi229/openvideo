package com.openvideo.app.ui.player

import com.openvideo.app.core.subtitle.*
import org.junit.Assert.*
import org.junit.Test

class PlayerSessionRequestTest {
    @Test fun completingAnOlderRequestCannotReplaceTheLatestSelection() {
        val gate = LatestPlayerRequest()
        val old = gate.next()
        val latest = gate.next()
        assertTrue(gate.accepts(latest))
        assertFalse(gate.accepts(old))
        gate.next() // media switch invalidates even a currently latest result
        assertFalse(gate.accepts(latest))
    }

    @Test fun clearingSubtitlesResetsBothRenderedTracksAndExportData() {
        val items = listOf(SubtitleItem(1, 0, 1000, "old"))
        val old = PlayerUiState(title = "new video", subtitles = items, currentSubtitle = "old",
            dualSubtitles = DualSubtitleState(PrimarySubtitle(items), SecondarySubtitle(items, true)))
        val cleared = old.withoutSubtitles()
        assertTrue(cleared.subtitles.isEmpty())
        assertEquals("", cleared.currentSubtitle)
        assertNull(cleared.dualSubtitles.textAt(500))
        assertEquals("new video", cleared.title)
    }

    @Test fun compatibilityNeverSilentlyDropsAuthenticationHeaders() {
        assertTrue(CompatibilityRequestPolicy.supports(emptyMap()))
        assertTrue(CompatibilityRequestPolicy.supports(mapOf("user-agent" to "test", "REFERER" to "https://host")))
        for (name in listOf("Authorization", "Cookie", "X-API-Key")) {
            assertFalse(CompatibilityRequestPolicy.supports(mapOf(name to "secret")))
        }
    }
}
