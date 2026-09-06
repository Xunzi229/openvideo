package com.openvideo.app.core.network

import org.junit.Assert.*
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class NetworkPolicyBoundaryTest {
    @Test fun urlWrappersPortsIpv6AndInvalidCharactersAreHandled() {
        for (wrap in listOf<Pair<String, String>>("<" to ">", "\"" to "\"", "'" to "'", "" to "")) {
            assertEquals(NetworkUrlPolicy.Validation.Valid("https://example.com:65535/a?q=1#part"),
                NetworkUrlPolicy.validatePlaybackUrl("${wrap.first}HTTPS://Example.COM:65535/a?q=1#part${wrap.second}"))
            assertEquals(WebDavConnectionPolicy.Validation.Valid("https://[::1]:8080/dav/"),
                WebDavConnectionPolicy.validateBaseUrl("${wrap.first}https://[::1]:8080/dav${wrap.second}"))
        }
        val invalid = listOf("x", "<https://a/", "\"https://a/", "'https://a/", "https://a/\u0001", "https://a/%zz", "https://a/a b")
        invalid.forEach {
            assertTrue(it, NetworkUrlPolicy.validatePlaybackUrl(it) is NetworkUrlPolicy.Validation.Invalid)
            assertTrue(it, WebDavConnectionPolicy.validateBaseUrl(it) is WebDavConnectionPolicy.Validation.Invalid)
        }
        assertEquals(NetworkUrlPolicy.Validation.Valid("http://[::1]/"), NetworkUrlPolicy.validatePlaybackUrl("http://[::1]/"))
        assertEquals(WebDavConnectionPolicy.Validation.Valid("https://example.com/"), WebDavConnectionPolicy.validateBaseUrl("https://example.com"))
        for (url in listOf("https://example.com/?q=1", "https://example.com/#part")) {
            assertEquals(WebDavConnectionPolicy.Validation.Invalid(WebDavConnectionPolicy.Error.QUERY_OR_FRAGMENT_NOT_ALLOWED), WebDavConnectionPolicy.validateBaseUrl(url))
        }
        assertEquals("dav", WebDavConnectionPolicy.displayNameFor("https://example.com/a/dav/"))
        assertEquals("example.com", WebDavConnectionPolicy.displayNameFor("https://example.com/"))
        assertEquals("/", WebDavConnectionPolicy.displayNameFor("/"))
        assertEquals(WebDavConnectionPolicy.Error.TIMEOUT, WebDavConnectionPolicy.classifyFailure(SocketTimeoutException()))
        assertEquals(WebDavConnectionPolicy.Error.CERTIFICATE_ERROR, WebDavConnectionPolicy.classifyFailure(SSLException("test")))
    }

    @Test fun recentDisplayRedactsQueryNamesWithoutDroppingFlagsOrFragments() {
        assertEquals("https://example.com/a?token&flag&x=1&KEY=redacted#part",
            NetworkRecentUrlPolicy.displayUrlFor("HTTPS://EXAMPLE.COM/a?token&flag&x=1&KEY=secret#part"))
        assertEquals("https://example.com/a?auth=redacted", NetworkRecentUrlPolicy.displayUrlFor("https://example.com/a?auth=s"))
        assertEquals("https://example.com/a", NetworkRecentUrlPolicy.displayUrlFor("https://example.com/a"))
        assertEquals("bad uri", NetworkRecentUrlPolicy.displayUrlFor(" bad uri "))
        assertEquals("bad uri", NetworkRecentUrlPolicy.titleFor(" bad uri "))
        assertEquals("example.com", NetworkRecentUrlPolicy.titleFor("https://example.com/"))
        assertEquals("urn:value", NetworkRecentUrlPolicy.titleFor("urn:value"))
        assertEquals("", NetworkRecentUrlPolicy.titleFor(""))
    }

    @Test fun sharedTextOnlyAcceptsSupportedActionsMimeTypesAndUrls() {
        for (mime in listOf(null, "text/plain", "text/html")) {
            assertEquals("https://example.com/a", NetworkSharedUrlPolicy.extractPlaybackUrl("android.intent.action.SEND", mime, "watch https://example.com/a.,;", null))
            assertNull(NetworkSharedUrlPolicy.extractPlaybackUrl("android.intent.action.SEND", mime, null, null))
        }
        assertNull(NetworkSharedUrlPolicy.extractPlaybackUrl("android.intent.action.SEND", "image/png", "https://example.com/", null))
        assertNull(NetworkSharedUrlPolicy.extractPlaybackUrl("android.intent.action.VIEW", null, null, null))
        assertNull(NetworkSharedUrlPolicy.extractPlaybackUrl("android.intent.action.VIEW", null, null, "https://user:pass@example.com/"))
        assertNull(NetworkSharedUrlPolicy.extractPlaybackUrl(null, null, null, "https://example.com/"))
    }

    @Test fun diagnosticHeadersAndRetryStatusesCoverEveryClassification() {
        val headers = mapOf("X-Token" to "a", "Secret" to "b", "Signature" to "c", "Normal" to "d")
        assertEquals(mapOf("X-Token" to "redacted", "Secret" to "redacted", "Signature" to "redacted", "Normal" to "d"), NetworkPlaybackHeaderPolicy.redactForDiagnostics(headers))
        assertEquals("OpenVideo (Android)", NetworkPlaybackHeaderPolicy.userAgent(null as String?))
        for (referer in listOf(" ", "rtsp://example.com/")) assertTrue(NetworkPlaybackHeaderPolicy.defaultRequestProperties(referer).isEmpty())
        for (status in listOf(200, 408, 425, 429, 499, 500, 599, 600)) {
            assertEquals(status in listOf(408, 425, 429, 500, 599), NetworkErrorClassifier.classifyHttpStatus(status).isRetryable)
        }
        for ((cause, expected) in listOf(UnknownHostException() to NetworkErrorClassifier.Type.DNS_FAILED,
            SocketTimeoutException() to NetworkErrorClassifier.Type.TIMEOUT, ConnectException() to NetworkErrorClassifier.Type.CONNECTION_FAILED)) {
            assertEquals(expected, NetworkErrorClassifier.classifyPlaybackError(0, IllegalStateException(cause)).type)
        }
    }

    @Test fun davParsingDropsUntrustedEntriesAndNormalizesCollectionNames() {
        val entries = WebDavDirectoryParser.parse("http://example.com:80/dav/", """
            <multistatus xmlns="DAV:">
              <response/>
              <response><href>https://example.com/dav/a.mp4</href></response>
              <response><href>http://example.com:81/dav/a.mp4</href></response>
              <response><href>http://user@example.com/dav/a.mp4</href></response>
              <response><href>http://other.com/dav/a.mp4</href></response>
              <response><href>/dav/folder</href><prop><resourcetype><collection/></resourcetype><displayname> </displayname></prop></response>
              <response><href>/dav/empty/</href></response>
              <response><href>/dav/b.mp4</href><getcontentlength>invalid</getcontentlength></response>
              <response><href>/dav/a.txt</href><displayname>Document</displayname><getcontentlength>0</getcontentlength></response>
            </multistatus>
        """.trimIndent())
        assertEquals(listOf("empty", "folder", "b.mp4", "Document"), entries.map { it.name })
        assertEquals("http://example.com:80/dav/folder/", entries[1].url)
        assertNull(entries[2].sizeBytes)
        assertEquals(0L, entries[3].sizeBytes)
        assertEquals(emptyList<WebDavDirectoryParser.Entry>(), WebDavDirectoryParser.parse("/dav/", "<multistatus/>"))
    }
}
