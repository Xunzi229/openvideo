package com.example.openvideo.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavDirectoryParserTest {

    @Test
    fun parsesMultiStatusIntoDirectoriesAndPlayableVideos() {
        val entries = WebDavDirectoryParser.parse(
            baseUrl = "https://example.com/dav/",
            xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <d:multistatus xmlns:d="DAV:">
                  <d:response>
                    <d:href>/dav/</d:href>
                    <d:propstat><d:prop><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat>
                  </d:response>
                  <d:response>
                    <d:href>/dav/Movies/</d:href>
                    <d:propstat><d:prop><d:displayname>Movies</d:displayname><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat>
                  </d:response>
                  <d:response>
                    <d:href>/dav/video.mp4</d:href>
                    <d:propstat><d:prop><d:getcontentlength>1048576</d:getcontentlength><d:resourcetype /></d:prop></d:propstat>
                  </d:response>
                  <d:response>
                    <d:href>/dav/notes.txt</d:href>
                    <d:propstat><d:prop><d:getcontentlength>12</d:getcontentlength><d:resourcetype /></d:prop></d:propstat>
                  </d:response>
                </d:multistatus>
            """.trimIndent()
        )

        assertEquals(3, entries.size)
        assertEquals("Movies", entries[0].name)
        assertTrue(entries[0].isDirectory)
        assertEquals("https://example.com/dav/Movies/", entries[0].url)

        assertEquals("video.mp4", entries[1].name)
        assertFalse(entries[1].isDirectory)
        assertTrue(entries[1].isPlayableVideo)
        assertEquals(1_048_576L, entries[1].sizeBytes)

        assertEquals("notes.txt", entries[2].name)
        assertFalse(entries[2].isPlayableVideo)
    }

    @Test
    fun buildsDepthOneDirectoryRequestWithoutLeakingCredentialsInUrl() {
        val request = WebDavConnectionPolicy.buildPropfindRequest(
            baseUrl = "https://example.com/dav/",
            username = "alice",
            password = "secret",
            userAgent = "OpenVideo/0.4.0 (Android)",
            depth = "1"
        )

        assertEquals("PROPFIND", request.method)
        assertEquals("1", request.header("Depth"))
        assertEquals("https://example.com/dav/", request.url.toString())
        assertFalse(request.url.toString().contains("alice"))
        assertFalse(request.url.toString().contains("secret"))
    }
}
