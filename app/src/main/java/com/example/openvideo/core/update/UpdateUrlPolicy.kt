package com.example.openvideo.core.update

import java.net.URI

/** Limits update navigation to the project's HTTPS GitHub Release pages. */
object UpdateUrlPolicy {
    private const val GITHUB_HOST = "github.com"
    private const val RELEASE_PATH_PREFIX = "/Xunzi229/openvideo/releases/tag/"

    fun isTrustedReleasePage(url: String): Boolean {
        val uri = parseHttps(url) ?: return false
        if (!uri.host.equals(GITHUB_HOST, ignoreCase = true)) return false
        val path = uri.rawPath.orEmpty()
        if (!path.startsWith(RELEASE_PATH_PREFIX)) return false
        val tag = path.removePrefix(RELEASE_PATH_PREFIX)
        return tag.isNotBlank() && '/' !in tag
    }

    private fun parseHttps(url: String): URI? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.host.isNullOrBlank() || uri.userInfo != null) return null
        if (uri.port !in setOf(-1, 443)) return null
        if (uri.rawFragment != null) return null
        return uri
    }
}
