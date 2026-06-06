package com.example.openvideo.core.network

import java.net.URI
import java.net.URISyntaxException

object NetworkUrlPolicy {

    private val supportedSchemes = setOf("http", "https", "rtsp")

    sealed class Validation {
        data class Valid(val normalizedUrl: String) : Validation()
        data class Invalid(val error: Error) : Validation()
    }

    enum class Error {
        EMPTY,
        MISSING_SCHEME,
        UNSUPPORTED_SCHEME,
        MISSING_HOST,
        ILLEGAL_CHARACTER
    }

    fun validatePlaybackUrl(input: String): Validation {
        val cleaned = stripPasteWrappers(input.trim())
        if (cleaned.isEmpty()) return Validation.Invalid(Error.EMPTY)
        if (cleaned.any { it.isWhitespace() || it.isISOControl() }) {
            return Validation.Invalid(Error.ILLEGAL_CHARACTER)
        }

        val uri = try {
            URI(cleaned)
        } catch (_: URISyntaxException) {
            return Validation.Invalid(Error.ILLEGAL_CHARACTER)
        }

        val scheme = uri.scheme?.lowercase() ?: return Validation.Invalid(Error.MISSING_SCHEME)
        if (scheme !in supportedSchemes) return Validation.Invalid(Error.UNSUPPORTED_SCHEME)

        val host = uri.host?.lowercase() ?: return Validation.Invalid(Error.MISSING_HOST)
        val authority = buildString {
            uri.userInfo?.let {
                append(it)
                append('@')
            }
            append(host)
            if (uri.port >= 0) {
                append(':')
                append(uri.port)
            }
        }
        return Validation.Valid(rebuildUrl(scheme, authority, uri))
    }

    private fun stripPasteWrappers(value: String): String {
        if (value.length < 2) return value
        return when {
            value.first() == '<' && value.last() == '>' -> value.substring(1, value.lastIndex).trim()
            value.first() == '"' && value.last() == '"' -> value.substring(1, value.lastIndex).trim()
            value.first() == '\'' && value.last() == '\'' -> value.substring(1, value.lastIndex).trim()
            else -> value
        }
    }

    private fun rebuildUrl(scheme: String, authority: String, uri: URI): String {
        return buildString {
            append(scheme)
            append("://")
            append(authority)
            uri.rawPath?.let(::append)
            uri.rawQuery?.let {
                append('?')
                append(it)
            }
            uri.rawFragment?.let {
                append('#')
                append(it)
            }
        }
    }
}
