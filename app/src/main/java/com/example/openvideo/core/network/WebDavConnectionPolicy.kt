package com.example.openvideo.core.network

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.net.URISyntaxException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

object WebDavConnectionPolicy {

    sealed class Validation {
        data class Valid(val normalizedBaseUrl: String) : Validation()
        data class Invalid(val error: Error) : Validation()
    }

    sealed class CredentialValidation {
        data object Valid : CredentialValidation()
        data class Invalid(val error: Error) : CredentialValidation()
    }

    sealed class ConnectionResult {
        data object Success : ConnectionResult()
        data class Failure(val error: Error) : ConnectionResult()
    }

    enum class Error {
        EMPTY,
        MISSING_SCHEME,
        UNSUPPORTED_SCHEME,
        MISSING_HOST,
        USERINFO_NOT_ALLOWED,
        QUERY_OR_FRAGMENT_NOT_ALLOWED,
        ILLEGAL_CHARACTER,
        EMPTY_USERNAME,
        EMPTY_PASSWORD,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        TIMEOUT,
        CERTIFICATE_ERROR,
        BAD_STATUS,
        NETWORK_ERROR
    }

    fun validateBaseUrl(input: String): Validation {
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
        if (scheme !in setOf("http", "https")) return Validation.Invalid(Error.UNSUPPORTED_SCHEME)
        val host = uri.host?.lowercase() ?: return Validation.Invalid(Error.MISSING_HOST)
        if (uri.userInfo != null) return Validation.Invalid(Error.USERINFO_NOT_ALLOWED)
        if (uri.rawQuery != null || uri.rawFragment != null) {
            return Validation.Invalid(Error.QUERY_OR_FRAGMENT_NOT_ALLOWED)
        }
        val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
        val normalizedPath = if (path.endsWith('/')) path else "$path/"
        val authority = buildString {
            append(host)
            if (uri.port >= 0) append(":${uri.port}")
        }
        return Validation.Valid("$scheme://$authority$normalizedPath")
    }

    fun validateCredentials(username: String, password: String): CredentialValidation {
        if (username.trim().isBlank()) return CredentialValidation.Invalid(Error.EMPTY_USERNAME)
        if (password.isBlank()) return CredentialValidation.Invalid(Error.EMPTY_PASSWORD)
        return CredentialValidation.Valid
    }

    fun displayNameFor(normalizedBaseUrl: String): String {
        val uri = URI(normalizedBaseUrl)
        val path = uri.path.trim('/').substringAfterLast('/', missingDelimiterValue = "")
        return path.ifBlank { uri.host.orEmpty() }.ifBlank { normalizedBaseUrl }
    }

    fun buildPropfindRequest(
        baseUrl: String,
        username: String,
        password: String,
        userAgent: String,
        depth: String = "0"
    ): Request {
        val body = PROPFIND_BODY.toRequestBody("application/xml; charset=utf-8".toMediaType())
        return Request.Builder()
            .url(baseUrl)
            .method("PROPFIND", body)
            .header("Depth", depth)
            .header("User-Agent", userAgent)
            .header("Authorization", Credentials.basic(username.trim(), password))
            .build()
    }

    fun classifyHttpStatus(statusCode: Int): ConnectionResult =
        when (statusCode) {
            200, 207 -> ConnectionResult.Success
            401 -> ConnectionResult.Failure(Error.UNAUTHORIZED)
            403 -> ConnectionResult.Failure(Error.FORBIDDEN)
            404 -> ConnectionResult.Failure(Error.NOT_FOUND)
            else -> ConnectionResult.Failure(Error.BAD_STATUS)
        }

    fun classifyFailure(error: Throwable): Error =
        when (error) {
            is SocketTimeoutException -> Error.TIMEOUT
            is SSLException -> Error.CERTIFICATE_ERROR
            else -> Error.NETWORK_ERROR
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

    private const val PROPFIND_BODY =
        """<?xml version="1.0" encoding="utf-8"?><propfind xmlns="DAV:"><prop><resourcetype/><displayname/></prop></propfind>"""
}
