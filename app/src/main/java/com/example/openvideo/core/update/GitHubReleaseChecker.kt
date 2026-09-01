package com.example.openvideo.core.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Fetches and validates the project's latest GitHub Release. */
object GitHubReleaseChecker {

    private const val API_URL =
        "https://api.github.com/repos/Xunzi229/openvideo/releases/latest"

    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L

    data class LatestRelease(
        val tagName: String,
        val releaseHtmlUrl: String
    )

    fun shouldRunPeriodicCheck(lastCheckEpochMs: Long, now: Long = System.currentTimeMillis()): Boolean {
        return now - lastCheckEpochMs >= CHECK_INTERVAL_MS
    }

    fun fetchLatestRelease(userAgent: String): LatestRelease? {
        val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", userAgent)
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) return null
            if (conn.contentLengthLong > MAX_RELEASE_JSON_BYTES) return null
            val body = conn.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RELEASE_JSON_BYTES) return null
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
            parseLatest(body)
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    fun isRemoteNewer(remoteTagName: String, localVersionName: String): Boolean {
        val r = parseVersionParts(stripPrefixV(remoteTagName))
        val l = parseVersionParts(stripPrefixV(localVersionName))
        if (r.isEmpty() && l.isEmpty()) return false
        val maxLen = maxOf(r.size, l.size)
        for (i in 0 until maxLen) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            when {
                a > b -> return true
                a < b -> return false
            }
        }
        return false
    }

    private fun parseLatest(jsonBody: String): LatestRelease? {
        val root = JSONObject(jsonBody)
        val tag = root.optString("tag_name").ifBlank { return null }
        val htmlUrl = root.optString("html_url").ifBlank { return null }
        return trustedReleaseOrNull(tag, htmlUrl)
    }

    internal fun trustedReleaseOrNull(
        tagName: String,
        releaseHtmlUrl: String
    ): LatestRelease? {
        if (tagName.isBlank() || !UpdateUrlPolicy.isTrustedReleasePage(releaseHtmlUrl)) return null
        return LatestRelease(
            tagName = tagName,
            releaseHtmlUrl = releaseHtmlUrl
        )
    }

    private fun stripPrefixV(s: String): String {
        val t = s.trim()
        return if (t.startsWith("v", ignoreCase = true)) t.drop(1).trim() else t
    }

    private fun parseVersionParts(s: String): List<Int> =
        s.split('.').mapNotNull { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull()
        }

    private const val MAX_RELEASE_JSON_BYTES = 1024L * 1024L
}
