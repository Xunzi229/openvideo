package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeOpenUrlSourceTest {

    @Test
    fun homeHeaderExposesOpenUrlButton() {
        val layout = resourceText("layout", "fragment_home.xml")

        assertTrue(layout.contains("""android:id="@+id/btn_open_url""""))
        assertTrue(layout.contains("""android:contentDescription="@string/home_open_url""""))
        assertTrue(layout.contains("""android:src="@drawable/ic_stream""""))
    }

    @Test
    fun homeFragmentShowsUrlDialogAndStartsPlaybackForValidUrl() {
        val source = sourceText("HomeFragment.kt")

        assertTrue(source.contains("import com.example.openvideo.ui.player.NetworkOpenUrlDialog"))
        assertTrue(source.contains("view.findViewById<ImageButton>(R.id.btn_open_url).setOnClickListener"))
        assertTrue(source.contains("showOpenUrlDialog()"))
        assertTrue(source.contains("private fun showOpenUrlDialog()"))
        val dialogBody = source.substringAfter("private fun showOpenUrlDialog()")
            .substringBefore("\n    private fun")
        assertTrue(dialogBody.contains("NetworkOpenUrlDialog.show("))
        assertTrue(dialogBody.contains("viewModel.recordNetworkRecentUrl(normalizedUrl, title)"))
    }

    private fun sourceText(name: String): String =
        String(Files.readAllBytes(sourceFile(name)))

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun resourceText(dir: String, name: String): String =
        String(Files.readAllBytes(resourceFile(dir, name)))

    private fun resourceFile(dir: String, name: String): Path {
        val relativePath = Paths.get("src", "main", "res", dir, name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
