package com.example.openvideo.core.network

import okhttp3.Credentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class WebDavConnectionPolicyTest {

    @Test
    fun normalizesWebDavBaseUrlsWithoutCredentialsInUrl() {
        assertEquals(
            WebDavConnectionPolicy.Validation.Valid("https://example.com/dav/"),
            WebDavConnectionPolicy.validateBaseUrl("  <HTTPS://Example.COM/dav/>  ")
        )
        assertEquals(
            WebDavConnectionPolicy.Validation.Valid("http://nas.local:8080/remote.php/dav/files/me/"),
            WebDavConnectionPolicy.validateBaseUrl("http://NAS.local:8080/remote.php/dav/files/me/")
        )

        assertInvalid("", WebDavConnectionPolicy.Error.EMPTY)
        assertInvalid("example.com/dav", WebDavConnectionPolicy.Error.MISSING_SCHEME)
        assertInvalid("rtsp://example.com/dav", WebDavConnectionPolicy.Error.UNSUPPORTED_SCHEME)
        assertInvalid("https:///dav", WebDavConnectionPolicy.Error.MISSING_HOST)
        assertInvalid("https://user:pass@example.com/dav", WebDavConnectionPolicy.Error.USERINFO_NOT_ALLOWED)
        assertInvalid("https://example.com/dav?token=secret", WebDavConnectionPolicy.Error.QUERY_OR_FRAGMENT_NOT_ALLOWED)
        assertInvalid("https://example.com/dav#folder", WebDavConnectionPolicy.Error.QUERY_OR_FRAGMENT_NOT_ALLOWED)
    }

    @Test
    fun validatesCredentialsBeforeSaving() {
        assertEquals(
            WebDavConnectionPolicy.CredentialValidation.Valid,
            WebDavConnectionPolicy.validateCredentials("alice", "secret")
        )
        assertEquals(
            WebDavConnectionPolicy.CredentialValidation.Invalid(WebDavConnectionPolicy.Error.EMPTY_USERNAME),
            WebDavConnectionPolicy.validateCredentials(" ", "secret")
        )
        assertEquals(
            WebDavConnectionPolicy.CredentialValidation.Invalid(WebDavConnectionPolicy.Error.EMPTY_PASSWORD),
            WebDavConnectionPolicy.validateCredentials("alice", " ")
        )
    }

    @Test
    fun buildsDepthZeroPropfindRequestWithBasicAuthorization() {
        val request = WebDavConnectionPolicy.buildPropfindRequest(
            baseUrl = "https://example.com/dav/",
            username = "alice",
            password = "secret",
            userAgent = "OpenVideo/0.4.0 (Android)"
        )

        assertEquals("PROPFIND", request.method)
        assertEquals("0", request.header("Depth"))
        assertEquals("OpenVideo/0.4.0 (Android)", request.header("User-Agent"))
        assertEquals(Credentials.basic("alice", "secret"), request.header("Authorization"))
        assertEquals("https://example.com/dav/", request.url.toString())
        assertTrue(request.body != null)
    }

    @Test
    fun classifiesWebDavTestResponses() {
        assertEquals(WebDavConnectionPolicy.ConnectionResult.Success, WebDavConnectionPolicy.classifyHttpStatus(207))
        assertEquals(WebDavConnectionPolicy.ConnectionResult.Success, WebDavConnectionPolicy.classifyHttpStatus(200))
        assertEquals(
            WebDavConnectionPolicy.ConnectionResult.Failure(WebDavConnectionPolicy.Error.UNAUTHORIZED),
            WebDavConnectionPolicy.classifyHttpStatus(401)
        )
        assertEquals(
            WebDavConnectionPolicy.ConnectionResult.Failure(WebDavConnectionPolicy.Error.FORBIDDEN),
            WebDavConnectionPolicy.classifyHttpStatus(403)
        )
        assertEquals(
            WebDavConnectionPolicy.ConnectionResult.Failure(WebDavConnectionPolicy.Error.BAD_STATUS),
            WebDavConnectionPolicy.classifyHttpStatus(500)
        )
        assertEquals(
            WebDavConnectionPolicy.ConnectionResult.Failure(WebDavConnectionPolicy.Error.NOT_FOUND),
            WebDavConnectionPolicy.classifyHttpStatus(404)
        )
    }

    @Test
    fun classifiesWebDavTransportFailures() {
        assertEquals(
            WebDavConnectionPolicy.Error.TIMEOUT,
            WebDavConnectionPolicy.classifyFailure(SocketTimeoutException())
        )
        assertEquals(
            WebDavConnectionPolicy.Error.CERTIFICATE_ERROR,
            WebDavConnectionPolicy.classifyFailure(SSLHandshakeException("certificate"))
        )
        assertEquals(
            WebDavConnectionPolicy.Error.NETWORK_ERROR,
            WebDavConnectionPolicy.classifyFailure(IllegalStateException("boom"))
        )
    }

    private fun assertInvalid(input: String, error: WebDavConnectionPolicy.Error) {
        val result = WebDavConnectionPolicy.validateBaseUrl(input)
        assertTrue("Expected invalid result for $input, got $result", result is WebDavConnectionPolicy.Validation.Invalid)
        assertEquals(error, (result as WebDavConnectionPolicy.Validation.Invalid).error)
    }
}
