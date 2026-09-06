package com.example.openvideo.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class WebDavConnectionClientTest {
    private fun client(reply: (Request) -> Response) = WebDavConnectionClient(
        OkHttpClient.Builder().addInterceptor { reply(it.request()) }.build(), WebDavMemoryCache()
    )

    private fun response(request: Request, code: Int, body: String = "") = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(code).message("test")
        .body(body.toResponseBody()).build()

    @Test fun connectionChecksSendCredentialsAndClassifyResponses() = runBlocking {
        for ((code, expected) in listOf(207 to null, 401 to WebDavConnectionPolicy.Error.UNAUTHORIZED,
            302 to WebDavConnectionPolicy.Error.REDIRECT_REJECTED, 500 to WebDavConnectionPolicy.Error.BAD_STATUS)) {
            val result = client {
                assertEquals("PROPFIND", it.method)
                assertEquals("0", it.header("Depth"))
                assertEquals("agent", it.header("User-Agent"))
                assertEquals("Basic dXNlcjpwYXNz", it.header("Authorization"))
                response(it, code)
            }.testConnection("https://example.com/dav/", " user ", "pass", "agent")
            assertEquals(expected?.let { WebDavConnectionPolicy.ConnectionResult.Failure(it) }
                ?: WebDavConnectionPolicy.ConnectionResult.Success, result)
        }
    }

    @Test fun directoryResultsAreParsedAndCached() = runBlocking {
        var requests = 0
        val client = client {
            requests++
            assertEquals("1", it.header("Depth"))
            response(it, 207, "<multistatus xmlns='DAV:'><response><href>/dav/a.mp4</href></response></multistatus>")
        }
        val first = client.listDirectory("https://example.com/dav/", "user", "pass", "agent")
            as WebDavConnectionClient.DirectoryResult.Success
        assertEquals("a.mp4", first.entries.single().name)
        assertEquals(first, client.listDirectory("https://example.com/dav/", "user", "pass", "agent"))
        assertEquals(1, requests)
    }

    @Test fun directoryRejectsStatusesMalformedXmlAndOversizedBodies() = runBlocking {
        val cases = listOf(
            Triple(403, "", WebDavConnectionPolicy.Error.FORBIDDEN),
            Triple(207, "invalid xml", WebDavConnectionPolicy.Error.INVALID_RESPONSE),
            Triple(207, "x".repeat(4 * 1024 * 1024 + 1), WebDavConnectionPolicy.Error.INVALID_RESPONSE)
        )
        for ((code, body, error) in cases) {
            val result = client { response(it, code, body) }
                .listDirectory("https://example.com/dav/", "u", "p", "agent")
            assertEquals(WebDavConnectionClient.DirectoryResult.Failure(error), result)
        }
    }

    @Test fun ioFailuresAreReturnedFromBothOperations() = runBlocking {
        val client = client { throw IOException("offline") }
        assertEquals(WebDavConnectionPolicy.ConnectionResult.Failure(WebDavConnectionPolicy.Error.NETWORK_ERROR),
            client.testConnection("https://example.com/", "u", "p", "a"))
        assertEquals(WebDavConnectionClient.DirectoryResult.Failure(WebDavConnectionPolicy.Error.NETWORK_ERROR),
            client.listDirectory("https://example.com/", "u", "p", "a"))
    }
}
