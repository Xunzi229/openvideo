package com.example.openvideo.ui.sources

import com.example.openvideo.data.local.MediaSourceEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceDetailPresentationPolicyTest {

    @Test
    fun buildUiStateShowsUrlSourceNameAddressAndLastUsed() {
        val state = SourceDetailPresentationPolicy.buildUiState(
            source = mediaSource(lastUsedAt = 12_345L),
            labels = SourceDetailLabels.englishDefaults(),
            formatTimestamp = { timestamp -> "time:$timestamp" }
        )

        assertEquals("URL", state.typeLabel)
        assertEquals("Example stream", state.name)
        assertEquals("https://example.com/video.mp4?token=***", state.address)
        assertEquals("time:12345", state.lastUsedLabel)
        assertEquals(true, state.canTestConnection)
        assertEquals(true, state.canDelete)
    }

    @Test
    fun buildUiStateShowsNeverWhenSourceHasNotBeenUsed() {
        val state = SourceDetailPresentationPolicy.buildUiState(
            source = mediaSource(lastUsedAt = 0L),
            labels = SourceDetailLabels.englishDefaults(),
            formatTimestamp = { timestamp -> "time:$timestamp" }
        )

        assertEquals("Never", state.lastUsedLabel)
    }

    private fun mediaSource(lastUsedAt: Long) = MediaSourceEntity(
        sourceId = 7L,
        type = "url",
        name = "Example stream",
        url = "https://example.com/video.mp4?token=secret",
        normalizedUrl = "https://example.com/video.mp4?token=secret",
        displayUrl = "https://example.com/video.mp4?token=***",
        lastUsedAt = lastUsedAt
    )
}
