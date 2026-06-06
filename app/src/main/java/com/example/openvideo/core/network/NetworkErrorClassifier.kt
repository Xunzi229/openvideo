package com.example.openvideo.core.network

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(UnstableApi::class)
object NetworkErrorClassifier {

    data class Result(
        val type: Type,
        val isRetryable: Boolean
    )

    enum class Type {
        CONNECTION_FAILED,
        DNS_FAILED,
        TIMEOUT,
        BAD_HTTP_STATUS,
        CLEARTEXT_NOT_PERMITTED,
        UNKNOWN_NETWORK,
        NON_NETWORK
    }

    fun classifyPlaybackError(errorCode: Int, cause: Throwable? = null): Result {
        classifyCause(cause)?.let { return it }

        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                Result(Type.CONNECTION_FAILED, isRetryable = true)
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                Result(Type.TIMEOUT, isRetryable = true)
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                Result(Type.BAD_HTTP_STATUS, isRetryable = true)
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                Result(Type.CLEARTEXT_NOT_PERMITTED, isRetryable = false)
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
                Result(Type.UNKNOWN_NETWORK, isRetryable = true)
            else ->
                Result(Type.NON_NETWORK, isRetryable = false)
        }
    }

    fun classifyHttpStatus(statusCode: Int): Result {
        val retryable = statusCode == 408 ||
            statusCode == 425 ||
            statusCode == 429 ||
            statusCode in 500..599
        return Result(Type.BAD_HTTP_STATUS, isRetryable = retryable)
    }

    private fun classifyCause(cause: Throwable?): Result? {
        return when (cause) {
            null -> null
            is HttpDataSource.InvalidResponseCodeException -> classifyHttpStatus(cause.responseCode)
            is UnknownHostException -> Result(Type.DNS_FAILED, isRetryable = true)
            is SocketTimeoutException -> Result(Type.TIMEOUT, isRetryable = true)
            is ConnectException -> Result(Type.CONNECTION_FAILED, isRetryable = true)
            else -> classifyCause(cause.cause)
        }
    }
}
