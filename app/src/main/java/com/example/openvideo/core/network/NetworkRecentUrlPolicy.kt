package com.example.openvideo.core.network

import java.net.URI

object NetworkRecentUrlPolicy {
    private val sensitiveQueryNames = listOf("token", "key", "sign", "auth")

    fun displayUrlFor(url: String): String {
        val normalizedUrl = when (val validation = NetworkUrlPolicy.validatePlaybackUrl(url)) {
            is NetworkUrlPolicy.Validation.Valid -> validation.normalizedUrl
            is NetworkUrlPolicy.Validation.Invalid -> url.trim()
        }
        val uri = runCatching { URI(normalizedUrl) }.getOrNull() ?: return normalizedUrl
        val rawQuery = uri.rawQuery ?: return normalizedUrl
        val redactedQuery = rawQuery.split('&').joinToString("&") { part ->
            val name = part.substringBefore('=', missingDelimiterValue = part)
            val value = part.substringAfter('=', missingDelimiterValue = "")
            if (isSensitiveQueryName(name)) {
                if (part.contains('=')) "$name=redacted" else name
            } else if (part.contains('=')) {
                "$name=$value"
            } else {
                part
            }
        }
        return normalizedUrl.substringBefore('?') + "?" + redactedQuery +
            uri.rawFragment.orEmpty().takeIf { it.isNotBlank() }?.let { "#$it" }.orEmpty()
    }

    fun titleFor(url: String): String {
        val normalizedUrl = when (val validation = NetworkUrlPolicy.validatePlaybackUrl(url)) {
            is NetworkUrlPolicy.Validation.Valid -> validation.normalizedUrl
            is NetworkUrlPolicy.Validation.Invalid -> url.trim()
        }
        val uri = runCatching { URI(normalizedUrl) }.getOrNull() ?: return normalizedUrl
        return uri.path
            ?.trimEnd('/')
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: uri.host
            ?: normalizedUrl
    }

    private fun isSensitiveQueryName(name: String): Boolean {
        val lower = name.lowercase()
        return sensitiveQueryNames.any { lower.contains(it) }
    }
}
