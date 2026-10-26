package com.openvideo.app.core.network

import android.app.Application
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import com.openvideo.app.di.AppModule
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 29, 33], application = Application::class)
class AndroidHttpCompatibilityTest {
    @Test fun packagedNetworkPolicyAllowsUserProvidedHttpAddresses() {
        val app = RuntimeEnvironment.getApplication()
        assertTrue(app.applicationInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC != 0)
        assertTrue(NetworkUrlPolicy.validatePlaybackUrl("http://192.168.1.2/video.mp4") is NetworkUrlPolicy.Validation.Valid)
        assertTrue(NetworkUrlPolicy.validatePlaybackUrl("https://example.com/video.mp4") is NetworkUrlPolicy.Validation.Valid)
    }

    @Test fun media3CanReadHttpVideoData() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("video-data"))
            server.start()
            val source = DefaultHttpDataSource.Factory().createDataSource()
            try {
                source.open(DataSpec(Uri.parse(server.url("/video.mp4").toString())))
                val buffer = ByteArray(32)
                val size = source.read(buffer, 0, buffer.size)
                assertEquals("video-data", String(buffer, 0, size))
                assertEquals("GET", server.takeRequest(5, TimeUnit.SECONDS)?.method)
            } finally { source.close() }
        }
    }

    @Test fun webDavCanListAnHttpServer() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(207).setBody(
                "<multistatus xmlns='DAV:'><response><href>/dav/a.mp4</href></response></multistatus>"
            ))
            server.start()
            val client = WebDavConnectionClient(AppModule.provideOkHttpClient(), WebDavMemoryCache())
            val result = client.listDirectory(server.url("/dav/").toString(), "", "", "OpenVideo-Test")
            assertEquals("a.mp4", (result as WebDavConnectionClient.DirectoryResult.Success).entries.single().name)
            val request = server.takeRequest(5, TimeUnit.SECONDS)
            assertEquals("PROPFIND", request?.method)
            assertEquals("1", request?.getHeader("Depth"))
        }
    }
}
