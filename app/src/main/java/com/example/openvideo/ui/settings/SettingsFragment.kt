package com.example.openvideo.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.openvideo.R
import com.example.openvideo.core.prefs.AspectRatio
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.example.openvideo.core.prefs.ThemeMode
import com.example.openvideo.ui.player.PlayerGlassSheetChoice
import com.example.openvideo.ui.player.PlayerGlassSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by activityViewModels()

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

        view.findViewById<View>(R.id.row_clear_cache).setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_clear_cache_title)
                .setMessage(R.string.dialog_clear_cache_message)
                .setPositiveButton(R.string.action_clear) { _, _ -> viewModel.clearCache() }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }

        view.findViewById<View>(R.id.row_clear_history).setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_clear_history_title)
                .setMessage(R.string.dialog_clear_history_message)
                .setPositiveButton(R.string.action_clear) { _, _ -> viewModel.clearHistory() }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }

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
        tv.text = when (viewModel.defaultRatio) {
            AspectRatio.FIT -> getString(R.string.settings_ratio_fit)
            AspectRatio.FILL -> getString(R.string.settings_ratio_fill)
            AspectRatio.CROP -> getString(R.string.settings_ratio_crop)
            AspectRatio.STRETCH -> getString(R.string.settings_ratio_stretch)
            AspectRatio.RATIO_4_3 -> "4:3"
            AspectRatio.RATIO_16_9 -> "16:9"
        }
    }

    private fun updateSpeedLabel(tv: TextView) {
        tv.text = "${viewModel.defaultSpeed}x"
    }

    private fun showDefaultRatioDialog(tvRatio: TextView) {
        val ratios = AspectRatio.entries
        PlayerGlassSheetDialog.showSingleChoice(
            context = requireContext(),
            layoutInflater = layoutInflater,
            titleRes = R.string.settings_default_ratio,
            choices = ratios.map { ratio ->
                PlayerGlassSheetChoice(
                    value = ratio,
                    label = ratioLabel(ratio),
                    selected = ratio == viewModel.defaultRatio
                )
            }
        ) { ratio ->
            viewModel.setDefaultRatio(ratio)
            updateRatioLabel(tvRatio)
        }
    }

    private fun showDefaultSpeedDialog(tvSpeed: TextView) {
        val speeds = DefaultPlayerSettings.supportedSpeeds
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
            }
        ) { speed ->
            viewModel.setDefaultSpeed(speed)
            updateSpeedLabel(tvSpeed)
        }
    }

    private fun ratioLabel(ratio: AspectRatio): String = when (ratio) {
        AspectRatio.FIT -> getString(R.string.settings_ratio_fit)
        AspectRatio.FILL -> getString(R.string.settings_ratio_fill)
        AspectRatio.CROP -> getString(R.string.settings_ratio_crop)
        AspectRatio.STRETCH -> getString(R.string.settings_ratio_stretch)
        AspectRatio.RATIO_4_3 -> "4:3"
        AspectRatio.RATIO_16_9 -> "16:9"
    }
}
