package com.example.openvideo.ui.sources

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SourcesInformationArchitectureSourceTest {

    @Test
    fun bottomNavigationExposesSourcesTab() {
        val menu = resourceText("menu", "bottom_nav_menu.xml")

        assertTrue(menu.contains("""android:id="@+id/nav_sources""""))
        assertTrue(menu.contains("""android:title="@string/nav_sources""""))
        assertTrue(menu.contains("""android:icon="@drawable/ic_stream""""))
    }

    @Test
    fun mainActivityRoutesSourcesTabToSourcesFragment() {
        val source = sourceText("MainActivity.kt")

        assertTrue(source.contains("import com.example.openvideo.ui.sources.SourcesFragment"))
        assertTrue(source.contains("R.id.nav_sources -> SourcesFragment()"))
    }

    @Test
    fun sourcesLayoutDeclaresCurrentAndFutureSourceRows() {
        val layout = resourceText("layout", "fragment_sources.xml")

        assertTrue(layout.contains("""android:text="@string/sources_title""""))
        assertTrue(layout.contains("""android:id="@+id/row_source_local""""))
        assertTrue(layout.contains("""android:id="@+id/row_source_open_url""""))
        assertTrue(layout.contains("""android:id="@+id/row_source_webdav""""))
        assertTrue(layout.contains("""android:id="@+id/row_source_future""""))
        assertTrue(layout.contains("""android:text="@string/sources_webdav_status_planned""""))
    }

    @Test
    fun sourcesRowsUseSharedFocusableForegroundForRemoteNavigation() {
        val layout = resourceText("layout", "fragment_sources.xml")
        listOf(
            "row_source_local",
            "row_source_open_url",
            "row_source_webdav",
            "row_source_future"
        ).forEach { rowId ->
            assertFocusableSourceRow(
                name = rowId,
                block = layout.substringAfter("""android:id="@+id/$rowId"""").substringBefore("<ImageView")
            )
        }

        listOf("item_media_source.xml", "item_source_recent_playback.xml").forEach { name ->
            assertFocusableSourceRow(
                name = name,
                block = resourceText("layout", name).substringBefore("<ImageView")
            )
        }
    }

    @Test
    fun sourcesFragmentReusesSharedOpenUrlDialog() {
        val source = sourceText("sources", "SourcesFragment.kt")

        assertTrue(source.contains("NetworkOpenUrlDialog.show("))
        assertTrue(source.contains("R.id.row_source_open_url"))
        assertTrue(source.contains("repository.recordNetworkRecentUrl(normalizedUrl, title)"))
    }

    @Test
    fun sourcesFragmentDefaultsFocusToLocalSourceRowForRemoteUse() {
        val source = sourceText("sources", "SourcesFragment.kt")

        assertTrue(source.contains("val localSourceRow = view.findViewById<View>(R.id.row_source_local)"))
        assertTrue(source.contains("localSourceRow.post { localSourceRow.requestFocus() }"))
    }

    @Test
    fun sourcesFragmentKeepsRemoteFocusOrderAlignedWithDynamicSections() {
        val source = sourceText("sources", "SourcesFragment.kt")

        assertTrue(source.contains("recentRecycler.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS"))
        assertTrue(source.contains("savedSourcesRecycler.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS"))
        assertTrue(source.contains("private fun updateSourcesContentFocusTargets("))
        assertTrue(source.contains("val contentAfterOpenUrlId = when"))
        assertTrue(source.contains("R.id.recycler_saved_sources"))
        assertTrue(source.contains("R.id.recycler_source_recent"))
        assertTrue(source.contains("R.id.row_source_webdav"))
        assertTrue(source.contains("view.findViewById<View>(R.id.row_source_local).nextFocusDownId = R.id.row_source_open_url"))
        assertTrue(source.contains("view.findViewById<View>(R.id.row_source_open_url).nextFocusUpId = R.id.row_source_local"))
        assertTrue(source.contains("view.findViewById<View>(R.id.row_source_open_url).nextFocusDownId = contentAfterOpenUrlId"))
        assertTrue(source.contains("view.findViewById<View>(R.id.row_source_webdav).nextFocusDownId = R.id.row_source_future"))
        assertTrue(source.contains("view.findViewById<View>(R.id.row_source_future).nextFocusUpId = R.id.row_source_webdav"))
        assertTrue(source.contains("updateSourcesContentFocusTargets(view, hasSavedSources, hasRecentPlayback)"))
    }

    private fun sourceText(vararg parts: String): String =
        loadText(Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", *parts))

    private fun resourceText(dir: String, name: String): String =
        loadText(Paths.get("src", "main", "res", dir, name))

    private fun assertFocusableSourceRow(name: String, block: String) {
        assertTrue("$name must be reachable by D-pad focus", block.contains("""android:focusable="true""""))
        assertTrue("$name must remain an explicit click target", block.contains("""android:clickable="true""""))
        assertTrue(
            "$name must show the shared focus ring",
            block.contains("""android:foreground="@drawable/bg_focusable_card"""")
        )
    }

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
