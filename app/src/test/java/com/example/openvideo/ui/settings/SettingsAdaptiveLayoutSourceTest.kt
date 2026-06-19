package com.example.openvideo.ui.settings

import org.junit.Assert.assertFalse
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
        assertTrue(fragment.contains("val mainActivity = activity as? MainActivity"))
        assertTrue(fragment.contains("val breakpoint = mainActivity?.breakpoint ?: ScreenBreakpoint.COMPACT"))
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

    @Test
    fun settingsRowsUseAtLeast56dpTouchTargets() {
        val layout = String(Files.readAllBytes(settingsLayoutSource()))

        assertFalse(layout.contains("""android:layout_height="52dp""""))
        assertTrue(layout.contains("""android:layout_height="56dp""""))
    }

    @Test
    fun tvModeKeepsSettingsPageToRemoteFriendlyRows() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))
        val layout = String(Files.readAllBytes(settingsLayoutSource()))

        assertTrue(fragment.contains("val mainActivity = activity as? MainActivity"))
        assertTrue(fragment.contains("applyTvSettingsSimplification(view, mainActivity?.isTvMode == true)"))
        assertTrue(fragment.contains("private fun applyTvSettingsSimplification(view: View, tvMode: Boolean)"))
        assertTrue(fragment.contains("R.id.settings_general_section_title"))
        assertTrue(fragment.contains("R.id.settings_backup_section"))
        assertTrue(fragment.contains("R.id.row_notifications"))
        assertTrue(fragment.contains("R.id.row_check_update"))
        assertTrue(fragment.contains("R.id.row_project_repo"))

        assertTrue(layout.contains("""android:id="@+id/settings_general_section_title""""))
        assertTrue(layout.contains("""android:id="@+id/divider_theme""""))
        assertTrue(layout.contains("""android:id="@+id/divider_language""""))
        assertTrue(layout.contains("""android:id="@+id/divider_notifications""""))
        assertTrue(layout.contains("""android:id="@+id/divider_check_update""""))
        assertTrue(layout.contains("""android:id="@+id/divider_project_repo""""))

        assertTrue(
            layout.indexOf("""android:id="@+id/row_check_update"""") <
                layout.indexOf("""android:id="@+id/divider_check_update"""")
        )
        assertTrue(
            layout.indexOf("""android:id="@+id/divider_check_update"""") <
                layout.indexOf("""android:text="@string/settings_version"""")
        )
        assertTrue(
            layout.indexOf("""android:id="@+id/row_project_repo"""") <
                layout.indexOf("""android:id="@+id/divider_project_repo"""")
        )
        assertTrue(
            layout.indexOf("""android:id="@+id/divider_project_repo"""") <
                layout.indexOf("""android:id="@+id/row_license"""")
        )
    }

    @Test
    fun tvModeSettingsRowsHaveRemoteFocusDefault() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))

        assertTrue(fragment.contains("applyTvSettingsFocusDefaults(view, tvMode)"))
        assertTrue(fragment.contains("private fun applyTvSettingsFocusDefaults(view: View, tvMode: Boolean)"))
        assertTrue(fragment.contains("R.id.row_default_ratio"))
        assertTrue(fragment.contains("R.id.row_default_speed"))
        assertTrue(fragment.contains("R.id.row_clear_cache"))
        assertTrue(fragment.contains("R.id.row_clear_history"))
        assertTrue(fragment.contains("R.id.row_license"))
        assertTrue(fragment.contains("row.isFocusable = tvMode"))
        assertTrue(fragment.contains("defaultFocus.post { defaultFocus.requestFocus() }"))
    }

    @Test
    fun tvModeSettingsRowsHaveStableRemoteFocusOrder() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))

        assertTrue(fragment.contains("applyTvSettingsFocusOrder(view)"))
        assertTrue(fragment.contains("private fun applyTvSettingsFocusOrder(view: View)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_default_ratio, R.id.row_default_ratio, R.id.row_default_speed)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_default_speed, R.id.row_default_ratio, R.id.row_tv_subtitle_settings)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_tv_subtitle_settings, R.id.row_default_speed, R.id.row_tv_audio_settings)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_tv_audio_settings, R.id.row_tv_subtitle_settings, R.id.row_tv_sources_settings)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_tv_sources_settings, R.id.row_tv_audio_settings, R.id.row_clear_cache)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_clear_cache, R.id.row_tv_sources_settings, R.id.row_clear_history)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_clear_history, R.id.row_clear_cache, R.id.row_license)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_license, R.id.row_clear_history, R.id.row_license)"))
        assertTrue(fragment.contains("private fun linkTvSettingsFocus(view: View, rowId: Int, upId: Int, downId: Int)"))
        assertTrue(fragment.contains("row.nextFocusUpId = upId"))
        assertTrue(fragment.contains("row.nextFocusDownId = downId"))
    }

    @Test
    fun tvModeSettingsExposeSubtitleAndAudioShortcutsThroughExistingActivities() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))
        val layout = String(Files.readAllBytes(settingsLayoutSource()))

        assertTrue(fragment.contains("import com.example.openvideo.ui.player.PlayerAudioSettingsActivity"))
        assertTrue(fragment.contains("import com.example.openvideo.ui.player.PlayerSubtitleSettingsActivity"))
        assertTrue(fragment.contains("R.id.row_tv_subtitle_settings"))
        assertTrue(fragment.contains("R.id.row_tv_audio_settings"))
        assertTrue(fragment.contains("PlayerSubtitleSettingsActivity::class.java"))
        assertTrue(fragment.contains("PlayerAudioSettingsActivity::class.java"))
        assertTrue(fragment.contains("val tvShortcutVisibility = if (tvMode) View.VISIBLE else View.GONE"))
        assertTrue(fragment.contains("R.id.divider_tv_subtitle_settings"))
        assertTrue(fragment.contains("R.id.divider_tv_audio_settings"))

        assertTrue(layout.contains("""android:id="@+id/row_tv_subtitle_settings""""))
        assertTrue(layout.contains("""android:id="@+id/divider_tv_subtitle_settings""""))
        assertTrue(layout.contains("""android:id="@+id/row_tv_audio_settings""""))
        assertTrue(layout.contains("""android:id="@+id/divider_tv_audio_settings""""))
        assertTrue(layout.contains("""android:text="@string/settings_group_subtitle""""))
        assertTrue(layout.contains("""android:text="@string/settings_group_audio""""))
        assertTrue(layout.contains("""android:visibility="gone""""))
    }

    @Test
    fun tvModeSettingsExposeSourcesShortcutThroughExistingSourcesFragment() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))
        val layout = String(Files.readAllBytes(settingsLayoutSource()))

        assertTrue(fragment.contains("import com.example.openvideo.ui.sources.SourcesFragment"))
        assertTrue(fragment.contains("R.id.row_tv_sources_settings"))
        assertTrue(fragment.contains("R.id.divider_tv_sources_settings"))
        assertTrue(fragment.contains("parentFragmentManager.beginTransaction()"))
        assertTrue(fragment.contains(".replace(R.id.fragment_container, SourcesFragment())"))
        assertTrue(fragment.contains(".addToBackStack(\"tv_settings_sources\")"))

        assertTrue(layout.contains("""android:id="@+id/row_tv_sources_settings""""))
        assertTrue(layout.contains("""android:id="@+id/divider_tv_sources_settings""""))
        assertTrue(layout.contains("""android:text="@string/nav_sources""""))
        assertTrue(layout.contains("""android:visibility="gone""""))
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
