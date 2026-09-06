package com.openvideo.app.core.subtitle

import android.app.Application
import android.net.Uri
import com.openvideo.app.core.network.WebDavMemoryCache
import com.openvideo.app.core.prefs.PlayerPrefs
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class SubtitleLoaderTest {
    @get:Rule val temporary = TemporaryFolder()
    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = PlayerPrefs(context)
    private val srt = "1\n00:00:01,000 --> 00:00:02,000\nhello\n"
    private val vtt = "WEBVTT\n\n00:01.000 --> 00:02.000\nhello\n"
    private val ass = "[Events]\nDialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,hello\n"
    private fun loader(client: OkHttpClient = OkHttpClient()) = SubtitleLoader(context, prefs, client, WebDavMemoryCache())

    @Test fun fileAndUriLoadingDispatchEverySupportedFormat() {
        prefs.subtitleEncoding = "auto"
        val loader = loader()
        for ((extension, text) in listOf("srt" to srt, "vtt" to vtt, "ass" to ass, "ssa" to ass)) {
            val file = temporary.newFile("subtitle.$extension").apply { writeText(text) }
            for (items in listOf(loader.loadFromFile(file), loader.loadFromUri(Uri.fromFile(file)))) {
                assertEquals(extension, "hello", items.single().text)
                assertEquals(1000L, items.single().startTimeMs)
            }
        }
        val unknown = temporary.newFile("subtitle.txt").apply { writeText(srt) }
        assertTrue(loader.loadFromFile(unknown).isEmpty())
        assertEquals("hello", loader.loadFromUri(Uri.fromFile(unknown)).single().text)
        val extensionless = temporary.newFile("subtitle").apply { writeText(srt) }
        assertEquals("hello", loader.loadFromUri(Uri.fromFile(extensionless)).single().text)
        assertTrue(loader.loadFromFile(File(temporary.root, "missing.srt")).isEmpty())
        assertTrue(loader.loadFromUri(Uri.parse("content://missing/subtitle")).isEmpty())
    }

    @Test fun explicitAndInvalidEncodingsUseExpectedFallback() {
        val file = temporary.newFile("encoded.srt").apply { writeText(srt) }
        for (encoding in listOf("UTF-8", "invalid-charset")) {
            prefs.subtitleEncoding = encoding
            assertEquals("hello", loader().loadFromFile(file).single().text)
            assertEquals("hello", loader().loadFromUri(Uri.fromFile(file)).single().text)
        }
    }

    @Test fun networkLoadingSendsValidHeadersCachesSuccessAndHandlesErrors() {
        var requests = 0
        var status = 200
        var body = srt
        val client = OkHttpClient.Builder().addInterceptor {
            requests++
            assertEquals("Bearer value", it.request().header("Authorization"))
            assertNull(it.request().header("Empty"))
            Response.Builder().request(it.request()).protocol(Protocol.HTTP_1_1)
                .code(status).message("test").body(body.toResponseBody()).build()
        }.build()
        val loader = loader(client)
        val headers = mapOf("Authorization" to "Bearer value", "" to "ignored", "Empty" to "")
        for ((extension, text) in listOf("srt" to srt, "vtt" to vtt, "ass" to ass, "ssa" to ass, "" to srt)) {
            body = text
            val url = "https://example.com/subtitle${if (extension.isEmpty()) "" else ".$extension"}?token=1#part"
            assertEquals("hello", loader.loadFromNetworkUrl(url, headers).single().text)
            val count = requests
            assertEquals("hello", loader.loadFromNetworkUrl(url, headers).single().text)
            assertEquals(count, requests)
        }
        status = 404
        assertTrue(loader.loadFromNetworkUrl("https://example.com/missing.srt", headers).isEmpty())
        assertTrue(loader.loadFromNetworkUrl("invalid URL").isEmpty())
        val failed = loader(OkHttpClient.Builder().addInterceptor { throw IOException("offline") }.build())
        assertTrue(failed.loadFromNetworkUrl("https://example.com/a.srt").isEmpty())
    }

    @Test fun sidecarDiscoveryRequiresExistingVideoAndReturnsMatchingFiles() {
        val video = temporary.newFile("movie.mp4")
        val subtitle = temporary.newFile("movie.srt")
        temporary.newFile("other.srt")
        assertEquals(listOf(subtitle.path), loader().findSubtitleCandidates(video.path).map { it.path })
        assertEquals(listOf(subtitle), loader().findSubtitleFiles(video.path))
        assertTrue(loader().findSubtitleCandidates(File(temporary.root, "absent.mp4").path).isEmpty())
    }
}
