package com.example.openvideo.ui.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class LocalSeriesEntrySourceTest {

    @Test
    fun localFolderHeaderExposesSeriesEntryButton() {
        val layout = String(Files.readAllBytes(localFoldersLayout()))

        assertTrue(layout.contains("""android:id="@+id/btn_series""""))
        assertTrue(layout.contains("""android:contentDescription="@string/series_list_title""""))
        assertTrue(layout.contains("""android:src="@drawable/ic_list""""))
    }

    @Test
    fun localFolderFragmentNavigatesToSeriesListWithoutStartingPlayer() {
        val source = String(Files.readAllBytes(localFolderFragmentSource()))

        assertTrue(source.contains("import com.example.openvideo.ui.series.SeriesListFragment"))
        assertTrue(source.contains("view.findViewById<View>(R.id.btn_series).setOnClickListener {"))
        assertTrue(source.contains("openSeriesList()"))
        assertTrue(source.contains("private fun openSeriesList()"))
        assertTrue(source.contains(".replace(R.id.fragment_container, SeriesListFragment())"))
        assertTrue(source.contains(".addToBackStack(\"series:list\")"))
        assertFalse(source.contains("SeriesListFragment.newInstance"))
        assertFalse(source.substringAfter("private fun openSeriesList()").substringBefore("\n    private fun").contains("PlayerActivity"))
    }

    private fun localFoldersLayout(): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", "fragment_local_folders.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun localFolderFragmentSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "local",
            "LocalFolderFragment.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
