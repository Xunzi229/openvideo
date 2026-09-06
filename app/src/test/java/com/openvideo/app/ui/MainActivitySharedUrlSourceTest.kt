package com.openvideo.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MainActivitySharedUrlSourceTest {

    @Test
    fun manifestAcceptsSharedAndViewedNetworkUrls() {
        val manifest = loadText(Paths.get("src", "main", "AndroidManifest.xml"))
        val mainActivityBlock = manifest.substringAfter("""android:name="com.example.openvideo.ui.MainActivity"""")
            .substringBefore("""</activity-alias>""")

        assertTrue(mainActivityBlock.contains("""android.intent.action.SEND"""))
        assertTrue(mainActivityBlock.contains("""android:mimeType="text/plain""""))
        assertTrue(mainActivityBlock.contains("""android.intent.action.VIEW"""))
        assertTrue(mainActivityBlock.contains("""android:scheme="http""""))
        assertTrue(mainActivityBlock.contains("""android:scheme="https""""))
        assertTrue(mainActivityBlock.contains("""android:scheme="rtsp""""))
        assertTrue(mainActivityBlock.contains("""android.intent.category.BROWSABLE"""))
    }

    @Test
    fun mainActivityRoutesSharedUrlsToPlayer() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "openvideo", "app", "ui", "MainActivity.kt")
        )

        assertTrue(source.contains("import com.openvideo.app.core.network.NetworkSharedUrlPolicy"))
        assertTrue(source.contains("import com.openvideo.app.core.network.NetworkRecentUrlPolicy"))
        assertTrue(source.contains("import com.openvideo.app.data.repository.VideoRepository"))
        assertTrue(source.contains("import com.openvideo.app.ui.player.PlayerActivityIntents"))
        assertTrue(source.contains("@Inject lateinit var repository: VideoRepository"))
        assertTrue(source.contains("handleSharedPlaybackIntent(intent)"))
        assertTrue(source.contains("override fun onNewIntent(intent: Intent)"))
        assertTrue(source.contains("NetworkSharedUrlPolicy.extractPlaybackUrl("))
        assertTrue(source.contains("intent.action"))
        assertTrue(source.contains("intent.type"))
        assertTrue(source.contains("intent.getStringExtra(Intent.EXTRA_TEXT)"))
        assertTrue(source.contains("intent.dataString"))
        assertTrue(source.contains("NetworkRecentUrlPolicy.titleFor(playbackUrl)"))
        assertTrue(source.contains("repository.recordNetworkRecentUrl(playbackUrl, title)"))
        assertTrue(source.contains("PlayerActivityIntents.networkPlayback(this, playbackUrl)"))
        assertTrue(source.contains("startActivity(playerIntent)"))
    }

    @Test
    fun homeOpenUrlReusesPlayerIntentHelper() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "openvideo", "app", "ui", "home", "HomeFragment.kt")
        )
        val dialogBody = source.substringAfter("private fun showOpenUrlDialog()")
            .substringBefore("\n    private fun")

        assertTrue(dialogBody.contains("NetworkOpenUrlDialog.show("))
        assertTrue(dialogBody.contains("viewModel.recordNetworkRecentUrl(normalizedUrl, title)"))
    }

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
