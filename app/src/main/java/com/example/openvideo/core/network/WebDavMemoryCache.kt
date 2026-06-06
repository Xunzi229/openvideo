package com.example.openvideo.core.network

import com.example.openvideo.core.subtitle.SubtitleItem
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavMemoryCache @Inject constructor() {
    private val directoryEntries = mutableMapOf<String, TimedValue<List<WebDavDirectoryParser.Entry>>>()
    private val subtitleEntries = mutableMapOf<String, TimedValue<List<SubtitleItem>>>()

    @Synchronized
    fun getDirectory(key: String, nowMs: Long = System.currentTimeMillis()): List<WebDavDirectoryParser.Entry>? =
        directoryEntries[key]?.takeIf { nowMs - it.storedAtMs <= DIRECTORY_TTL_MS }?.value

    @Synchronized
    fun putDirectory(
        key: String,
        entries: List<WebDavDirectoryParser.Entry>,
        nowMs: Long = System.currentTimeMillis()
    ) {
        directoryEntries[key] = TimedValue(entries, nowMs)
    }

    @Synchronized
    fun getSubtitle(key: String, nowMs: Long = System.currentTimeMillis()): List<SubtitleItem>? =
        subtitleEntries[key]?.takeIf { nowMs - it.storedAtMs <= SUBTITLE_TTL_MS }?.value

    @Synchronized
    fun putSubtitle(
        key: String,
        subtitles: List<SubtitleItem>,
        nowMs: Long = System.currentTimeMillis()
    ) {
        subtitleEntries[key] = TimedValue(subtitles, nowMs)
    }

    @Synchronized
    fun clear() {
        directoryEntries.clear()
        subtitleEntries.clear()
    }

    fun cacheKey(
        namespace: String,
        url: String,
        requestHeaders: Map<String, String>
    ): String {
        val normalizedHeaders = requestHeaders
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .entries
            .joinToString("\n") { (name, value) -> "${name.trim().lowercase()}:${value.trim()}" }
        return "$namespace:${sha256("$url\n$normalizedHeaders")}"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private data class TimedValue<T>(
        val value: T,
        val storedAtMs: Long
    )

    companion object {
        const val DIRECTORY_TTL_MS: Long = 60_000L
        const val SUBTITLE_TTL_MS: Long = 5 * 60_000L
    }
}
