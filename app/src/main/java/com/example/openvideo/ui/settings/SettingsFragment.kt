package com.example.openvideo.ui.settings

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.openvideo.R
import com.example.openvideo.core.prefs.SettingsBackupFileWriter
import com.example.openvideo.core.prefs.SettingsBackupSchema
import com.example.openvideo.core.ui.ScreenBreakpoint
import com.example.openvideo.ui.MainActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.example.openvideo.core.prefs.ThemeMode
import com.example.openvideo.ui.player.PlayerAspectRatioOptions
import com.example.openvideo.ui.player.PlayerAudioSettingsActivity
import com.example.openvideo.ui.player.PlayerGlassSheetChoice
import com.example.openvideo.ui.player.PlayerGlassSheetDialog
import com.example.openvideo.ui.player.PlayerSubtitleSettingsActivity
import com.example.openvideo.ui.sources.SourcesFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by activityViewModels()
    private var activeSettingsDialog: Dialog? = null

    private val exportSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(SettingsBackupSchema.MIME_TYPE)
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            when (viewModel.writeSettingsExportTo(requireContext(), uri)) {
                is SettingsBackupFileWriter.Result.Success ->
                    Toast.makeText(
                        requireContext(),
                        R.string.settings_toast_export_success,
                        Toast.LENGTH_SHORT
                    ).show()
                is SettingsBackupFileWriter.Result.Failure ->
                    Toast.makeText(
                        requireContext(),
                        R.string.settings_toast_export_failed,
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

    private val importSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = viewModel.readAndImportSettings(requireContext(), uri)) {
                is SettingsViewModel.ImportResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.settings_toast_import_success,
                        Toast.LENGTH_SHORT
                    ).show()
                    // Recreate activity so theme / language changes take effect immediately
                    activity?.recreate()
                }
                is SettingsViewModel.ImportResult.ParseFailure -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.settings_toast_import_parse_error,
                        Toast.LENGTH_LONG
                    ).show()
                }
                is SettingsViewModel.ImportResult.ReadFailure -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.settings_toast_import_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        private val PROJECT_REPO_URI: Uri = Uri.parse("https://github.com/Xunzi229/openvideo")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyAdaptiveSettingsLayout(view)

        val tvTheme = view.findViewById<TextView>(R.id.tv_theme_value)
        val tvLanguage = view.findViewById<TextView>(R.id.tv_language_value)
        val tvRatio = view.findViewById<TextView>(R.id.tv_ratio_value)
        val tvSpeed = view.findViewById<TextView>(R.id.tv_speed_value)
        val tvCacheSize = view.findViewById<TextView>(R.id.tv_cache_size)
        val tvHistoryCount = view.findViewById<TextView>(R.id.tv_history_count)
        val tvVersion = view.findViewById<TextView>(R.id.tv_version)

        updateThemeLabel(tvTheme)
        updateLanguageLabel(tvLanguage)
        updateRatioLabel(tvRatio)
        updateSpeedLabel(tvSpeed)
        tvVersion.text = viewModel.installedVersionName()

        view.findViewById<View>(R.id.row_theme).setOnClickListener {
            val modes = ThemeMode.entries
            val next = (modes.indexOf(viewModel.themeMode) + 1) % modes.size
            viewModel.setThemeMode(modes[next])
            updateThemeLabel(tvTheme)
        }

        view.findViewById<View>(R.id.row_language).setOnClickListener { row ->
            val langs = listOf("system", "zh", "en")
            val current = langs.indexOf(viewModel.language).let { idx ->
                if (idx >= 0) idx else 0
            }
            val next = (current + 1) % langs.size
            viewModel.setLanguage(langs[next])
            updateLanguageLabel(tvLanguage)
            // API 33+: per-app locales trigger recreation; immediate recreate() races and can ignore zh-CN.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                row.post { requireActivity().recreate() }
            }
        }

        view.findViewById<View>(R.id.row_notifications).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationSettingsActivity::class.java))
        }

        view.findViewById<View>(R.id.row_default_ratio).setOnClickListener {
            showDefaultRatioDialog(tvRatio)
        }

        view.findViewById<View>(R.id.row_default_speed).setOnClickListener {
            showDefaultSpeedDialog(tvSpeed)
        }

        view.findViewById<View>(R.id.row_tv_subtitle_settings).setOnClickListener {
            startActivity(Intent(requireContext(), PlayerSubtitleSettingsActivity::class.java))
        }

        view.findViewById<View>(R.id.row_tv_audio_settings).setOnClickListener {
            startActivity(Intent(requireContext(), PlayerAudioSettingsActivity::class.java))
        }

        view.findViewById<View>(R.id.row_tv_sources_settings).setOnClickListener {
            openTvSourcesSettings()
        }

        view.findViewById<View>(R.id.row_clear_cache).setOnClickListener {
            showExclusiveSettingsDialog { onDismiss ->
                SettingsConfirmationActionSheet.show(
                    context = requireContext(),
                    titleRes = R.string.dialog_clear_cache_title,
                    messageRes = R.string.dialog_clear_cache_message,
                    confirmRes = R.string.action_clear,
                    cancelRes = R.string.action_cancel,
                    onDismiss = onDismiss,
                    onConfirm = { viewModel.clearCache() }
                )
            }
        }

        view.findViewById<View>(R.id.row_clear_history).setOnClickListener {
            showExclusiveSettingsDialog { onDismiss ->
                SettingsConfirmationActionSheet.show(
                    context = requireContext(),
                    titleRes = R.string.dialog_clear_history_title,
                    messageRes = R.string.dialog_clear_history_message,
                    confirmRes = R.string.action_clear,
                    cancelRes = R.string.action_cancel,
                    onDismiss = onDismiss,
                    onConfirm = { viewModel.clearHistory() }
                )
            }
        }

        bindBackupSection(view)

        view.findViewById<View>(R.id.row_project_repo).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, PROJECT_REPO_URI))
        }

        view.findViewById<View>(R.id.row_license).setOnClickListener {
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.settings_license))
            startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
        }

        val updateBadgeDot = view.findViewById<View>(R.id.update_badge_dot)
        view.findViewById<View>(R.id.row_check_update).setOnClickListener {
            viewModel.onCheckUpdateClick(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cacheSize.collect { tvCacheSize.text = it }
                }
                launch {
                    viewModel.historyCount.collect { tvHistoryCount.text = it.toString() }
                }
                launch {
                    viewModel.updateBadgeVisible.collect { visible ->
                        updateBadgeDot.visibility = if (visible) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        view?.let(::applyAdaptiveSettingsLayout)
    }

    private fun applyAdaptiveSettingsLayout(view: View) {
        val mainActivity = activity as? MainActivity
        val breakpoint = mainActivity?.breakpoint ?: ScreenBreakpoint.COMPACT
        val wide = breakpoint.isAtLeastMedium
        val content = view.findViewById<LinearLayout>(R.id.settings_content)
        val columns = view.findViewById<LinearLayout>(R.id.settings_columns)
        val primaryColumn = view.findViewById<LinearLayout>(R.id.settings_primary_column)
        val secondaryColumn = view.findViewById<LinearLayout>(R.id.settings_secondary_column)
        val horizontalPadding = if (wide) 16.dpToPx() else 0
        val columnGap = if (wide) 12.dpToPx() else 0
        val maxContentWidthDp = if (breakpoint == ScreenBreakpoint.EXPANDED) 1120 else 920

        content.updateLayoutParams<FrameLayout.LayoutParams> {
            width = if (wide) {
                maxContentWidthDp.dpToPx().coerceAtMost(resources.displayMetrics.widthPixels)
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }
            gravity = if (wide) Gravity.TOP or Gravity.CENTER_HORIZONTAL else Gravity.TOP
        }
        if (wide) {
            columns.orientation = LinearLayout.HORIZONTAL
        } else {
            columns.orientation = LinearLayout.VERTICAL
        }
        columns.setPadding(horizontalPadding, 0, horizontalPadding, 0)
        primaryColumn.updateLayoutParams<LinearLayout.LayoutParams> {
            width = if (wide) 0 else ViewGroup.LayoutParams.MATCH_PARENT
            weight = if (wide) 1f else 0f
            marginStart = 0
            marginEnd = columnGap
        }
        secondaryColumn.updateLayoutParams<LinearLayout.LayoutParams> {
            width = if (wide) 0 else ViewGroup.LayoutParams.MATCH_PARENT
            weight = if (wide) 1f else 0f
            marginStart = columnGap
            marginEnd = 0
        }
        applyTvSettingsSimplification(view, mainActivity?.isTvMode == true)
    }

    private fun applyTvSettingsSimplification(view: View, tvMode: Boolean) {
        val visibility = if (tvMode) View.GONE else View.VISIBLE
        intArrayOf(
            R.id.settings_general_section_title,
            R.id.row_theme,
            R.id.divider_theme,
            R.id.row_language,
            R.id.divider_language,
            R.id.row_notifications,
            R.id.divider_notifications,
            R.id.row_check_update,
            R.id.divider_check_update,
            R.id.row_project_repo,
            R.id.divider_project_repo
        ).forEach { id ->
            view.findViewById<View>(id)?.visibility = visibility
        }
        if (tvMode) {
            view.findViewById<View>(R.id.settings_backup_section)?.visibility = View.GONE
        }
        val tvShortcutVisibility = if (tvMode) View.VISIBLE else View.GONE
        listOf(
            R.id.row_tv_subtitle_settings,
            R.id.divider_tv_subtitle_settings,
            R.id.row_tv_audio_settings,
            R.id.divider_tv_audio_settings,
            R.id.row_tv_sources_settings,
            R.id.divider_tv_sources_settings
        ).forEach { id ->
            view.findViewById<View>(id)?.visibility = tvShortcutVisibility
        }
        applyTvSettingsFocusDefaults(view, tvMode)
    }

    private fun applyTvSettingsFocusDefaults(view: View, tvMode: Boolean) {
        listOf(
            R.id.row_default_ratio,
            R.id.row_default_speed,
            R.id.row_tv_subtitle_settings,
            R.id.row_tv_audio_settings,
            R.id.row_tv_sources_settings,
            R.id.row_clear_cache,
            R.id.row_clear_history,
            R.id.row_license
        ).forEach { id ->
            val row = view.findViewById<View>(id) ?: return@forEach
            row.isFocusable = tvMode
        }
        if (tvMode) {
            applyTvSettingsFocusOrder(view)
            val defaultFocus = view.findViewById<View>(R.id.row_default_ratio)
            defaultFocus.post { defaultFocus.requestFocus() }
        }
    }

    private fun applyTvSettingsFocusOrder(view: View) {
        linkTvSettingsFocus(view, R.id.row_default_ratio, R.id.row_default_ratio, R.id.row_default_speed)
        linkTvSettingsFocus(view, R.id.row_default_speed, R.id.row_default_ratio, R.id.row_tv_subtitle_settings)
        linkTvSettingsFocus(view, R.id.row_tv_subtitle_settings, R.id.row_default_speed, R.id.row_tv_audio_settings)
        linkTvSettingsFocus(view, R.id.row_tv_audio_settings, R.id.row_tv_subtitle_settings, R.id.row_tv_sources_settings)
        linkTvSettingsFocus(view, R.id.row_tv_sources_settings, R.id.row_tv_audio_settings, R.id.row_clear_cache)
        linkTvSettingsFocus(view, R.id.row_clear_cache, R.id.row_tv_sources_settings, R.id.row_clear_history)
        linkTvSettingsFocus(view, R.id.row_clear_history, R.id.row_clear_cache, R.id.row_license)
        linkTvSettingsFocus(view, R.id.row_license, R.id.row_clear_history, R.id.row_license)
    }

    private fun linkTvSettingsFocus(view: View, rowId: Int, upId: Int, downId: Int) {
        val row = view.findViewById<View>(rowId) ?: return
        row.nextFocusUpId = upId
        row.nextFocusDownId = downId
    }

    private fun openTvSourcesSettings() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SourcesFragment())
            .addToBackStack("tv_settings_sources")
            .commit()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).roundToInt()

    private fun updateThemeLabel(tv: TextView) {
        tv.text = when (viewModel.themeMode) {
            ThemeMode.SYSTEM -> getString(R.string.settings_theme_system)
            ThemeMode.DARK -> getString(R.string.settings_theme_dark)
            ThemeMode.LIGHT -> getString(R.string.settings_theme_light)
        }
    }

    private fun updateLanguageLabel(tv: TextView) {
        tv.text = when (viewModel.language) {
            "zh" -> getString(R.string.settings_language_zh)
            "en" -> getString(R.string.settings_language_en)
            else -> getString(R.string.settings_language_system)
        }
    }

    private fun updateRatioLabel(tv: TextView) {
        val option = PlayerAspectRatioOptions.entries.firstOrNull { it.ratio == viewModel.defaultRatio }
        tv.text = option?.let { getString(it.labelRes) }.orEmpty()
    }

    private fun updateSpeedLabel(tv: TextView) {
        tv.text = "${viewModel.defaultSpeed}x"
    }

    private fun showDefaultRatioDialog(tvRatio: TextView) {
        showExclusiveSettingsDialog { onDismiss ->
            PlayerGlassSheetDialog.showSingleChoice(
                context = requireContext(),
                layoutInflater = layoutInflater,
                titleRes = R.string.settings_default_ratio,
                choices = PlayerAspectRatioOptions.entries.map { option ->
                    PlayerGlassSheetChoice(
                        value = option.ratio,
                        label = getString(option.labelRes),
                        selected = option.ratio == viewModel.defaultRatio
                    )
                },
                onDismiss = onDismiss
            ) { ratio ->
                viewModel.setDefaultRatio(ratio)
                updateRatioLabel(tvRatio)
            }
        }
    }

    private fun showDefaultSpeedDialog(tvSpeed: TextView) {
        val speeds = DefaultPlayerSettings.supportedSpeeds
        showExclusiveSettingsDialog { onDismiss ->
            PlayerGlassSheetDialog.showSingleChoice(
                context = requireContext(),
                layoutInflater = layoutInflater,
                titleRes = R.string.settings_default_speed,
                choices = speeds.map { speed ->
                    PlayerGlassSheetChoice(
                        value = speed,
                        label = "${speed}x",
                        selected = speed == viewModel.defaultSpeed
                    )
                },
                onDismiss = onDismiss
            ) { speed ->
                viewModel.setDefaultSpeed(speed)
                updateSpeedLabel(tvSpeed)
            }
        }
    }

    private fun bindBackupSection(view: View) {
        val section = view.findViewById<View>(R.id.settings_backup_section)
        val exportVisible = SettingsBackupUiPolicy.SETTINGS_EXPORT_ENTRY_VISIBLE
        val importVisible = SettingsBackupUiPolicy.SETTINGS_IMPORT_ENTRY_VISIBLE
        if (!exportVisible && !importVisible) {
            section.visibility = View.GONE
            return
        }
        section.visibility = View.VISIBLE

        val rowExport = view.findViewById<View>(R.id.row_export_settings)
        rowExport.visibility = if (exportVisible) View.VISIBLE else View.GONE
        rowExport.setOnClickListener {
            exportSettingsLauncher.launch(SettingsBackupSchema.SUGGESTED_FILENAME)
        }

        val rowImport = view.findViewById<View>(R.id.row_import_settings)
        rowImport.visibility = if (importVisible) View.VISIBLE else View.GONE
        rowImport.setOnClickListener {
            importSettingsLauncher.launch(arrayOf(SettingsBackupSchema.MIME_TYPE, "*/*"))
        }
    }

    private fun showExclusiveSettingsDialog(
        showDialog: (onDismiss: () -> Unit) -> Dialog
    ) {
        val current = activeSettingsDialog
        if (current?.isShowing == true) return
        var dialog: Dialog? = null
        val clearActiveDialog = {
            if (activeSettingsDialog === dialog) {
                activeSettingsDialog = null
            }
        }
        dialog = showDialog(clearActiveDialog)
        activeSettingsDialog = dialog
    }

    override fun onDestroyView() {
        activeSettingsDialog?.dismiss()
        activeSettingsDialog = null
        super.onDestroyView()
    }

}
