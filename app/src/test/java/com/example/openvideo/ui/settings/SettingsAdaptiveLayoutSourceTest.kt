package com.example.openvideo.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SettingsAdaptiveLayoutSourceTest {

    @Test
    fun settingsPageUsesBreakpointAwareTwoColumnContainer() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))
        val layout = String(Files.readAllBytes(settingsLayoutSource()))

        assertTrue(fragment.contains("import android.widget.FrameLayout"))
        assertTrue(fragment.contains("import android.content.res.Configuration"))
        assertTrue(fragment.contains("import android.widget.LinearLayout"))
        assertTrue(fragment.contains("import androidx.core.view.updateLayoutParams"))
        assertTrue(fragment.contains("import com.example.openvideo.core.ui.ScreenBreakpoint"))
        assertTrue(fragment.contains("import com.example.openvideo.ui.MainActivity"))
        assertTrue(fragment.contains("applyAdaptiveSettingsLayout(view)"))
        assertTrue(fragment.contains("private fun applyAdaptiveSettingsLayout(view: View)"))
        assertTrue(fragment.contains("val breakpoint = (activity as? MainActivity)?.breakpoint ?: ScreenBreakpoint.COMPACT"))
        assertTrue(fragment.contains("val wide = breakpoint.isAtLeastMedium"))
        assertTrue(fragment.contains("content.updateLayoutParams<FrameLayout.LayoutParams>"))
        assertTrue(fragment.contains("columns.orientation = LinearLayout.HORIZONTAL"))
        assertTrue(fragment.contains("columns.orientation = LinearLayout.VERTICAL"))
        assertTrue(fragment.contains("primaryColumn.updateLayoutParams<LinearLayout.LayoutParams>"))
        assertTrue(fragment.contains("secondaryColumn.updateLayoutParams<LinearLayout.LayoutParams>"))
        assertTrue(fragment.contains("override fun onConfigurationChanged(newConfig: Configuration)"))
        assertTrue(fragment.contains("view?.let(::applyAdaptiveSettingsLayout)"))

        assertTrue(layout.contains("""android:id="@+id/settings_content""""))
        assertTrue(layout.contains("""android:id="@+id/settings_columns""""))
        assertTrue(layout.contains("""android:id="@+id/settings_primary_column""""))
        assertTrue(layout.contains("""android:id="@+id/settings_secondary_column""""))
    }

    private fun settingsFragmentSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "settings",
            "SettingsFragment.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun settingsLayoutSource(): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", "fragment_settings.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
