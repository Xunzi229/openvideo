package com.example.openvideo.core.network

import android.content.Context
import java.net.URI

object NetworkPlaybackHeaderPolicy {
    private val sensitiveHeaderNames = listOf("authorization", "cookie", "token", "key", "secret", "sign")

    fun userAgent(context: Context): String {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        return userAgent(versionName)
    }

    fun userAgent(versionName: String?): String {
        val version = versionName?.trim().orEmpty()
        return if (version.isBlank()) {
            "OpenVideo (Android)"
        } else {
            "OpenVideo/$version (Android)"
        }
    }

    fun defaultRequestProperties(referer: String? = null): Map<String, String> {
        val normalizedReferer = normalizeReferer(referer) ?: return emptyMap()
        return mapOf("Referer" to normalizedReferer)
    }

    fun redactForDiagnostics(headers: Map<String, String>): Map<String, String> {
        return headers.mapValues { (name, value) ->
            if (isSensitiveHeaderName(name)) "redacted" else value
        }
    }

    private fun normalizeReferer(referer: String?): String? {
        val value = referer?.trim().orEmpty()
        if (value.isBlank()) return null
        val validation = NetworkUrlPolicy.validatePlaybackUrl(value)
        if (validation !is NetworkUrlPolicy.Validation.Valid) return null
        val uri = runCatching { URI(validation.normalizedUrl) }.getOrNull() ?: return null
        if (uri.scheme !in setOf("http", "https")) return null
        if (uri.userInfo != null) return null
        return validation.normalizedUrl
    }

    private fun isSensitiveHeaderName(name: String): Boolean {
        val lower = name.lowercase()
        return sensitiveHeaderNames.any { lower.contains(it) }
    }
}
