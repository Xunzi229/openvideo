package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class NetworkOpenUrlDialogSourceTest {

    @Test
    fun sharedDialogValidatesRecordsAndStartsNetworkPlayback() {
        val source = playerSource("NetworkOpenUrlDialog.kt")

        assertTrue(source.contains("NetworkUrlPolicy.validatePlaybackUrl(input.text.toString())"))
        assertTrue(source.contains("NetworkUrlPolicy.Validation.Valid"))
        assertTrue(source.contains("NetworkRecentUrlPolicy.titleFor(result.normalizedUrl)"))
        assertTrue(source.contains("onRecordRecent(result.normalizedUrl, title)"))
        assertTrue(source.contains("PlayerActivityIntents.networkPlayback(context, result.normalizedUrl)"))
        assertTrue(source.contains("context.startActivity(intent)"))
        assertTrue(source.contains("R.string.home_open_url_invalid"))
    }

    @Test
    fun homeFragmentUsesSharedDialogInsteadOfDuplicatingUrlValidation() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", "HomeFragment.kt")
        )

        assertTrue(source.contains("NetworkOpenUrlDialog.show("))
        assertTrue(source.contains("viewModel.recordNetworkRecentUrl(normalizedUrl, title)"))
    }

    private fun playerSource(name: String): String = loadText(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", name)
    )

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
