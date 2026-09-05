package com.example.openvideo.core.diagnostics

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException

@OptIn(UnstableApi::class)
class CrashReportingPolicyTest {

    @Test
    fun reportedMissingContentUriAndUnrecognizedNetworkInputStayLocal() {
        assertFalse(CrashReportingPolicy.shouldReportPlayerFailure(
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            IOException("ContentDataSource", FileNotFoundException("ENOENT"))
        ))
        assertFalse(CrashReportingPolicy.shouldReportPlayerFailure(
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            IOException("None of the available extractors could read the stream")
        ))
    }

    @Test
    fun expectedMediaNetworkAndPermissionCodesDoNotAlertEvenWithRuntimeWrappers() {
        val codes = listOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
            PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
            PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED
        )
        codes.forEach { code ->
            assertFalse("Expected playback code $code should stay local",
                CrashReportingPolicy.shouldReportPlayerFailure(code, IllegalStateException("wrapped input")))
        }
    }

    @Test
    fun wrappedExpectedFailuresAndCancellationStayLocalWithoutAnErrorCode() {
        listOf(
            FileNotFoundException("missing"), SocketTimeoutException("timeout"),
            SecurityException("denied"), CancellationException("cancelled")
        ).forEach { cause ->
            assertFalse(CrashReportingPolicy.shouldReport(
                CrashCategoryPolicy.SOURCE_PLAYER, IllegalStateException("wrapper", cause)
            ))
        }
    }

    @Test
    fun caughtProgramErrorsAndExplicitSystemErrorCodesAreReported() {
        listOf(NullPointerException("view"), IllegalStateException("Fragment not attached"),
            IllegalArgumentException("invalid internal state")).forEach { error ->
            assertTrue(CrashReportingPolicy.shouldReport(CrashCategoryPolicy.SOURCE_PLAYER, error))
        }
        listOf(
            PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED,
            PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED,
            PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED,
            PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR
        ).forEach { code ->
            assertTrue("Internal code $code must alert",
                CrashReportingPolicy.shouldReportPlayerFailure(code, Exception("internal failure")))
        }
    }

    @Test
    fun decoderFailuresRequireAnInternalCause() {
        listOf(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED).forEach { code ->
            assertFalse(CrashReportingPolicy.shouldReportPlayerFailure(code, Exception("no suitable decoder")))
            assertTrue(CrashReportingPolicy.shouldReportPlayerFailure(
                code, Exception("decoder failed", IllegalStateException("codec state"))
            ))
        }
    }

    @Test
    fun unknownPlaybackErrorsNeedEvidenceOfAnInternalFailure() {
        assertFalse(CrashReportingPolicy.shouldReportPlayerFailure(9876, Exception("unknown")))
        assertTrue(CrashReportingPolicy.shouldReportPlayerFailure(9876, NullPointerException("internal")))
    }

    @Test
    fun unexpectedLoaderIoWrapperDoesNotHideAnInternalRuntimeFailure() {
        assertTrue(CrashReportingPolicy.shouldReportPlayerFailure(
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            IOException("Unexpected loader exception", NullPointerException("internal bug"))
        ))
        assertFalse(CrashReportingPolicy.shouldReportPlayerFailure(
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            IllegalStateException("source error", FileNotFoundException("missing"))
        ))
    }

    @Test
    fun memoryAndLinkageErrorsAreNeverFilteredByAnInputErrorCode() {
        listOf(OutOfMemoryError("heap"), UnsatisfiedLinkError("native library")).forEach { error ->
            assertTrue(CrashReportingPolicy.shouldReportPlayerFailure(
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND, RuntimeException("wrapper", error)
            ))
        }
    }

    @Test
    fun allUncaughtCrashesAndIsolatedVendorThreadFailuresAreReported() {
        listOf(FileNotFoundException("missing"), IOException("bad media"),
            SecurityException("denied"), RuntimeException("unknown")).forEach { error ->
            assertTrue(CrashReportingPolicy.shouldReport(CrashCategoryPolicy.SOURCE_UNCAUGHT, error))
        }
        assertTrue(CrashReportingPolicy.shouldReport(
            CrashCategoryPolicy.SOURCE_OEM_THREAD, NullPointerException("KeyMapObserver.stop")
        ))
        assertFalse(CrashReportingPolicy.shouldReport("diagnostic", RuntimeException("trace")))
    }

    @Test
    fun cyclicCausesDoNotHangReporting() {
        val first = RuntimeException("first")
        val second = RuntimeException("second", first)
        first.initCause(second)
        assertTrue(CrashReportingPolicy.shouldReport(CrashCategoryPolicy.SOURCE_PLAYER, first))
    }
}
