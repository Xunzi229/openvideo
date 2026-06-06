package com.example.openvideo.core.network

object NetworkPlaybackRetryPolicy {
    private const val MAX_AUTO_RETRY_ATTEMPTS = 3
    private const val BASE_DELAY_MS = 1_000L

    sealed class Decision {
        data class Retry(val delayMs: Long, val nextAttempt: Int) : Decision()
        data object DoNotRetry : Decision()
    }

    fun nextDecision(
        errorCode: Int,
        cause: Throwable?,
        completedAttempts: Int
    ): Decision {
        val classification = NetworkErrorClassifier.classifyPlaybackError(errorCode, cause)
        return nextDecision(classification, completedAttempts)
    }

    fun nextDecision(
        classification: NetworkErrorClassifier.Result,
        completedAttempts: Int
    ): Decision {
        if (!classification.isRetryable || completedAttempts >= MAX_AUTO_RETRY_ATTEMPTS) {
            return Decision.DoNotRetry
        }
        return Decision.Retry(
            delayMs = BASE_DELAY_MS shl completedAttempts.coerceAtLeast(0),
            nextAttempt = completedAttempts + 1
        )
    }
}
