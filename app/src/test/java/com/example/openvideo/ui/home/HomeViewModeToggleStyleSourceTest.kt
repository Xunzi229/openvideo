package com.example.openvideo.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HomeViewModeToggleStyleSourceTest {

    @Test
    fun viewModeToggleDoesNotUseElevatedWhiteBackground() {
        val source = String(Files.readAllBytes(homeLayoutSource()))
        val toggleContainer = source
            .substringBefore("<ImageButton\n                android:id=\"@+id/btn_list_view\"")
            .substringAfterLast("<LinearLayout")

        assertTrue(
            "View-mode toggle container should be addressable so its styling does not regress",
            toggleContainer.contains("android:id=\"@+id/view_mode_toggle\"")
        )
        assertTrue(
            "View-mode toggle container should blend with the gray page instead of drawing a white card",
            toggleContainer.contains("android:background=\"@android:color/transparent\"")
        )
        assertFalse(
            "View-mode toggle container should not use the elevated white surface color",
            toggleContainer.contains("@color/ov_bg_elevated")
        )
    }

    private fun homeLayoutSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "layout",
            "fragment_home.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
