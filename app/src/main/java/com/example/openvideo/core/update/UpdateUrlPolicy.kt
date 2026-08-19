package com.example.openvideo.core.update

import java.net.URI

/** Limits self-update traffic to the project's GitHub release infrastructure. */
object UpdateUrlPolicy {
    private const val GITHUB_HOST = "github.com"
    private const val RELEASE_PATH_PREFIX = "/Xunzi229/openvideo/releases/download/"
    private val redirectHosts = setOf(
        GITHUB_HOST,
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )

    fun isTrustedReleasePage(url: String): Boolean {
        val uri = parseHttps(url) ?: return false
        return uri.host.equals(GITHUB_HOST, ignoreCase = true) &&
            uri.rawPath.orEmpty().startsWith("/Xunzi229/openvideo/releases/")
    }

    fun isTrustedReleaseAsset(url: String): Boolean {
        val uri = parseHttps(url) ?: return false
        return uri.host.equals(GITHUB_HOST, ignoreCase = true) &&
            uri.rawPath.orEmpty().startsWith(RELEASE_PATH_PREFIX)
    }

    fun isTrustedRedirect(url: String): Boolean {
        val uri = parseHttps(url) ?: return false
        return uri.host.lowercase() in redirectHosts
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
