package com.example.openvideo.ui.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import com.example.openvideo.core.diagnostics.CrashCategory
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(UnstableApi::class)
class PlayerFailureClassificationPolicyTest {

    @Test
    fun unrecognizedExtractorFailureMapsToMediaInputEvenWhenWrappedAsIo() {
        val extractorFailure = UnrecognizedInputFormatException(
            "None of the available extractors could read the stream",
            Uri.EMPTY,
            emptyList()
        )

        assertEquals(
            CrashCategory.MEDIA_INPUT,
            PlayerFailureClassificationPolicy.category(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                IllegalStateException("source error", extractorFailure)
            )
        )
        assertEquals(
            true,
            PlayerFailureClassificationPolicy.isMediaInputFailure(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                IllegalStateException("source error", extractorFailure)
            )
        )
    }

    @Test
    fun malformedAndUnsupportedContainersMapToMediaInput() {
        assertEquals(
            CrashCategory.MEDIA_INPUT,
            PlayerFailureClassificationPolicy.category(
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                null
            )
        )
        assertEquals(
            CrashCategory.MEDIA_INPUT,
            PlayerFailureClassificationPolicy.category(
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                null
            )
        )
    }

    @Test
    fun decoderAndIoRuntimeFailuresRemainPlaybackFailures() {
        assertEquals(
            CrashCategory.PLAYBACK,
            PlayerFailureClassificationPolicy.category(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                IllegalStateException("codec init failed")
            )
        )
        assertEquals(
            CrashCategory.PLAYBACK,
            PlayerFailureClassificationPolicy.category(
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                java.io.FileNotFoundException("missing")
            )
        )
    }
}
