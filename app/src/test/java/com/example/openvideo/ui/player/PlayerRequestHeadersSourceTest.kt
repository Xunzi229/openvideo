package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerRequestHeadersSourceTest {

    @Test
    fun activityReadsIntentRequestHeadersAndPassesThemToViewModel() {
        val source = playerSource("PlayerActivity.kt")
        val onCreate = source.substringAfter("override fun onCreate(savedInstanceState: Bundle?) {")
            .substringBefore("\n    private fun applyPlayerSettings()")

        assertTrue(onCreate.contains("val requestHeaders = PlayerActivityIntents.requestHeaders(intent)"))
        assertTrue(onCreate.contains("val externalSubtitleUri = PlayerActivityIntents.externalSubtitleUri(intent)"))
        assertTrue(onCreate.contains("requestHeaders = requestHeaders"))
        assertFalse(onCreate.contains("playbackCoordinator.snapshot?.requestHeaders"))
    }

    @Test
    fun viewModelStoresRequestHeadersAndReusesThemForRetry() {
        val source = playerSource("PlayerViewModel.kt")
        val initialize = source.substringAfter("fun initialize(uri: Uri, title: String, id: Long, path: String = \"\", requestHeaders: Map<String, String> = emptyMap()) {")
            .substringBefore("\n    fun restorePosition")
        val retry = source.substringAfter("fun retryPlayback(resetAutoRetry: Boolean = true) {")
            .substringBefore("\n    fun handleNetworkAutoRetry")

        assertTrue(source.contains("private var requestHeaders: Map<String, String> = emptyMap()"))
        assertTrue(initialize.contains("this.requestHeaders = requestHeaders"))
        assertTrue(initialize.contains("playerManager.setMediaUri(uri, requestHeaders)"))
        assertTrue(retry.contains("playerManager.setMediaUri(uri, requestHeaders)"))
        assertTrue(source.contains("playerManager.setMediaUri(uri, emptyMap())"))
        assertTrue(source.contains("playerManager.setMediaUri(item.uri, emptyMap())"))
    }

    @Test
    fun viewModelUsesCurrentRequestHeadersWhenLoadingNetworkSubtitles() {
        val source = playerSource("PlayerViewModel.kt")
        val loadSubtitles = source.substringAfter("fun loadSubtitles(")
            .substringBefore("\n    fun getCurrentSubtitle()")

        assertTrue(loadSubtitles.contains("PlayerSubtitleLoadCoordinator.load("))
        assertTrue(loadSubtitles.contains("requestHeaders = requestHeaders"))
    }

    @Test
    fun activityPrefersIntentSubtitleUriBeforeVideoUriFallback() {
        val source = playerSource("PlayerActivity.kt")
        val onCreate = source.substringAfter("override fun onCreate(savedInstanceState: Bundle?) {")
            .substringBefore("\n    private fun applyPlayerSettings()")

        assertTrue(onCreate.contains("playerPrefs.externalSubtitleUri.ifBlank { externalSubtitleUri.ifBlank { uriString } }"))
    }

    private fun playerSource(name: String): String = loadText(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", name)
    )

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
