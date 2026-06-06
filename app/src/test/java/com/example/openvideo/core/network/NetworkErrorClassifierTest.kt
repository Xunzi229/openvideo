package com.example.openvideo.core.network

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorClassifierTest {

    @Test
    fun classifiesMedia3NetworkErrors() {
        assertEquals(
            NetworkErrorClassifier.Type.CONNECTION_FAILED,
            NetworkErrorClassifier.classifyPlaybackError(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            ).type
        )
        assertEquals(
            NetworkErrorClassifier.Type.TIMEOUT,
            NetworkErrorClassifier.classifyPlaybackError(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            ).type
        )
        assertEquals(
            NetworkErrorClassifier.Type.BAD_HTTP_STATUS,
            NetworkErrorClassifier.classifyPlaybackError(
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
            ).type
        )
        assertEquals(
            NetworkErrorClassifier.Type.CLEARTEXT_NOT_PERMITTED,
            NetworkErrorClassifier.classifyPlaybackError(
                PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED
            ).type
        )
    }

    @Test
    fun classifiesCommonNetworkCauses() {
        assertEquals(
            NetworkErrorClassifier.Type.DNS_FAILED,
            NetworkErrorClassifier.classifyPlaybackError(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                UnknownHostException("example.test")
            ).type
        )
        assertEquals(
            NetworkErrorClassifier.Type.TIMEOUT,
            NetworkErrorClassifier.classifyPlaybackError(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                SocketTimeoutException("timeout")
            ).type
        )
        assertEquals(
            NetworkErrorClassifier.Type.CONNECTION_FAILED,
            NetworkErrorClassifier.classifyPlaybackError(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                ConnectException("refused")
            ).type
        )
    }

    @Test
    fun classifiesHttpStatusRetryability() {
        assertTrue(NetworkErrorClassifier.classifyHttpStatus(429).isRetryable)
        assertTrue(NetworkErrorClassifier.classifyHttpStatus(503).isRetryable)
        assertFalse(NetworkErrorClassifier.classifyHttpStatus(401).isRetryable)
        assertFalse(NetworkErrorClassifier.classifyHttpStatus(403).isRetryable)
        assertFalse(NetworkErrorClassifier.classifyHttpStatus(404).isRetryable)
    }

    @Test
    fun leavesLocalIoErrorsForExistingPlaybackErrorModel() {
        val result = NetworkErrorClassifier.classifyPlaybackError(
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        )

        assertEquals(NetworkErrorClassifier.Type.NON_NETWORK, result.type)
        assertFalse(result.isRetryable)
    }
}
