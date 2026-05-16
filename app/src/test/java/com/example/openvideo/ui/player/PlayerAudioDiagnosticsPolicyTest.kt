package com.example.openvideo.ui.player

import com.example.openvideo.core.player.PlayerAudioDiagnostics
import com.example.openvideo.core.player.PlayerAudioTrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerAudioDiagnosticsPolicyTest {

    @Test
    fun trackSummaryIncludesCodecLanguageChannelsSampleRateAndBitrate() {
        val summary = PlayerAudioDiagnosticsPolicy.trackSummary(
            track = track(
                mimeType = "audio/eac3",
                language = "en",
                channelCount = 6,
                sampleRate = 48000,
                bitrate = 768000
            ),
            streamLabel = "Stream 1",
            unsupportedLabel = "Unsupported"
        )

        assertEquals("Stream 1 / E-AC-3 / en / 5.1 / 48000 Hz / 768 kbps", summary)
    }

    @Test
    fun runtimeInputSummaryIncludesFallbackTagWhenNeeded() {
        val summary = PlayerAudioDiagnosticsPolicy.runtimeInputSummary(
            diagnostics = PlayerAudioDiagnostics(
                lastInputMimeType = "audio/vnd.dts",
                lastInputLanguage = "ja",
                lastInputChannelCount = 8,
                lastInputSampleRate = 96000,
                lastInputNeedsSoftwareFallback = true
            ),
            softwareFallbackLabel = "Needs software fallback"
        )

        assertEquals("DTS/DCA / ja / 7.1 / 96000 Hz / Needs software fallback", summary)
    }

    @Test
    fun compatibilityMessageDistinguishesActiveFallbackFromMissingFallback() {
        assertEquals(
            "Software fallback active",
            PlayerAudioDiagnosticsPolicy.compatibilityMessage(
                diagnostics = PlayerAudioDiagnostics(
                    lastDecoderName = "ffmpegAudioDecoder",
                    lastInputNeedsSoftwareFallback = true
                ),
                fallbackActiveLabel = "Software fallback active",
                fallbackNeededLabel = "Software fallback needed"
            )
        )

        assertEquals(
            "Software fallback needed",
            PlayerAudioDiagnosticsPolicy.compatibilityMessage(
                diagnostics = PlayerAudioDiagnostics(
                    lastDecoderName = "c2.android.audio.decoder",
                    lastInputNeedsSoftwareFallback = true
                ),
                fallbackActiveLabel = "Software fallback active",
                fallbackNeededLabel = "Software fallback needed"
            )
        )
    }

    @Test
    fun quickTrackSummaryMatchesInfoSummaryWithoutSupportSuffix() {
        val summary = PlayerAudioDiagnosticsPolicy.quickTrackSummary(
            track = track(
                mimeType = "audio/mp4a-latm",
                language = "zh",
                channelCount = 2,
                sampleRate = 44100,
                bitrate = 128000
            ),
            streamLabel = "Stream 2"
        )

        assertTrue(summary.contains("AAC"))
        assertTrue(summary.contains("zh"))
        assertTrue(summary.contains("Stereo"))
        assertTrue(summary.contains("44100 Hz"))
        assertTrue(summary.contains("128 kbps"))
    }

    private fun track(
        mimeType: String = "audio/mp4a-latm",
        language: String? = "en",
        channelCount: Int = 2,
        sampleRate: Int = 48000,
        bitrate: Int = 128000,
        supported: Boolean = true
    ) = PlayerAudioTrackInfo(
        groupIndex = 0,
        trackIndex = 0,
        mimeType = mimeType,
        language = language,
        channelCount = channelCount,
        sampleRate = sampleRate,
        bitrate = bitrate,
        selected = false,
        supported = supported
    )
}
