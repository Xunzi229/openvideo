package com.example.openvideo.core.diagnostics

import android.os.RemoteException
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import java.io.IOException
import java.util.concurrent.CancellationException

/** Remote alerts are for crashes and internal failures; expected playback failures stay local. */
@OptIn(UnstableApi::class)
object CrashReportingPolicy {

    fun shouldReport(source: String, throwable: Throwable): Boolean = when (source) {
        CrashCategoryPolicy.SOURCE_UNCAUGHT,
        CrashCategoryPolicy.SOURCE_OEM_THREAD -> true
        CrashCategoryPolicy.SOURCE_PLAYER -> shouldReportPlayerFailure(
            errorCode = (throwable as? PlaybackException)?.errorCode,
            throwable = throwable
        )
        else -> false
    }

    fun shouldReportPlayerFailure(errorCode: Int?, throwable: Throwable): Boolean {
        val chain = CrashCategoryPolicy.throwableChain(throwable)
        // Fatal VM/linkage failures must survive even an inaccurate playback error code.
        if (chain.any { it is Error }) return true
        if (isExpectedPlaybackError(errorCode)) return false
        val rootCause = chain.last()
        if (rootCause is IOException || rootCause is SecurityException || rootCause is CancellationException) {
            return false
        }

        return when (errorCode) {
            PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED,
            PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED,
            PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED,
            PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR -> true
            // Decoder init/decode errors also cover unsupported or damaged input. Require a
            // concrete internal cause instead of treating every decoder failure as a system bug.
            else -> rootCause is RuntimeException || rootCause is RemoteException
        }
    }

    private fun isExpectedPlaybackError(errorCode: Int?): Boolean = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
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
        PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED,
        PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR,
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
        PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION,
        PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED,
        PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED -> true
        else -> false
    }
}
