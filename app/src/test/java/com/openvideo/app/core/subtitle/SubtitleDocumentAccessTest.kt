package com.openvideo.app.core.subtitle

import android.app.Application
import android.net.Uri
import android.content.ContentResolver
import com.openvideo.app.core.network.WebDavMemoryCache
import com.openvideo.app.core.prefs.PlayerPrefs
import com.openvideo.app.ui.player.PlayerSubtitleLoadCoordinator
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, shadows = [TemporaryGrantResolver::class])
class SubtitleDocumentAccessTest {
    @get:Rule val temporary = TemporaryFolder()
    private val srt = "1\n00:00:01,000 --> 00:00:02,000\nhello\n"

    @Test fun documentGrantIsRetainedWithoutChangingTheUri() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://persistent/123")
        assertEquals(uri, SubtitleDocumentAccess.retain(context, uri))
        assertTrue(context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission })
    }

    @Test fun providerWithTemporaryGrantGetsBoundedReusablePrivateCopy() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://temporary/123")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { srt.byteInputStream() }
        val retained = SubtitleDocumentAccess.retain(context, uri)!!
        assertEquals("file", retained.scheme)
        assertEquals(retained, SubtitleDocumentAccess.retain(context, uri))
        assertEquals(srt, java.io.File(retained.path!!).readText())
        assertEquals(srt, context.contentResolver.openInputStream(retained)!!.bufferedReader().use { it.readText() })
        val prefs = PlayerPrefs(context).apply { subtitleEncoding = "auto" }
        val loader = SubtitleLoader(context, prefs, OkHttpClient(), WebDavMemoryCache())
        // Use the FileUri directly: Android Uri serialization cannot round-trip
        // Windows host backslashes used by Robolectric's private files directory.
        val subtitles = loader.loadFromUri(retained)
        assertEquals("hello", subtitles.single().text)
    }

    @Test fun opaqueDocumentCanLoadWithoutExtensionOrDisplayName() {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://temporary/456")
        shadowOf(context.contentResolver).registerInputStream(uri,
            "WEBVTT\n\n00:01.000 --> 00:02.000\nhello\n".byteInputStream())
        val loader = SubtitleLoader(context, PlayerPrefs(context), OkHttpClient(), WebDavMemoryCache())
        assertEquals("hello", PlayerSubtitleLoadCoordinator.load(uri.toString(), "/local/video.mp4", loader,
            explicitSubtitle = true).single().text)
    }

    @Test fun oversizedImportLeavesNoPartialPrivateFile() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://temporary/large")
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArray(SubtitleInput.MAX_BYTES + 1).inputStream())
        assertNull(SubtitleDocumentAccess.retain(context, uri))
        val directory = java.io.File(context.filesDir, "imported_subtitles")
        assertTrue(directory.listFiles().orEmpty().none { it.extension == "tmp" })
    }
}

@Implements(ContentResolver::class)
class TemporaryGrantResolver : ShadowContentResolver() {
    @Implementation
    public override fun takePersistableUriPermission(uri: Uri, flags: Int) {
        if (uri.authority == "temporary") throw SecurityException("Temporary grant only")
        super.takePersistableUriPermission(uri, flags)
    }
}
