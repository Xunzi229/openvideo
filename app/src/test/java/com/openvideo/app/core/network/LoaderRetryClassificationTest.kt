package com.openvideo.app.core.network

import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.upstream.Loader
import org.junit.Assert.assertEquals
import org.junit.Test

class LoaderRetryClassificationTest {
    @Test fun extractorRuntimeFailureDoesNotScheduleNetworkRetries() {
        val error = IllegalStateException("source", Loader.UnexpectedLoaderException(IllegalArgumentException()))
        assertEquals(NetworkPlaybackRetryPolicy.Decision.DoNotRetry,
            NetworkPlaybackRetryPolicy.nextDecision(PlaybackException.ERROR_CODE_IO_UNSPECIFIED, error, 0))
    }

    @Test fun actualTimeoutStillUsesBoundedNetworkRetry() {
        assertEquals(NetworkPlaybackRetryPolicy.Decision.Retry(1000, 1),
            NetworkPlaybackRetryPolicy.nextDecision(PlaybackException.ERROR_CODE_IO_UNSPECIFIED, java.net.SocketTimeoutException(), 0))
    }
}
