package com.example.openvideo.core.media

import android.app.Application
import android.net.Uri
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class LocalMediaUriBehaviorTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun remoteSchemesRemainIntactAndAreConsideredPlayable() {
        for (value in listOf("content://media/42", "HTTP://example.com/a?x=1#part", "https://example.com/a", "rtsp://example.com/a")) {
            assertTrue(value, LocalMediaUriPolicy.isPlayable(" $value "))
            assertEquals(Uri.parse(value), LocalMediaUriPolicy.playbackUri(" $value "))
        }
    }

    @Test fun rawFileNamesPreserveHashCharactersAndRequireAnExistingFile() {
        val file = temporary.newFile("a # clip.mp4")
        val uri = LocalMediaUriPolicy.playbackUri(file.path)
        assertEquals(file.absolutePath.replace('\\', '/'), uri.path?.replace('\\', '/'))
        assertNull(uri.query)
        assertNull(uri.fragment)
        assertTrue(LocalMediaUriPolicy.isPlayable(file.path))
        val canonical = LocalMediaUriPolicy.playbackUri("file:///Movies/video.mp4?junk=1#clip")
        assertNull(canonical.query)
        assertNull(canonical.fragment)
        assertTrue(canonical.path!!.replace('\\', '/').endsWith("/Movies/video.mp4"))
        assertFalse(LocalMediaUriPolicy.isPlayable(temporary.root.path))
        assertFalse(LocalMediaUriPolicy.isPlayable(file.path + "missing"))
        assertFalse(LocalMediaUriPolicy.isPlayable("file:///does/not/exist"))
    }

    @Test fun emptyAndPathlessInputsHaveDefinedFallbacks() {
        assertFalse(LocalMediaUriPolicy.isPlayable(" "))
        assertThrows(IllegalArgumentException::class.java) { LocalMediaUriPolicy.playbackUri(" ") }
        assertEquals(Uri.parse("file://host"), LocalMediaUriPolicy.playbackUri("file://host"))
        assertFalse(LocalMediaUriPolicy.isPlayable("file://host"))
    }
}
