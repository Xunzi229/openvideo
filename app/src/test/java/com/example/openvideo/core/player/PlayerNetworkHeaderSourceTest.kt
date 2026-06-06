package com.example.openvideo.core.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerNetworkHeaderSourceTest {

    @Test
    fun playerManagerUsesHttpDataSourceWithOpenVideoUserAgent() {
        val source = playerManagerSource()
        val initializeBlock = source.substringAfter("fun initialize(mediaUri: Uri? = null): ExoPlayer {")
            .substringBefore("\n    fun release()")

        assertTrue(source.contains("import androidx.media3.datasource.DefaultDataSource"))
        assertTrue(source.contains("import androidx.media3.datasource.DefaultHttpDataSource"))
        assertTrue(source.contains("import androidx.media3.exoplayer.source.DefaultMediaSourceFactory"))
        assertTrue(source.contains("import com.example.openvideo.core.network.NetworkPlaybackHeaderPolicy"))
        assertTrue(initializeBlock.contains("DefaultHttpDataSource.Factory()"))
        assertTrue(initializeBlock.contains("setUserAgent(NetworkPlaybackHeaderPolicy.userAgent(context))"))
        assertTrue(initializeBlock.contains("setDefaultRequestProperties(NetworkPlaybackHeaderPolicy.defaultRequestProperties())"))
        assertTrue(initializeBlock.contains("DefaultDataSource.Factory(context, httpDataSourceFactory)"))
        assertTrue(initializeBlock.contains("DefaultMediaSourceFactory(dataSourceFactory)"))
        assertTrue(initializeBlock.contains("setMediaSourceFactory(mediaSourceFactory)"))
    }

    @Test
    fun playerManagerCanApplyPerPlaybackRequestHeaders() {
        val source = playerManagerSource()
        val setMediaUriBlock = source.substringAfter("fun setMediaUri(uri: Uri, requestHeaders: Map<String, String> = emptyMap()) {")
            .substringBefore("\n    fun togglePlayPause()")

        assertTrue(source.contains("private var httpDataSourceFactory: DefaultHttpDataSource.Factory? = null"))
        assertTrue(source.contains("this.httpDataSourceFactory = httpDataSourceFactory"))
        assertTrue(setMediaUriBlock.contains("NetworkPlaybackHeaderPolicy.defaultRequestProperties() + requestHeaders"))
        assertTrue(setMediaUriBlock.contains("httpDataSourceFactory?.setDefaultRequestProperties("))
        assertTrue(setMediaUriBlock.contains("MediaItem.fromUri(uri)"))
        assertTrue(setMediaUriBlock.contains("playWhenReady = true"))
        assertTrue(setMediaUriBlock.contains("prepare()"))
    }

    private fun playerManagerSource(): String = loadText(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "core", "player", "PlayerManager.kt")
    )

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
