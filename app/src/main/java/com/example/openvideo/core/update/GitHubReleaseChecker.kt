package com.example.openvideo.core.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches [GitHub latest release API](https://docs.github.com/en/rest/releases/releases#get-the-latest-release).
 * Picks ABI-specific APK when multiple assets exist; supports [.sha256] sidecar files or SHA256SUMS.
 */
object GitHubReleaseChecker {

    private const val API_URL =
        "https://api.github.com/repos/Xunzi229/openvideo/releases/latest"

    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L

    /** Common ABI markers in split APK filenames (order matters for substring checks — use full ABI strings). */
    private val KNOWN_ABI_MARKERS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

    data class ReleaseAsset(val name: String, val browserDownloadUrl: String)

    data class LatestRelease(
        val tagName: String,
        val releaseHtmlUrl: String,
        val assets: List<ReleaseAsset>
    )

    fun shouldRunPeriodicCheck(lastCheckEpochMs: Long, now: Long = System.currentTimeMillis()): Boolean {
        return now - lastCheckEpochMs >= CHECK_INTERVAL_MS
    }

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

    /** Prefer ABI-specific APK URL for this device; fallback release page. */
    fun preferredDownloadUrl(release: LatestRelease, supportedAbis: Array<String>): String {
        val apk = selectApkForAbi(release.assets, supportedAbis)
        return apk?.browserDownloadUrl ?: release.releaseHtmlUrl
    }

    fun selectApkForAbi(assets: List<ReleaseAsset>, supportedAbis: Array<String>): ReleaseAsset? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apks.isEmpty()) return null
        if (apks.size == 1) return apks.first()

        fun apkNamesAbiTagged(name: String): Boolean =
            KNOWN_ABI_MARKERS.any { marker -> name.contains(marker, ignoreCase = true) }

        val abiTagged = apks.filter { apkNamesAbiTagged(it.name) }
        val universal = apks.filter { !apkNamesAbiTagged(it.name) }

        for (abi in supportedAbis) {
            val hit = abiTagged.find { asset ->
                asset.name.contains(abi, ignoreCase = true)
            }
            if (hit != null) return hit
        }
        return universal.firstOrNull() ?: apks.first()
    }

    /**
     * Expected SHA-256 as 64 hex chars, or null if no checksum asset is listed.
     * [fetchShaSidecarText] must load remote small files when needed.
     */
    fun resolveExpectedSha256Hex(
        assets: List<ReleaseAsset>,
        selectedApk: ReleaseAsset,
        fetchText: (String) -> String?
    ): String? {
        val apkName = selectedApk.name
        val companionExact = listOf("$apkName.sha256", "$apkName.sha256.txt")
        for (candidate in companionExact) {
            val asset = assets.find { it.name.equals(candidate, ignoreCase = true) } ?: continue
            val text = fetchText(asset.browserDownloadUrl) ?: continue
            parseSingleHexLine(text)?.let { return it }
        }
        val sumsNames = listOf("SHA256SUMS", "SHA256SUMS.txt", "checksums.sha256", "checksums.txt")
        for (sumsName in sumsNames) {
            val sumsAsset = assets.find { it.name.equals(sumsName, ignoreCase = true) } ?: continue
            val text = fetchText(sumsAsset.browserDownloadUrl) ?: continue
            parseSha256SumLineForFile(text, apkName)?.let { return it }
        }
        return null
    }

    fun fetchUrlText(url: String, userAgent: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", userAgent)
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            if (code !in 200..299) return null
            stream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    fun normalizeSha256Hex(raw: String): String? {
        val hex = raw.trim().lowercase().filter { it in '0'..'9' || it in 'a'..'f' }
        return hex.takeIf { it.length == 64 }
    }

    fun parseSingleHexLine(text: String): String? {
        val line = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return null
        val token = line.trim().split(Regex("\\s+")).firstOrNull() ?: return null
        return normalizeSha256Hex(token)
    }

    /** Parses GNU sha256sum-style lines: `hash *filename` or `hash  filename`. */
    fun parseSha256SumLineForFile(fullText: String, apkFileName: String): String? {
        val target = apkFileName.trim()
        for (line in fullText.lines()) {
            if (line.isBlank()) continue
            if (!line.contains(target, ignoreCase = false)) continue
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.isEmpty()) continue
            normalizeSha256Hex(parts[0])?.let { return it }
        }
        return null
    }

    private fun parseLatest(jsonBody: String): LatestRelease? {
        val root = JSONObject(jsonBody)
        val tag = root.optString("tag_name").ifBlank { return null }
        val htmlUrl = root.optString("html_url").ifBlank { return null }
        val arr = root.optJSONArray("assets") ?: return LatestRelease(tag, htmlUrl, emptyList())
        val list = ArrayList<ReleaseAsset>()
        for (i in 0 until arr.length()) {
            val a = arr.optJSONObject(i) ?: continue
            val name = a.optString("name")
            if (name.isBlank()) continue
            val url = a.optString("browser_download_url")
            if (url.isBlank()) continue
            list.add(ReleaseAsset(name = name, browserDownloadUrl = url))
        }
        return LatestRelease(tagName = tag, releaseHtmlUrl = htmlUrl, assets = list)
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
