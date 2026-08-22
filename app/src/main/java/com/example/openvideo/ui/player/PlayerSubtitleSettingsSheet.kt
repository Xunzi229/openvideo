package com.example.openvideo.ui.player

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SubtitleBgStyle
import com.example.openvideo.core.subtitle.OnlineSubtitlePrivacyPolicy
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionSheet
import com.example.openvideo.core.ui.AppleActionStyle
import com.example.openvideo.core.ui.AppleAlertDialog
import com.example.openvideo.core.ui.AppleHud
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlayerSubtitleSettingsSheet : BaseSettingsSheet() {
    override val layoutResId: Int = R.layout.activity_player_subtitle_settings

    override fun settingsSheetPanelRootId(): Int = R.id.subtitle_settings_panel_root

    override fun settingsSheetPlayerPrefs(): PlayerPrefs = playerPrefs

    override fun settingsSheetDefaultFocusId(): Int = R.id.btn_load_subtitle

    private val viewModel: PlayerViewModel by activityViewModels()

    @Inject lateinit var playerPrefs: PlayerPrefs

    var onDismissListener: (() -> Unit)? = null

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
        onDismissListener = null
    }

    private val pickSubtitleLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // store into prefs so PlayerActivity can pick it up
            playerPrefs.externalSubtitleUri = uri.toString()
            dismiss()
        }
    }

    private val pickSecondarySubtitleLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        viewModel.loadSecondarySubtitles(
            uriString = uri.toString(),
            videoPath = viewModel.currentVideoSource()
        ) { decision ->
            val messageRes = PlayerSubtitleLoadToastPolicy.messageRes(decision.toastKind)
            if (messageRes != null) {
                AppleHud.show(requireContext(), messageRes)
            }
        }
    }

    private val exportSubtitleLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-subrip")
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            when (viewModel.writeCurrentSubtitleUtf8ExportTo(requireContext(), uri)) {
                is PlayerViewModel.SubtitleExportResult.Success ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_success)
                is PlayerViewModel.SubtitleExportResult.NoSubtitles ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_no_subtitles)
                is PlayerViewModel.SubtitleExportResult.NoDelay ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_no_delay)
                is PlayerViewModel.SubtitleExportResult.OriginalOverwriteBlocked ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_original_blocked)
                is PlayerViewModel.SubtitleExportResult.OpenStreamFailed,
                is PlayerViewModel.SubtitleExportResult.WriteFailed ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_failed)
            }
        }
    }

    private val delayCorrectionExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-subrip")
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            when (viewModel.writeCurrentSubtitleDelayCorrectionExportTo(requireContext(), uri)) {
                is PlayerViewModel.SubtitleExportResult.Success ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_success)
                is PlayerViewModel.SubtitleExportResult.NoSubtitles ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_no_subtitles)
                is PlayerViewModel.SubtitleExportResult.NoDelay ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_no_delay)
                is PlayerViewModel.SubtitleExportResult.OriginalOverwriteBlocked ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_original_blocked)
                is PlayerViewModel.SubtitleExportResult.OpenStreamFailed,
                is PlayerViewModel.SubtitleExportResult.WriteFailed ->
                    AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_failed)
            }
        }
    }

    private fun showOnlineSubtitlePrivacyNotice() {
        val notice = OnlineSubtitlePrivacyPolicy.firstUseNotice()
        if (!notice.requiresUserConfirmation) return
        AppleAlertDialog.show(
            context = requireContext(),
            title = getString(R.string.player_settings_online_subtitle_privacy_title),
            message = getString(R.string.player_settings_online_subtitle_privacy_message),
            actions = listOf(
                AppleAction(getString(R.string.action_cancel), AppleActionStyle.CANCEL),
                AppleAction(getString(R.string.action_ok)) {
                    AppleHud.show(requireContext(), R.string.player_settings_online_subtitle_search_pending)
                }
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLoad = view.findViewById<MaterialButton>(R.id.btn_load_subtitle)
        val btnLoadSecondary = view.findViewById<MaterialButton>(R.id.btn_load_secondary_subtitle)
        val btnOnlineSubtitleSearch = view.findViewById<MaterialButton>(R.id.btn_online_subtitle_search)
        val btnExportSubtitle = view.findViewById<MaterialButton>(R.id.btn_export_subtitle_utf8)
        val btnExportDelayCorrected = view.findViewById<MaterialButton>(R.id.btn_export_subtitle_delay_corrected)
        val tvSize = view.findViewById<TextView>(R.id.tv_subtitle_size_value)
        val sbSize = view.findViewById<SeekBar>(R.id.sb_subtitle_size)
        val tvBg = view.findViewById<TextView>(R.id.tv_subtitle_bg_value)
        val sbPosition = view.findViewById<SeekBar>(R.id.sb_subtitle_position)
        val tvEncoding = view.findViewById<TextView>(R.id.tv_encoding_value)
        val tvPreview = view.findViewById<TextView>(R.id.tv_subtitle_preview)
        val tvSecondaryPreview = view.findViewById<TextView>(R.id.tv_secondary_subtitle_preview)
        val tvSecondarySize = view.findViewById<TextView>(R.id.tv_secondary_subtitle_size_value)
        val sbSecondarySize = view.findViewById<SeekBar>(R.id.sb_secondary_subtitle_size)
        val tvSecondaryBg = view.findViewById<TextView>(R.id.tv_secondary_subtitle_bg_value)
        val sbSecondaryPosition = view.findViewById<SeekBar>(R.id.sb_secondary_subtitle_position)
        val tvSubtitleInfo = view.findViewById<TextView>(R.id.tv_subtitle_info_summary)
        val tvPrimaryLanguage = view.findViewById<TextView>(R.id.tv_subtitle_primary_language_value)
        val tvSecondaryLanguage = view.findViewById<TextView>(R.id.tv_subtitle_secondary_language_value)
        val swPreferBilingual = view.findViewById<SwitchMaterial>(R.id.switch_subtitle_prefer_bilingual)
        val swSecondarySubtitleEnabled = view.findViewById<SwitchMaterial>(R.id.switch_secondary_subtitle_enabled)
        val tvSubtitleDelay = view.findViewById<TextView>(R.id.tv_subtitle_delay_value)
        val btnSubtitleDelayMinus = view.findViewById<MaterialButton>(R.id.btn_subtitle_delay_minus)
        val btnSubtitleDelayPlus = view.findViewById<MaterialButton>(R.id.btn_subtitle_delay_plus)
        val btnSubtitleDelayReset = view.findViewById<MaterialButton>(R.id.btn_subtitle_delay_reset)

        fun updateSubtitlePreview(position: Float = playerPrefs.subtitlePosition) {
            PlayerSubtitleSettingsPreviewPolicy.apply(
                preview = tvPreview,
                sampleText = getString(R.string.player_settings_subtitle_preview_sample),
                sizeSp = playerPrefs.subtitleSize,
                textColor = playerPrefs.subtitleColor,
                bgStyle = playerPrefs.subtitleBgStyle,
                position = position
            )
        }

        fun updateSecondarySubtitlePreview(position: Float = playerPrefs.secondarySubtitlePosition) {
            PlayerSubtitleSettingsPreviewPolicy.apply(
                preview = tvSecondaryPreview,
                sampleText = getString(R.string.player_settings_secondary_subtitle_preview_sample),
                sizeSp = playerPrefs.secondarySubtitleSize,
                textColor = playerPrefs.secondarySubtitleColor,
                bgStyle = playerPrefs.secondarySubtitleBgStyle,
                position = position
            )
        }

        fun updateSubtitleDelayText() {
            tvSubtitleDelay.text = getString(R.string.player_settings_unit_ms, playerPrefs.subtitleDelayMs)
        }

        fun updateSubtitleInfoText() {
            tvSubtitleInfo.text = PlayerSubtitleInfoUiPolicy.summaryText(
                info = viewModel.currentSubtitleInfo(),
                labels = PlayerSubtitleInfoUiPolicy.Labels(
                    noLoadedSubtitle = getString(R.string.player_settings_subtitle_info_empty),
                    sourcePrefix = getString(R.string.player_settings_subtitle_info_source),
                    linesPrefix = getString(R.string.player_settings_subtitle_info_lines),
                    timeRangePrefix = getString(R.string.player_settings_subtitle_info_time_range),
                    encodingPrefix = getString(R.string.player_settings_subtitle_info_encoding),
                    styledLinesPrefix = getString(R.string.player_settings_subtitle_info_styled_lines)
                )
            )
        }

        btnLoad.setOnClickListener {
            pickSubtitleLauncher.launch(arrayOf("*/*"))
        }
        btnLoadSecondary.setOnClickListener {
            pickSecondarySubtitleLauncher.launch(arrayOf("*/*"))
        }
        btnOnlineSubtitleSearch.setOnClickListener {
            showOnlineSubtitlePrivacyNotice()
        }
        btnExportSubtitle.setOnClickListener {
            if (!viewModel.hasCurrentSubtitles()) {
                AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_no_subtitles)
                return@setOnClickListener
            }
            exportSubtitleLauncher.launch(viewModel.suggestedSubtitleExportFileName())
        }
        btnExportDelayCorrected.setOnClickListener {
            if (!viewModel.hasCurrentSubtitles()) {
                AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_no_subtitles)
                return@setOnClickListener
            }
            if (playerPrefs.subtitleDelayMs == 0) {
                AppleHud.show(requireContext(), R.string.player_settings_subtitle_export_no_delay)
                return@setOnClickListener
            }
            delayCorrectionExportLauncher.launch(viewModel.suggestedSubtitleDelayCorrectionExportFileName())
        }
        btnSubtitleDelayMinus.setOnClickListener {
            playerPrefs.subtitleDelayMs -= 500
            updateSubtitleDelayText()
        }
        btnSubtitleDelayPlus.setOnClickListener {
            playerPrefs.subtitleDelayMs += 500
            updateSubtitleDelayText()
        }
        btnSubtitleDelayReset.setOnClickListener {
            playerPrefs.subtitleDelayMs = 0
            updateSubtitleDelayText()
        }
        val languageKeys = arrayOf("unknown", "chinese", "english", "bilingual", "japanese", "korean")
        fun languageLabel(key: String): String = when (key) {
            "chinese" -> getString(R.string.player_settings_subtitle_language_chinese)
            "english" -> getString(R.string.player_settings_subtitle_language_english)
            "bilingual" -> getString(R.string.player_settings_subtitle_language_bilingual)
            "japanese" -> getString(R.string.player_settings_subtitle_language_japanese)
            "korean" -> getString(R.string.player_settings_subtitle_language_korean)
            else -> getString(R.string.player_settings_subtitle_language_any)
        }
        fun updateSubtitleLanguageText() {
            tvPrimaryLanguage.text = languageLabel(playerPrefs.subtitlePrimaryLanguage)
            tvSecondaryLanguage.text = languageLabel(playerPrefs.subtitleSecondaryLanguage)
        }
        tvPrimaryLanguage.setOnClickListener {
            AppleActionSheet.showPicker(
                context = requireContext(),
                title = getString(R.string.player_settings_subtitle_language_title),
                items = languageKeys.map { it to languageLabel(it) },
                selected = playerPrefs.subtitlePrimaryLanguage
            ) { key ->
                playerPrefs.subtitlePrimaryLanguage = key
                updateSubtitleLanguageText()
            }
        }
        tvSecondaryLanguage.setOnClickListener {
            AppleActionSheet.showPicker(
                context = requireContext(),
                title = getString(R.string.player_settings_subtitle_language_title),
                items = languageKeys.map { it to languageLabel(it) },
                selected = playerPrefs.subtitleSecondaryLanguage
            ) { key ->
                playerPrefs.subtitleSecondaryLanguage = key
                updateSubtitleLanguageText()
            }
        }
        swPreferBilingual.isChecked = playerPrefs.subtitlePreferBilingual
        swPreferBilingual.setOnCheckedChangeListener { _, checked ->
            playerPrefs.subtitlePreferBilingual = checked
        }
        swSecondarySubtitleEnabled.isChecked = viewModel.uiState.value.dualSubtitles.secondary.enabled
        swSecondarySubtitleEnabled.setOnCheckedChangeListener { _, checked ->
            viewModel.setSecondarySubtitlesEnabled(checked)
        }
        updateSubtitleDelayText()
        updateSubtitleInfoText()
        updateSubtitleLanguageText()
        updateSubtitlePreview()
        updateSecondarySubtitlePreview()

        val currentSize = playerPrefs.subtitleSize
        sbSize.progress = (currentSize - 14) / 2
        tvSize.text = "${currentSize}sp"
        var pendingSubtitleSize = currentSize
        sbSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val size = 14 + progress * 2
                tvSize.text = "${size}sp"
                if (fromUser) pendingSubtitleSize = size
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.subtitleSize = pendingSubtitleSize
                updateSubtitlePreview()
            }
        })

        PlayerSubtitleColorSwatchBinder.bind(
            root = view.findViewById(R.id.subtitle_color_swatch_row),
            playerPrefs = playerPrefs,
            density = resources.displayMetrics.density,
            onColorChanged = { updateSubtitlePreview() }
        )

        fun subtitleBgLabel(style: SubtitleBgStyle): String = when (style) {
            SubtitleBgStyle.NONE -> getString(R.string.settings_subtitle_bg_none)
            SubtitleBgStyle.SEMI_TRANSPARENT -> getString(R.string.settings_subtitle_bg_semi)
            SubtitleBgStyle.OPAQUE -> getString(R.string.settings_subtitle_bg_opaque)
        }
        fun updateBgText() {
            tvBg.text = subtitleBgLabel(playerPrefs.subtitleBgStyle)
        }
        updateBgText()
        tvBg.setOnClickListener {
            AppleActionSheet.showPicker(
                context = requireContext(),
                title = getString(R.string.settings_subtitle_bg),
                items = SubtitleBgStyle.entries.map { it to subtitleBgLabel(it) },
                selected = playerPrefs.subtitleBgStyle
            ) { style ->
                playerPrefs.subtitleBgStyle = style
                updateBgText()
                updateSubtitlePreview()
            }
        }

        var pendingSubtitlePosition = playerPrefs.subtitlePosition
        sbPosition.progress = (pendingSubtitlePosition * 100).toInt()
        sbPosition.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pendingSubtitlePosition = progress / 100f
                    updateSubtitlePreview(pendingSubtitlePosition)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.subtitlePosition = pendingSubtitlePosition
                updateSubtitlePreview(pendingSubtitlePosition)
            }
        })

        val currentSecondarySize = playerPrefs.secondarySubtitleSize
        sbSecondarySize.progress = (currentSecondarySize - 14) / 2
        tvSecondarySize.text = "${currentSecondarySize}sp"
        var pendingSecondarySubtitleSize = currentSecondarySize
        sbSecondarySize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val size = 14 + progress * 2
                tvSecondarySize.text = "${size}sp"
                if (fromUser) pendingSecondarySubtitleSize = size
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.secondarySubtitleSize = pendingSecondarySubtitleSize
                updateSecondarySubtitlePreview()
            }
        })

        PlayerSubtitleColorSwatchBinder.bindSecondary(
            root = view.findViewById(R.id.secondary_subtitle_color_swatch_row),
            playerPrefs = playerPrefs,
            density = resources.displayMetrics.density,
            onColorChanged = { updateSecondarySubtitlePreview() }
        )

        fun updateSecondaryBgText() {
            tvSecondaryBg.text = subtitleBgLabel(playerPrefs.secondarySubtitleBgStyle)
        }
        updateSecondaryBgText()
        tvSecondaryBg.setOnClickListener {
            AppleActionSheet.showPicker(
                context = requireContext(),
                title = getString(R.string.settings_subtitle_bg),
                items = SubtitleBgStyle.entries.map { it to subtitleBgLabel(it) },
                selected = playerPrefs.secondarySubtitleBgStyle
            ) { style ->
                playerPrefs.secondarySubtitleBgStyle = style
                updateSecondaryBgText()
                updateSecondarySubtitlePreview()
            }
        }

        var pendingSecondarySubtitlePosition = playerPrefs.secondarySubtitlePosition
        sbSecondaryPosition.progress = (pendingSecondarySubtitlePosition * 100).toInt()
        sbSecondaryPosition.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pendingSecondarySubtitlePosition = progress / 100f
                    updateSecondarySubtitlePreview(pendingSecondarySubtitlePosition)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.secondarySubtitlePosition = pendingSecondarySubtitlePosition
                updateSecondarySubtitlePreview(pendingSecondarySubtitlePosition)
            }
        })

        val encodings = arrayOf("auto", "UTF-8", "GBK", "GB2312", "Big5", "Shift_JIS", "EUC-KR")
        fun encodingLabel(encoding: String): String =
            if (encoding == "auto") getString(R.string.settings_encoding_auto) else encoding
        fun updateEncText() {
            tvEncoding.text = encodingLabel(playerPrefs.subtitleEncoding)
        }
        updateEncText()
        tvEncoding.setOnClickListener {
            AppleActionSheet.showPicker(
                context = requireContext(),
                title = getString(R.string.settings_subtitle_encoding),
                items = encodings.map { it to encodingLabel(it) },
                selected = playerPrefs.subtitleEncoding
            ) { encoding ->
                playerPrefs.subtitleEncoding = encoding
                updateEncText()
            }
        }
    }
}
