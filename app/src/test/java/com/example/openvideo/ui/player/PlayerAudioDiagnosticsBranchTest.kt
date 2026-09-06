package com.example.openvideo.ui.player

import com.example.openvideo.core.player.PlayerAudioDiagnostics
import com.example.openvideo.core.player.PlayerAudioTrackInfo
import org.junit.Assert.*
import org.junit.Test

class PlayerAudioDiagnosticsBranchTest {
    private fun track(mime: String = "audio/ac3", language: String? = null, channels: Int = 0, rate: Int = 0, bitrate: Int = 0) =
        PlayerAudioTrackInfo(0, 0, mime, language, channels, rate, bitrate, false, false)

    @Test fun allSoftwareFallbackFormatsAndDecoderNamesAreRecognized() {
        for (mime in listOf("audio/vnd.dts", "audio/vnd.dts.hd", "audio/vnd.dts.uhd", "audio/x-dts", "custom/dts", "custom/dca")) {
            assertTrue(mime, track(mime.uppercase()).isDtsAudio)
            assertTrue(mime, track(mime).requiresSoftwareAudioFallback)
        }
        for (mime in listOf("audio/true-hd", "audio/mlp", "custom/truehd", "custom/mlp")) {
            assertFalse(mime, track(mime).isDtsAudio)
            assertTrue(mime, track(mime).requiresSoftwareAudioFallback)
        }
        assertFalse(track("audio/aac").requiresSoftwareAudioFallback)
        for ((name, expected) in listOf(null to false, "hardware" to false, "FFMPEG.audio" to true)) {
            assertEquals(expected, PlayerAudioDiagnostics(lastDecoderName = name).isUsingFfmpegDecoder)
        }
    }

    @Test fun summariesOmitUnknownFieldsAndIncludeKnownFields() {
        for (language in listOf(null, "", " ", "und", "en")) for (known in listOf(false, true)) {
            val info = track(language = language, channels = if (known) 2 else 0, rate = if (known) 48000 else 0,
                bitrate = if (known) 128000 else 0)
            val expected = listOfNotNull("Stream", "AC-3", language?.takeIf { it == "en" },
                if (known) "Stereo" else null, if (known) "48000 Hz" else null, if (known) "128 kbps" else null).joinToString(" / ")
            assertEquals(expected, PlayerAudioDiagnosticsPolicy.quickTrackSummary(info, "Stream"))
            assertEquals("$expected / unsupported", PlayerAudioDiagnosticsPolicy.trackSummary(info, "Stream", "unsupported"))
            assertEquals(expected, PlayerAudioDiagnosticsPolicy.trackSummary(info.copy(supported = true), "Stream", "unsupported"))
            for (mime in listOf(null, "", "audio/ac3")) {
                val diagnostic = PlayerAudioDiagnostics(lastInputMimeType = mime, lastInputLanguage = language,
                    lastInputChannelCount = info.channelCount, lastInputSampleRate = info.sampleRate)
                val expectedRuntime = listOfNotNull(if (mime == "audio/ac3") "AC-3" else null,
                    language?.takeIf { it == "en" }, if (known) "Stereo" else null, if (known) "48000 Hz" else null)
                assertEquals(expectedRuntime.takeIf { it.isNotEmpty() }?.joinToString(" / "),
                    PlayerAudioDiagnosticsPolicy.runtimeInputSummary(diagnostic, "software"))
            }
        }
    }

    @Test fun codecChannelAndBitrateLabelsCoverKnownAndFallbackValues() {
        val codecs = mapOf("audio/mp4a-latm" to "AAC", "audio/ac3" to "AC-3", "audio/eac3" to "E-AC-3",
            "audio/vnd.dts" to "DTS/DCA", "audio/vnd.dts.hd" to "DTS-HD", "audio/vnd.dts.uhd" to "DTS-UHD",
            "audio/x-dts" to "DTS/DCA", "audio/true-hd" to "Dolby TrueHD", "audio/mlp" to "MLP", "" to "Audio", "custom" to "custom")
        codecs.forEach { (mime, label) -> assertEquals(label, PlayerAudioDiagnosticsPolicy.codecLabel(mime)) }
        mapOf(0 to null, 1 to "Mono", 2 to "Stereo", 6 to "5.1", 8 to "7.1", 4 to "4 ch").forEach { (count, label) ->
            assertEquals(label, PlayerAudioDiagnosticsPolicy.channelLabel(count))
        }
        assertNull(PlayerAudioDiagnosticsPolicy.bitrateLabel(0))
        assertEquals("1.0 Mbps", PlayerAudioDiagnosticsPolicy.bitrateLabel(1_000_000))
        assertEquals("999 kbps", PlayerAudioDiagnosticsPolicy.bitrateLabel(999_999))
    }
}
