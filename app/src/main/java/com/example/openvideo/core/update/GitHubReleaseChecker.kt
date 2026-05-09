package com.example.openvideo.core.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches [GitHub latest release API](https://docs.github.com/en/rest/releases/releases#get-the-latest-release)
 * and compares [tag_name] to the installed app version. Requires a valid User-Agent.
 */
object GitHubReleaseChecker {

    private const val API_URL =
        "https://api.github.com/repos/Xunzi229/openvideo/releases/latest"

    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L

    data class LatestRelease(
        val tagName: String,
        val apkBrowserDownloadUrl: String?,
        val releaseHtmlUrl: String
    )

    fun shouldRunPeriodicCheck(lastCheckEpochMs: Long, now: Long = System.currentTimeMillis()): Boolean {
        return now - lastCheckEpochMs >= CHECK_INTERVAL_MS
    }

    /**
     * @return null on network/parse failure (caller keeps previous badge state).
     */
    fun fetchLatestRelease(userAgent: String): LatestRelease? {
        val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", userAgent)
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) return null
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

    fun preferredDownloadUrl(release: LatestRelease): String =
        release.apkBrowserDownloadUrl ?: release.releaseHtmlUrl

    private fun parseLatest(jsonBody: String): LatestRelease? {
        val root = JSONObject(jsonBody)
        val tag = root.optString("tag_name").ifBlank { return null }
        val htmlUrl = root.optString("html_url").ifBlank { return null }
        var apkUrl: String? = null
        val assets = root.optJSONArray("assets") ?: return LatestRelease(tag, null, htmlUrl)
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            val name = a.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = a.optString("browser_download_url").takeIf { it.isNotBlank() }
                break
            }
        }
        return LatestRelease(tag, apkUrl, htmlUrl)
    }

    private fun stripPrefixV(s: String): String {
        val t = s.trim()
        return if (t.startsWith("v", ignoreCase = true)) t.drop(1).trim() else t
    }

    private fun parseVersionParts(s: String): List<Int> =
        s.split('.').mapNotNull { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull()
        }
}
