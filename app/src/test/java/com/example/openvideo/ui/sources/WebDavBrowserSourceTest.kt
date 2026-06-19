package com.example.openvideo.ui.sources

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class WebDavBrowserSourceTest {

    @Test
    fun sourceDetailShowsBrowseActionOnlyForWebDavSources() {
        val source = sourceText("SourceDetailFragment.kt")
        val layout = resourceText("layout", "fragment_source_detail.xml")

        assertTrue(layout.contains("""android:id="@+id/btn_browse_source""""))
        assertTrue(layout.contains("""android:text="@string/source_detail_browse""""))
        assertTrue(source.contains("btn_browse_source"))
        assertTrue(source.contains("""source.type.equals("webdav", ignoreCase = true)"""))
        assertTrue(source.contains("WebDavBrowserFragment.newInstance("))
    }

    @Test
    fun webDavBrowserListsFoldersAndPlaysVideoUrls() {
        val fragment = sourceText("WebDavBrowserFragment.kt")
        val layout = resourceText("layout", "fragment_webdav_browser.xml")
        val row = resourceText("layout", "item_webdav_entry.xml")

        assertTrue(layout.contains("""android:id="@+id/recycler_webdav_entries""""))
        assertTrue(layout.contains("""android:id="@+id/tv_webdav_empty""""))
        assertTrue(row.contains("""android:id="@+id/tv_webdav_entry_name""""))
        assertTrue(row.contains("""android:id="@+id/tv_webdav_entry_meta""""))
        assertTrue(fragment.contains("webDavConnectionClient.listDirectory("))
        assertTrue(fragment.contains("repository.getWebDavCredentials(sourceId)"))
        assertTrue(fragment.contains("newInstance(sourceId, entry.url)"))
        assertTrue(fragment.contains("PlayerActivityIntents.networkPlayback("))
        assertFalse(fragment.contains("password + \"@\""))
        assertFalse(fragment.contains("username + \":\""))
    }

    @Test
    fun webDavBrowserUsesRemoteFocusableRowsAndBackDefaultFocus() {
        val fragment = sourceText("WebDavBrowserFragment.kt")
        val row = resourceText("layout", "item_webdav_entry.xml")

        assertTrue(row.contains("""android:focusable="true""""))
        assertTrue(row.contains("""android:clickable="true""""))
        assertTrue(row.contains("""android:foreground="@drawable/bg_focusable_card""""))
        assertTrue(fragment.contains("val backButton = view.findViewById<View>(R.id.btn_webdav_back)"))
        assertTrue(fragment.contains("backButton.post { backButton.requestFocus() }"))
    }

    @Test
    fun webDavBrowserKeepsBackFocusLinkedToEntriesOrEmptyState() {
        val fragment = sourceText("WebDavBrowserFragment.kt")
        val layout = resourceText("layout", "fragment_webdav_browser.xml")
        val emptyBlock = layout.substringAfter("""android:id="@+id/tv_webdav_empty"""")
            .substringBefore("""<androidx.recyclerview.widget.RecyclerView""")

        assertTrue(emptyBlock.contains("""android:clickable="true""""))
        assertTrue(emptyBlock.contains("""android:focusable="true""""))
        assertTrue(emptyBlock.contains("""android:foreground="@drawable/bg_focusable_card""""))
        assertTrue(emptyBlock.contains("""android:nextFocusUp="@id/btn_webdav_back""""))
        assertTrue(layout.contains("""android:descendantFocusability="afterDescendants""""))
        assertTrue(layout.contains("""android:nextFocusUp="@id/btn_webdav_back""""))
        assertTrue(fragment.contains("private fun updateContentFocusTarget(hasEntries: Boolean)"))
        assertTrue(fragment.contains("val contentFocusTargetId = if (hasEntries) R.id.recycler_webdav_entries else R.id.tv_webdav_empty"))
        assertTrue(fragment.contains("backButton.nextFocusDownId = contentFocusTargetId"))
        assertTrue(fragment.contains("updateContentFocusTarget(result.entries.isNotEmpty())"))
        assertTrue(fragment.contains("updateContentFocusTarget(hasEntries = false)"))
    }

    @Test
    fun webDavBrowserPassesAuthorizationHeaderForPlayableVideos() {
        val fragment = sourceText("WebDavBrowserFragment.kt")

        assertTrue(fragment.contains("import okhttp3.Credentials"))
        assertTrue(fragment.contains("import com.example.openvideo.core.network.WebDavSubtitleMatcher"))
        assertTrue(fragment.contains("private var credentials: WebDavCredentialStore.Credentials? = null"))
        assertTrue(fragment.contains("this@WebDavBrowserFragment.credentials = credentials"))
        assertTrue(fragment.contains("\"Authorization\" to Credentials.basic(credentials.username, credentials.password)"))
        assertTrue(fragment.contains("requestHeaders = mapOf("))
        assertTrue(fragment.contains("externalSubtitleUri = WebDavSubtitleMatcher.matchForVideo("))
        assertTrue(fragment.contains("entries = adapter.currentList"))
        assertFalse("WebDAV playback must not put credentials in the URL", fragment.contains("entry.url.replace(\"https://\","))
    }

    private fun sourceText(name: String): String =
        rootText("app", "src", "main", "java", "com", "example", "openvideo", "ui", "sources", name)

    private fun resourceText(dir: String, name: String): String =
        rootText("app", "src", "main", "res", dir, name)

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
