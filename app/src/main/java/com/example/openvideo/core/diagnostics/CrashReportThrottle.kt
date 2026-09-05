package com.example.openvideo.core.diagnostics

import androidx.media3.common.PlaybackException

/** Bounds repeated nonfatal alerts within this process without retaining Throwables or media URLs. */
internal class CrashReportThrottle(
    private val intervalMs: Long = 10 * 60 * 1000L,
    private val maxEntries: Int = 64
) {
    init {
        require(intervalMs > 0)
        require(maxEntries > 0)
    }

    private val lastReports = linkedMapOf<String, Long>()

    @Synchronized
    fun shouldEnqueue(source: String, throwable: Throwable, nowMs: Long): Boolean {
        if (source == CrashCategoryPolicy.SOURCE_UNCAUGHT) return true
        val fingerprint = buildString {
            appendLine(source)
            appendLine((throwable as? PlaybackException)?.errorCode)
            CrashCategoryPolicy.throwableChain(throwable).forEach { error ->
                appendLine(error.javaClass.name)
                error.stackTrace.take(8).forEach { frame -> appendLine(frame.toString()) }
            }
        }
        val previous = lastReports[fingerprint]
        if (previous != null && nowMs >= previous && nowMs - previous < intervalMs) return false
        lastReports.remove(fingerprint)
        lastReports[fingerprint] = nowMs
        while (lastReports.size > maxEntries) {
            lastReports.remove(lastReports.keys.first())
        }
        return true
    }
}
