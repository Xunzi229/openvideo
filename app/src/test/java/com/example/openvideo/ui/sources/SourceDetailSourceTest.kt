package com.example.openvideo.ui.sources

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SourceDetailSourceTest {

    @Test
    fun repositoryPersistsUrlMediaSourceWhenRecordingNetworkRecentUrl() {
        val source = repositoryText()

        assertTrue(source.contains("private val mediaSourceDao: MediaSourceDao"))
        assertTrue(source.contains("fun getMediaSources(): Flow<List<MediaSourceEntity>>"))
        assertTrue(source.contains("suspend fun getMediaSource(sourceId: Long): MediaSourceEntity?"))
        assertTrue(source.contains("suspend fun deleteMediaSource(sourceId: Long)"))
        assertTrue(source.contains("""type = "url""""))
        assertTrue(source.contains("mediaSourceDao.upsert("))
        assertTrue(source.contains("mediaSourceDao.delete(sourceId)"))
    }

    @Test
    fun sourcesFragmentListsSavedSourcesAndOpensDetailFragment() {
        val source = sourceText("sources", "SourcesFragment.kt")
        val layout = resourceText("layout", "fragment_sources.xml")
        val row = resourceText("layout", "item_media_source.xml")

        assertTrue(layout.contains("""android:text="@string/sources_section_saved""""))
        assertTrue(layout.contains("""android:id="@+id/recycler_saved_sources""""))
        assertTrue(layout.contains("""android:id="@+id/tv_saved_sources_empty""""))
        assertTrue(row.contains("""android:id="@+id/tv_media_source_name""""))
        assertTrue(row.contains("""android:id="@+id/tv_media_source_address""""))
        assertTrue(source.contains("repository.getMediaSources()"))
        assertTrue(source.contains("SourceDetailFragment.newInstance(source.sourceId)"))
        assertTrue(source.contains("addToBackStack(\"source:\${source.sourceId}\")"))
    }

    @Test
    fun sourceDetailFragmentShowsFieldsAndConfirmsDelete() {
        val source = sourceText("sources", "SourceDetailFragment.kt")
        val layout = resourceText("layout", "fragment_source_detail.xml")

        assertTrue(layout.contains("""android:id="@+id/tv_source_detail_name""""))
        assertTrue(layout.contains("""android:id="@+id/tv_source_detail_address""""))
        assertTrue(layout.contains("""android:id="@+id/tv_source_detail_last_used""""))
        assertTrue(layout.contains("""android:id="@+id/btn_test_source""""))
        assertTrue(layout.contains("""android:id="@+id/btn_delete_source""""))
        assertTrue(source.contains("SourceDetailPresentationPolicy.buildUiState("))
        assertTrue(source.contains("SettingsConfirmationActionSheet.show("))
        assertTrue(source.contains("showExclusiveSourceDialog"))
        assertFalse(source.contains("MaterialAlertDialogBuilder"))
        assertFalse(source.contains(".setPositiveButton("))
        assertFalse(source.contains(".setNegativeButton("))
        assertTrue(source.contains("R.string.source_detail_delete_title"))
        assertTrue(source.contains("message = getString(R.string.source_detail_delete_message, source.name)"))
        assertTrue(source.contains("confirmRes = R.string.action_delete"))
        assertTrue(source.contains("cancelRes = R.string.action_cancel"))
        assertTrue(source.contains("repository.deleteMediaSource(sourceId)"))
    }

    @Test
    fun destructiveConfirmationRuleRequiresSettingsActionSheetStyle() {
        val designSystem = rootText("docs", "design-system.md")

        assertTrue(designSystem.contains("破坏性操作确认"))
        assertTrue(designSystem.contains("SettingsConfirmationActionSheet"))
        assertTrue(designSystem.contains("删除"))
        assertTrue(designSystem.contains("清除"))
        assertTrue(designSystem.contains("MaterialAlertDialogBuilder"))
    }

    private fun repositoryText(): String =
        loadText(Paths.get("src", "main", "java", "com", "example", "openvideo", "data", "repository", "VideoRepository.kt"))

    private fun sourceText(vararg parts: String): String =
        loadText(Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", *parts))

    private fun resourceText(dir: String, name: String): String =
        loadText(Paths.get("src", "main", "res", dir, name))

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            Paths.get("..").resolve(parts.fold(Paths.get("")) { path, part -> path.resolve(part) })
        ).first(Files::exists)
}
