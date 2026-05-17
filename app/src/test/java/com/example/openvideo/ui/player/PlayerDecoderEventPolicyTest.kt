package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDecoderEventPolicyTest {

    @Test
    fun videoDecoderEventsKeepNameAndAddSoftwareTagWhenSoftware() {
        assertEquals(
            listOf("video_decoder=OMX.google.h264.decoder", "video_decoder_software"),
            PlayerDecoderEventPolicy.videoDecoderEvents("OMX.google.h264.decoder")
        )
    }

    @Test
    fun videoDecoderEventsDoNotTagHardwareDecoders() {
        assertEquals(
            listOf("video_decoder=c2.qti.avc.decoder"),
            PlayerDecoderEventPolicy.videoDecoderEvents("c2.qti.avc.decoder")
        )
    }

    @Test
    fun audioDecoderEventsTagFfmpegFallback() {
        val events = PlayerDecoderEventPolicy.audioDecoderEvents("libffmpegAudioDecoder")
        assertTrue(events.contains("audio_decoder=libffmpegAudioDecoder"))
        assertTrue(events.contains("audio_decoder_software"))
    }

    @Test
    fun emptyDecoderNameProducesNoEvents() {
        assertTrue(PlayerDecoderEventPolicy.videoDecoderEvents("   ").isEmpty())
        assertTrue(PlayerDecoderEventPolicy.audioDecoderEvents("").isEmpty())
    }

    @Test
    fun isSoftwareDecoderRecognizesCommonAospAndFfmpegNames() {
        assertTrue(PlayerDecoderEventPolicy.isSoftwareDecoder("c2.android.aac.decoder"))
        assertTrue(PlayerDecoderEventPolicy.isSoftwareDecoder("OMX.google.h264.decoder"))
        assertTrue(PlayerDecoderEventPolicy.isSoftwareDecoder("ffmpegAudioDecoder"))
        assertFalse(PlayerDecoderEventPolicy.isSoftwareDecoder("OMX.qcom.video.decoder.avc"))
        assertFalse(PlayerDecoderEventPolicy.isSoftwareDecoder("c2.exynos.h264.decoder"))
    }

    @Test
    fun codecErrorEventsKeepShortClassName() {
        assertEquals(
            listOf("video_codec_error=DecoderInitializationException"),
            PlayerDecoderEventPolicy.videoCodecErrorEvents(
                "androidx.media3.exoplayer.mediacodec.MediaCodecRenderer\$DecoderInitializationException"
            )
        )
        assertEquals(
            listOf("audio_codec_error=DecoderQueryException"),
            PlayerDecoderEventPolicy.audioCodecErrorEvents(
                "androidx.media3.exoplayer.mediacodec.MediaCodecUtil\$DecoderQueryException"
            )
        )
    }

    @Test
    fun codecErrorEventsAreEmptyWhenClassNameIsBlank() {
        assertTrue(PlayerDecoderEventPolicy.videoCodecErrorEvents("").isEmpty())
        assertTrue(PlayerDecoderEventPolicy.audioCodecErrorEvents("   ").isEmpty())
    }
}
