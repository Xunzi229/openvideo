package com.openvideo.app.core.network

import com.openvideo.app.core.subtitle.SubtitleItem
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavMemoryCache @Inject constructor() {
    private val directoryEntries = TimedCache<List<WebDavDirectoryParser.Entry>>(
        DIRECTORY_TTL_MS, 2L * 1024 * 1024
    ) { entries -> entries.sumOf { 128L + 2L * (it.name.length + it.url.length) } }
    private val subtitleEntries = TimedCache<List<SubtitleItem>>(
        SUBTITLE_TTL_MS, 8L * 1024 * 1024
    ) { items -> items.sumOf { 256L + 2L * it.text.length } }

    @Synchronized
    fun getDirectory(key: String, nowMs: Long = System.currentTimeMillis()): List<WebDavDirectoryParser.Entry>? =
        directoryEntries.get(key, nowMs)

    @Synchronized
    fun putDirectory(key: String, entries: List<WebDavDirectoryParser.Entry>, nowMs: Long = System.currentTimeMillis()) {
        directoryEntries.put(key, entries, nowMs)
    }

    @Synchronized
    fun getSubtitle(key: String, nowMs: Long = System.currentTimeMillis()): List<SubtitleItem>? =
        subtitleEntries.get(key, nowMs)

    @Synchronized
    fun putSubtitle(key: String, subtitles: List<SubtitleItem>, nowMs: Long = System.currentTimeMillis()) {
        subtitleEntries.put(key, subtitles, nowMs)
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

    private class TimedCache<T>(
        private val ttlMs: Long,
        private val maxBytes: Long,
        private val weigh: (T) -> Long
    ) {
        private data class Entry<T>(val value: T, val timeMs: Long, val bytes: Long)
        private val entries = LinkedHashMap<String, Entry<T>>(32, 0.75f, true)
        private var bytes = 0L

        fun get(key: String, nowMs: Long): T? {
            expire(nowMs)
            return entries[key]?.value
        }

        fun put(key: String, value: T, nowMs: Long) {
            expire(nowMs)
            entries.remove(key)?.let { bytes -= it.bytes }
            val size = weigh(value) + key.length * 2L + 64L
            if (size > maxBytes) return
            entries[key] = Entry(value, nowMs, size)
            bytes += size
            val iterator = entries.iterator()
            while (bytes > maxBytes || entries.size > MAX_ENTRIES) {
                bytes -= iterator.next().value.bytes
                iterator.remove()
            }
        }

        private fun expire(nowMs: Long) {
            val iterator = entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next().value
                if (nowMs < entry.timeMs || nowMs - entry.timeMs > ttlMs) {
                    bytes -= entry.bytes
                    iterator.remove()
                }
            }
        }

        fun clear() { entries.clear(); bytes = 0L }
    }

    companion object {
        const val MAX_ENTRIES = 32
        const val DIRECTORY_TTL_MS: Long = 60_000L
        const val SUBTITLE_TTL_MS: Long = 5 * 60_000L
    }
}
