package com.example.openvideo.ui.sources

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SourcesRecentPlaybackSourceTest {

    @Test
    fun sourcesLayoutDeclaresRecentPlaybackSection() {
        val layout = resourceText("layout", "fragment_sources.xml")

        assertTrue(layout.contains("""android:text="@string/sources_section_recent""""))
        assertTrue(layout.contains("""android:id="@+id/recycler_source_recent""""))
        assertTrue(layout.contains("""android:id="@+id/tv_source_recent_empty""""))
    }

    @Test
    fun sourcesFragmentAggregatesLocalHistoryAndNetworkRecentUrls() {
        val source = sourceText("sources", "SourcesFragment.kt")

        assertTrue(source.contains("combine(repository.getHistory(), repository.getNetworkRecentUrls())"))
        assertTrue(source.contains("SourceRecentPlaybackPolicy.buildItems("))
        assertTrue(source.contains("SourceRecentPlaybackLabels.from(requireContext())"))
        assertTrue(source.contains("File(path).exists()"))
    }

    @Test
    fun sourcesFragmentRoutesRecentRowsBySourceType() {
        val source = sourceText("sources", "SourcesFragment.kt")

        assertTrue(source.contains("SourceRecentPlaybackType.LOCAL"))
        assertTrue(source.contains("SourceRecentPlaybackType.NETWORK_URL"))
        assertTrue(source.contains("PlayerActivityIntents.networkPlayback(requireContext(), item.playbackUri)"))
        assertTrue(source.contains("repository.recordNetworkRecentUrl(item.playbackUri, item.title)"))
    }

    @Test
    fun recentPlaybackRowShowsSourceLabelAndDetailLabel() {
        val layout = resourceText("layout", "item_source_recent_playback.xml")

        assertTrue(layout.contains("""android:id="@+id/iv_source_icon""""))
        assertTrue(layout.contains("""android:id="@+id/tv_source_label""""))
        assertTrue(layout.contains("""android:id="@+id/tv_source_recent_title""""))
        assertTrue(layout.contains("""android:id="@+id/tv_source_recent_detail""""))
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
