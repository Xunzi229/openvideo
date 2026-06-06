package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerActivityIntentsSourceTest {

    @Test
    fun networkPlaybackIntentCarriesPlayerExtras() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerActivityIntents.kt")
        )
        val block = source.substringAfter("fun networkPlayback(")

        assertTrue(block.contains("Intent(context, PlayerActivity::class.java)"))
        assertTrue(block.contains("NetworkRecentUrlPolicy.titleFor(normalizedUrl)"))
        assertTrue(block.contains("putExtra(\"video_uri\", normalizedUrl)"))
        assertTrue(block.contains("putExtra(\"video_title\", title)"))
        assertTrue(block.contains("putExtra(\"video_id\", normalizedUrl.hashCode().toLong())"))
        assertTrue(block.contains("putExtra(\"video_path\", normalizedUrl)"))
    }

    @Test
    fun networkPlaybackIntentCarriesSanitizedRequestHeadersSeparatelyFromUrl() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerActivityIntents.kt")
        )
        val block = source.substringAfter("fun networkPlayback(")

        assertTrue(block.contains("requestHeaders: Map<String, String> = emptyMap()"))
        assertTrue(source.contains("const val EXTRA_REQUEST_HEADERS"))
        assertTrue(source.contains("fun requestHeaders(intent: Intent): Map<String, String>"))
        assertTrue(source.contains("Bundle().apply"))
        assertTrue(source.contains("putString(name.trim(), value.trim())"))
        assertTrue(source.contains("putExtra("))
        assertTrue(source.contains("EXTRA_REQUEST_HEADERS"))
        assertTrue(source.contains("key.isNotBlank() && value.isNotBlank()"))
        assertFalse("Request headers must not be appended to the playback URL", block.contains("Authorization\" + normalizedUrl"))
        assertFalse("Request headers must not be embedded as query params", block.contains("normalizedUrl + \"?\""))
    }

    @Test
    fun networkPlaybackIntentCarriesOptionalExternalSubtitleUri() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerActivityIntents.kt")
        )

        assertTrue(source.contains("const val EXTRA_EXTERNAL_SUBTITLE_URI"))
        assertTrue(source.contains("externalSubtitleUri: String? = null"))
        assertTrue(source.contains("putExtra(EXTRA_EXTERNAL_SUBTITLE_URI, externalSubtitleUri.trim())"))
        assertTrue(source.contains("fun externalSubtitleUri(intent: Intent): String"))
        assertTrue(source.contains("intent.getStringExtra(EXTRA_EXTERNAL_SUBTITLE_URI)?.trim().orEmpty()"))
    }

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
