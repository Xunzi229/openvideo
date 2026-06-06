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
    fun sourcesFragmentReusesSharedOpenUrlDialog() {
        val source = sourceText("sources", "SourcesFragment.kt")

        assertTrue(source.contains("NetworkOpenUrlDialog.show("))
        assertTrue(source.contains("R.id.row_source_open_url"))
        assertTrue(source.contains("repository.recordNetworkRecentUrl(normalizedUrl, title)"))
    }

    private fun sourceText(vararg parts: String): String =
        loadText(Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", *parts))

    private fun resourceText(dir: String, name: String): String =
        loadText(Paths.get("src", "main", "res", dir, name))

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
