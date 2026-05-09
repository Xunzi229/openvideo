package com.example.openvideo.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.openvideo.R
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

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

        updateThemeLabel(tvTheme)
        updateLanguageLabel(tvLanguage)
        updateRatioLabel(tvRatio)
        updateSpeedLabel(tvSpeed)

        view.findViewById<View>(R.id.row_theme).setOnClickListener {
            val modes = ThemeMode.entries
            val next = (modes.indexOf(viewModel.themeMode) + 1) % modes.size
            viewModel.setThemeMode(modes[next])
            updateThemeLabel(tvTheme)
        }

        view.findViewById<View>(R.id.row_language).setOnClickListener {
            val langs = listOf("system", "zh", "en")
            val next = (langs.indexOf(viewModel.language) + 1) % langs.size
            viewModel.setLanguage(langs[next])
            requireActivity().recreate()
        }

        view.findViewById<View>(R.id.row_default_ratio).setOnClickListener {
            val ratios = AspectRatio.entries
            val next = (ratios.indexOf(viewModel.defaultRatio) + 1) % ratios.size
            viewModel.setDefaultRatio(ratios[next])
            updateRatioLabel(tvRatio)
        }

        view.findViewById<View>(R.id.row_default_speed).setOnClickListener {
            val speeds = DefaultPlayerSettings.supportedSpeeds
            val next = (speeds.indexOf(viewModel.defaultSpeed) + 1) % speeds.size
            viewModel.setDefaultSpeed(speeds[next])
            updateSpeedLabel(tvSpeed)
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cacheSize.collect { tvCacheSize.text = it }
                }
                launch {
                    viewModel.historyCount.collect { tvHistoryCount.text = it.toString() }
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
}
