package com.example.openvideo.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FocusStyleSourceTest {

    @Test
    fun coreBrowserItemsUseSharedFocusableForeground() {
        val focusDrawable = readResource("res", "drawable", "bg_focusable_card.xml")

        assertTrue(focusDrawable.contains("""<selector xmlns:android="http://schemas.android.com/apk/res/android">"""))
        assertTrue(focusDrawable.contains("""android:state_focused="true""""))
        assertTrue(focusDrawable.contains("""android:color="@color/ov_accent_blue""""))

        listOf(
            "item_video.xml",
            "item_video_grid.xml",
            "item_video_folder.xml",
            "item_series.xml",
            "item_series_episode.xml"
        ).forEach { name ->
            val layout = readResource("res", "layout", name)

            assertTrue("$name must be reachable by D-pad focus", layout.contains("""android:focusable="true""""))
            assertTrue("$name must remain an explicit click target", layout.contains("""android:clickable="true""""))
            assertTrue(
                "$name must show the shared focus ring",
                layout.contains("""android:foreground="@drawable/bg_focusable_card"""")
            )
        }
    }

    private fun readResource(vararg parts: String): String =
        String(Files.readAllBytes(resourcePath(*parts)))

    private fun resourcePath(vararg parts: String): Path {
        val relativePath = Paths.get("src", "main", *parts)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
