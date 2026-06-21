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
        assertTrue(fragment.contains("R.id.row_version"))
        assertTrue(fragment.contains("R.id.row_license"))
        assertTrue(fragment.contains("row.isFocusable = tvMode"))
        assertTrue(fragment.contains("row.isFocusableInTouchMode = tvMode"))
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
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_clear_history, R.id.row_clear_cache, R.id.row_version)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_version, R.id.row_clear_history, R.id.row_license)"))
        assertTrue(fragment.contains("linkTvSettingsFocus(view, R.id.row_license, R.id.row_version, R.id.row_license)"))
        assertTrue(fragment.contains("private fun linkTvSettingsFocus(view: View, rowId: Int, upId: Int, downId: Int)"))
        assertTrue(fragment.contains("row.nextFocusUpId = upId"))
        assertTrue(fragment.contains("row.nextFocusDownId = downId"))
    }

    @Test
    fun tvModeSettingsRowsKeepHorizontalDpadFocusOnCurrentRow() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))

        assertTrue(fragment.contains("row.nextFocusLeftId = rowId"))
        assertTrue(fragment.contains("row.nextFocusRightId = rowId"))
    }

    @Test
    fun tvModeSettingsKeepsVersionRowInRemoteFocusOrder() {
        val layout = String(Files.readAllBytes(settingsLayoutSource()))

        val versionRow = layout.substringBefore("""android:text="@string/settings_version"""")
            .substringAfterLast("<LinearLayout")
        assertTrue(versionRow.contains("""android:id="@+id/row_version""""))
        assertTrue(versionRow.contains("""android:layout_height="56dp""""))
        assertTrue(versionRow.contains("""android:background="?attr/selectableItemBackground""""))
        assertTrue(
            layout.indexOf("""android:id="@+id/row_clear_history"""") <
                layout.indexOf("""android:id="@+id/row_version"""")
        )
        assertTrue(
            layout.indexOf("""android:id="@+id/row_version"""") <
                layout.indexOf("""android:id="@+id/row_license"""")
        )
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

    @Test
    fun tvModeShortcutRowsHaveStaticFocusableDescriptions() {
        val layout = String(Files.readAllBytes(settingsLayoutSource()))

        assertTrue(
            rowOpeningTag(layout, "row_tv_subtitle_settings")
                .contains("""android:contentDescription="@string/settings_group_subtitle"""")
        )
        assertTrue(
            rowOpeningTag(layout, "row_tv_audio_settings")
                .contains("""android:contentDescription="@string/settings_group_audio"""")
        )
        assertTrue(
            rowOpeningTag(layout, "row_tv_sources_settings")
                .contains("""android:contentDescription="@string/nav_sources"""")
        )
    }

    @Test
    fun tvModeRetainedRowsHaveStaticFocusableDescriptions() {
        val layout = String(Files.readAllBytes(settingsLayoutSource()))

        assertTrue(
            rowOpeningTag(layout, "row_default_ratio")
                .contains("""android:contentDescription="@string/settings_default_ratio"""")
        )
        assertTrue(
            rowOpeningTag(layout, "row_default_speed")
                .contains("""android:contentDescription="@string/settings_default_speed"""")
        )
        assertTrue(
            rowOpeningTag(layout, "row_clear_cache")
                .contains("""android:contentDescription="@string/settings_clear_cache"""")
        )
        assertTrue(
            rowOpeningTag(layout, "row_clear_history")
                .contains("""android:contentDescription="@string/settings_clear_history"""")
        )
        assertTrue(
            rowOpeningTag(layout, "row_version")
                .contains("""android:contentDescription="@string/settings_version"""")
        )
        assertTrue(
            rowOpeningTag(layout, "row_license")
                .contains("""android:contentDescription="@string/settings_license"""")
        )
    }

    @Test
    fun defaultPlaybackRowsSyncFocusableDescriptionsWithCurrentValues() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))

        val ratioBlock = fragment.substringAfter("private fun updateRatioLabel(tv: TextView)")
            .substringBefore("\n    private fun updateSpeedLabel")
        assertTrue(ratioBlock.contains("updateSettingsRowDescription(tv, R.string.settings_default_ratio)"))

        val speedBlock = fragment.substringAfter("private fun updateSpeedLabel(tv: TextView)")
            .substringBefore("\n    private fun showDefaultRatioDialog")
        assertTrue(speedBlock.contains("updateSettingsRowDescription(tv, R.string.settings_default_speed)"))

        assertTrue(fragment.contains("private fun updateSettingsRowDescription(valueView: TextView, titleRes: Int)"))
        assertTrue(fragment.contains("(valueView.parent as? View)?.contentDescription"))
        assertTrue(fragment.contains("listOf(getString(titleRes), valueView.text).joinToString(\" \")"))
    }

    @Test
    fun storageRowsSyncFocusableDescriptionsWithCurrentValues() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))

        val cacheCollector = fragment.substringAfter("viewModel.cacheSize.collect")
            .substringBefore("\n                }")
        assertTrue(cacheCollector.contains("tvCacheSize.text = it"))
        assertTrue(cacheCollector.contains("updateSettingsRowDescription(tvCacheSize, R.string.settings_clear_cache)"))

        val historyCollector = fragment.substringAfter("viewModel.historyCount.collect")
            .substringBefore("\n                }")
        assertTrue(historyCollector.contains("tvHistoryCount.text = it.toString()"))
        assertTrue(historyCollector.contains("updateSettingsRowDescription(tvHistoryCount, R.string.settings_clear_history)"))
    }

    @Test
    fun versionRowSyncsFocusableDescriptionWithInstalledVersion() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))

        val versionInitBlock = fragment.substringAfter("val tvVersion = view.findViewById<TextView>(R.id.tv_version)")
            .substringBefore("\n\n        view.findViewById<View>(R.id.row_theme)")
        assertTrue(versionInitBlock.contains("tvVersion.text = viewModel.installedVersionName()"))
        assertTrue(versionInitBlock.contains("updateSettingsRowDescription(tvVersion, R.string.settings_version)"))
    }

    @Test
    fun tvModeKeepsBackupSectionHiddenEvenIfBackupUiSwitchesAreEnabled() {
        val fragment = String(Files.readAllBytes(settingsFragmentSource()))

        assertTrue(fragment.contains("bindBackupSection(view, tvMode = (activity as? MainActivity)?.isTvMode == true)"))
        assertTrue(fragment.contains("private fun bindBackupSection(view: View, tvMode: Boolean)"))
        assertTrue(fragment.contains("if (tvMode) {"))
        assertTrue(fragment.contains("section.visibility = View.GONE"))
        assertTrue(fragment.contains("return"))
        assertTrue(fragment.contains("SettingsBackupUiPolicy.SETTINGS_EXPORT_ENTRY_VISIBLE"))
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

    private fun rowOpeningTag(layout: String, rowId: String): String {
        val rowIdIndex = layout.indexOf("""android:id="@+id/$rowId"""")
        assertTrue("Missing row id $rowId", rowIdIndex >= 0)
        val tagStart = layout.lastIndexOf("<LinearLayout", rowIdIndex)
        val tagEnd = layout.indexOf(">", rowIdIndex)
        assertTrue("Missing row opening tag for $rowId", tagStart >= 0 && tagEnd > tagStart)
        return layout.substring(tagStart, tagEnd)
    }
}
