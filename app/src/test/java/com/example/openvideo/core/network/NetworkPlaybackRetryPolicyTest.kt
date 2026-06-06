package com.example.openvideo.core.network

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class NetworkPlaybackRetryPolicyTest {

    @Test
    fun retryableNetworkErrorsUseBoundedExponentialBackoff() {
        val first = NetworkPlaybackRetryPolicy.nextDecision(
            errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            cause = null,
            completedAttempts = 0
        )
        val second = NetworkPlaybackRetryPolicy.nextDecision(
            errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            cause = null,
            completedAttempts = 1
        )
        val third = NetworkPlaybackRetryPolicy.nextDecision(
            errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            cause = null,
            completedAttempts = 2
        )
        val exhausted = NetworkPlaybackRetryPolicy.nextDecision(
            errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            cause = null,
            completedAttempts = 3
        )

        assertEquals(NetworkPlaybackRetryPolicy.Decision.Retry(delayMs = 1_000, nextAttempt = 1), first)
        assertEquals(NetworkPlaybackRetryPolicy.Decision.Retry(delayMs = 2_000, nextAttempt = 2), second)
        assertEquals(NetworkPlaybackRetryPolicy.Decision.Retry(delayMs = 4_000, nextAttempt = 3), third)
        assertEquals(NetworkPlaybackRetryPolicy.Decision.DoNotRetry, exhausted)
    }

    @Test
    fun nonRetryableNetworkErrorsDoNotAutoRetry() {
        assertEquals(
            NetworkPlaybackRetryPolicy.Decision.DoNotRetry,
            NetworkPlaybackRetryPolicy.nextDecision(
                errorCode = PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
                cause = null,
                completedAttempts = 0
            )
        )
        assertEquals(
            NetworkPlaybackRetryPolicy.Decision.DoNotRetry,
            NetworkPlaybackRetryPolicy.nextDecision(
                classification = NetworkErrorClassifier.classifyHttpStatus(404),
                completedAttempts = 0
            )
        )
    }

    @Test
    fun retryableHttpStatusesCanAutoRetry() {
        val decision = NetworkPlaybackRetryPolicy.nextDecision(
            classification = NetworkErrorClassifier.classifyHttpStatus(503),
            completedAttempts = 0
        )

        assertTrue(decision is NetworkPlaybackRetryPolicy.Decision.Retry)
    }

    @Test
    fun classifierHttpStatusRetryabilityFeedsRetryPolicy() {
        assertFalse(NetworkErrorClassifier.classifyHttpStatus(404).isRetryable)
        assertTrue(NetworkErrorClassifier.classifyHttpStatus(503).isRetryable)
    }

    @Test
    fun classifierReadsMedia3HttpResponseCodeException() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "core", "network", "NetworkErrorClassifier.kt")
        )

        assertTrue(source.contains("import androidx.media3.datasource.HttpDataSource"))
        assertTrue(source.contains("is HttpDataSource.InvalidResponseCodeException -> classifyHttpStatus(cause.responseCode)"))
    }

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
