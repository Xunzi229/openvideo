package com.example.openvideo.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.example.openvideo.R
import com.example.openvideo.core.diagnostics.CrashLogger
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.AudioChannel
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SubtitleBgStyle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial

class PlayerSettingsDialog(
    context: Context,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val onRequestPickSubtitle: () -> Unit = {}
) : BottomSheetDialog(context) {

    private val primaryItems by lazy {
        listOf(
            PrimaryItem(SettingsPage.AUDIO, R.string.player_sheet_audio_track, R.drawable.ic_audio_track),
            PrimaryItem(SettingsPage.SUBTITLE, R.string.player_sheet_subtitles, R.drawable.ic_subtitles),
            PrimaryItem(SettingsPage.ASPECT, R.string.player_sheet_aspect_ratio, R.drawable.ic_aspect_ratio),
            PrimaryItem(SettingsPage.DISPLAY, R.string.player_sheet_display, R.drawable.ic_display_settings),
            PrimaryItem(SettingsPage.PLAYLIST, R.string.player_sheet_playlist, R.drawable.ic_playlist),
            PrimaryItem(SettingsPage.STREAM, R.string.player_sheet_stream, R.drawable.ic_stream),
            PrimaryItem(SettingsPage.INFO, R.string.player_sheet_info, R.drawable.ic_info),
            PrimaryItem(SettingsPage.SHARE, R.string.player_sheet_share, R.drawable.ic_share),
            PrimaryItem(SettingsPage.CUT, R.string.player_sheet_cut, R.drawable.ic_cut),
            PrimaryItem(SettingsPage.BOOKMARK, R.string.player_sheet_bookmark, R.drawable.ic_bookmark),
            PrimaryItem(SettingsPage.TUTORIAL, R.string.player_sheet_tutorial, R.drawable.ic_tutorial),
            PrimaryItem(SettingsPage.MORE, R.string.player_sheet_more, R.drawable.ic_more_vert)
        )
    }

    private val subtitleBgOptions = SubtitleBgStyle.entries.toTypedArray()
    private val subtitleEncodingOptions = arrayOf("auto", "UTF-8", "GBK", "GB2312", "Big5", "Shift_JIS", "EUC-KR")

    private lateinit var primaryPage: View
    private lateinit var detailPage: View
    private lateinit var grid: GridLayout
    private lateinit var primarySwitches: LinearLayout
    private lateinit var detailTitle: TextView
    private lateinit var detailContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_player_settings)
        setCanceledOnTouchOutside(true)

        window?.apply {
            val width = context.resources.displayMetrics.widthPixels
            val height = context.resources.displayMetrics.heightPixels
            val bounds = PlayerSettingsLayoutPolicy.panelBounds(width, height)
            setLayout(bounds.width, bounds.height)
            setGravity(PlayerSettingsLayoutPolicy.panelGravity(width, height))
            setDimAmount(0.55f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        runCatching {
            primaryPage = requireView(R.id.settings_primary_page)
            detailPage = requireView(R.id.settings_detail_page)
            grid = requireView(R.id.settings_grid)
            primarySwitches = requireView(R.id.settings_switches)
            detailTitle = requireView(R.id.settings_detail_title)
            detailContainer = requireView(R.id.settings_detail_container)

            requireView<ImageButton>(R.id.settings_detail_back).setOnClickListener {
                showPrimaryPage()
            }
            setupPrimaryGrid()
            setupPrimarySwitches()
            showPrimaryPage()
        }.onFailure { error ->
            CrashLogger.logPlayerError(context, error)
        }
    }

    private fun setupPrimaryGrid() {
        grid.removeAllViews()
        primaryItems.forEach { item ->
            grid.addView(createPrimaryItemView(item))
        }
    }

    private fun createPrimaryItemView(item: PrimaryItem): View {
        val cell = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = dp(86)
            isClickable = true
            isFocusable = true
            setPadding(4, 6, 4, 4)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            setOnClickListener { handlePrimaryClick(item) }
        }

        val iconWrap = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = context.getDrawable(
                if (item.page == SettingsPage.AUDIO) {
                    R.drawable.bg_player_settings_icon_selected
                } else {
                    R.drawable.bg_player_settings_icon
                }
            )
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        iconWrap.addView(ImageView(context).apply {
            setImageResource(item.iconRes)
            setColorFilter(context.getColor(if (item.page == SettingsPage.AUDIO) R.color.ov_accent_blue else android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
        })

        cell.addView(iconWrap)
        cell.addView(TextView(context).apply {
            text = context.getString(item.titleRes)
            gravity = Gravity.CENTER
            setTextColor(context.getColor(if (item.page == SettingsPage.AUDIO) R.color.ov_accent_blue else android.R.color.white))
            textSize = 12f
            maxLines = 1
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        })

        return cell
    }

    private fun setupPrimarySwitches() {
        primarySwitches.removeAllViews()
        addSwitchRow(
            parent = primarySwitches,
            title = context.getString(R.string.player_sheet_video_display),
            checked = playerPrefs.videoDisplayEnabled
        ) { checked ->
            playerPrefs.videoDisplayEnabled = checked
        }
        addSwitchRow(
            parent = primarySwitches,
            title = context.getString(R.string.player_sheet_keyboard_shortcuts),
            checked = playerPrefs.keyboardShortcuts
        ) { checked ->
            playerPrefs.keyboardShortcuts = checked
        }
        addSwitchRow(
            parent = primarySwitches,
            title = context.getString(R.string.player_sheet_bg_playback),
            checked = playerPrefs.bgAudio
        ) { checked ->
            playerPrefs.bgAudio = checked
        }
        addSwitchRow(
            parent = primarySwitches,
            title = context.getString(R.string.player_sheet_remember_position),
            checked = playerPrefs.rememberProgress
        ) { checked ->
            playerPrefs.rememberProgress = checked
        }
    }

    private fun handlePrimaryClick(item: PrimaryItem) {
        when (item.page) {
            SettingsPage.AUDIO,
            SettingsPage.SUBTITLE,
            SettingsPage.ASPECT,
            SettingsPage.DISPLAY,
            SettingsPage.MORE -> showDetailPage(item.page, context.getString(item.titleRes))
            SettingsPage.SHARE -> shareVideoTitle()
            else -> showUnavailable()
        }
    }

    private fun showPrimaryPage() {
        primaryPage.visibility = View.VISIBLE
        detailPage.visibility = View.GONE
    }

    private fun showDetailPage(page: SettingsPage, title: String) {
        detailTitle.text = title
        detailContainer.removeAllViews()
        when (page) {
            SettingsPage.AUDIO -> buildAudioPage()
            SettingsPage.SUBTITLE -> buildSubtitlePage()
            SettingsPage.ASPECT -> buildAspectPage()
            SettingsPage.DISPLAY -> buildDisplayPage()
            SettingsPage.MORE -> buildMorePage()
            else -> buildUnavailablePage()
        }
        primaryPage.visibility = View.GONE
        detailPage.visibility = View.VISIBLE
    }

    private fun buildAudioPage() {
        addRadioRow(
            title = context.getString(R.string.player_sheet_audio_track_english),
            checked = true
        ) {}
        addRadioRow(
            title = context.getString(R.string.player_sheet_disable),
            checked = false,
            enabled = false
        ) {}
        addCheckboxRow(
            title = context.getString(R.string.player_sheet_software_audio_decoder),
            checked = playerPrefs.softwareAudioDecoder
        ) { checked ->
            playerPrefs.softwareAudioDecoder = checked
        }
        addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.player_sheet_enable),
            checked = !playerPrefs.pauseOnExit
        ) { checked ->
            playerPrefs.pauseOnExit = !checked
        }
        addRadioRow(
            title = context.getString(R.string.player_sheet_stereo_mode),
            checked = playerPrefs.audioChannel == AudioChannel.STEREO
        ) {
            playerPrefs.audioChannel = AudioChannel.STEREO
        }
        addRadioRow(
            title = context.getString(R.string.player_sheet_sync),
            checked = playerPrefs.audioDelay == 0
        ) {
            playerPrefs.audioDelay = 0
        }
        addCheckboxRow(
            title = context.getString(R.string.player_sheet_av_sync),
            checked = playerPrefs.audioSyncEnabled
        ) { checked ->
            playerPrefs.audioSyncEnabled = checked
        }
    }

    private fun buildSubtitlePage() {
        addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.player_sheet_subtitle_switch),
            checked = playerPrefs.subtitlesEnabled
        ) { checked ->
            playerPrefs.subtitlesEnabled = checked
        }
        addActionRow(context.getString(R.string.player_sheet_select_subtitle_file)) {
            dismiss()
            onRequestPickSubtitle()
        }
        addSeekRow(
            title = context.getString(R.string.player_sheet_subtitle_delay),
            min = -5000,
            maxValue = 5000,
            value = 0,
            label = { "${it}ms" }
        ) {}
        addSeekRow(
            title = context.getString(R.string.settings_subtitle_size),
            min = 12,
            maxValue = 36,
            value = playerPrefs.subtitleSize,
            label = { "${it}sp" }
        ) { value ->
            playerPrefs.subtitleSize = value
        }
        addChoiceRow(
            title = context.getString(R.string.settings_subtitle_color),
            value = subtitleColorOptionFor(playerPrefs.subtitleColor).label,
            options = subtitleColorOptions.map { it.label }
        ) { selected ->
            subtitleColorOptions.firstOrNull { it.label == selected }?.let {
                playerPrefs.subtitleColor = it.color
            }
        }
        addChoiceRow(
            title = context.getString(R.string.settings_subtitle_bg),
            value = subtitleBgLabel(playerPrefs.subtitleBgStyle),
            options = subtitleBgOptions.map(::subtitleBgLabel)
        ) { selected ->
            subtitleBgOptions.firstOrNull { subtitleBgLabel(it) == selected }?.let {
                playerPrefs.subtitleBgStyle = it
            }
        }
        addChoiceRow(
            title = context.getString(R.string.settings_subtitle_encoding),
            value = subtitleEncodingLabel(playerPrefs.subtitleEncoding),
            options = subtitleEncodingOptions.map(::subtitleEncodingLabel)
        ) { selected ->
            val index = subtitleEncodingOptions.map(::subtitleEncodingLabel).indexOf(selected)
            if (index >= 0) playerPrefs.subtitleEncoding = subtitleEncodingOptions[index]
        }
    }

    private fun buildAspectPage() {
        addAspectRow(context.getString(R.string.player_sheet_fit_screen), AspectRatio.FIT)
        addAspectRow(context.getString(R.string.player_sheet_fill_screen), AspectRatio.FILL)
        addAspectRow(context.getString(R.string.settings_ratio_16_9), AspectRatio.RATIO_16_9)
        addAspectRow(context.getString(R.string.settings_ratio_4_3), AspectRatio.RATIO_4_3)
        addAspectRow(context.getString(R.string.player_sheet_original_ratio), AspectRatio.FIT)
        addAspectRow(context.getString(R.string.settings_ratio_crop), AspectRatio.CROP)
        addAspectRow(context.getString(R.string.settings_ratio_stretch), AspectRatio.STRETCH)
    }

    private fun buildDisplayPage() {
        addSeekRow(
            title = context.getString(R.string.player_sheet_brightness),
            min = -100,
            maxValue = 100,
            value = playerPrefs.brightnessAdjustment,
            label = { "$it%" }
        ) { value ->
            playerPrefs.brightnessAdjustment = value
            playerManager.setBrightness(value / 100f)
        }
        addSeekRow(
            title = context.getString(R.string.player_sheet_contrast),
            min = -100,
            maxValue = 100,
            value = playerPrefs.contrastAdjustment,
            label = { "$it%" }
        ) { value ->
            playerPrefs.contrastAdjustment = value
            playerManager.setContrast(value / 100f)
        }
        addSeekRow(
            title = context.getString(R.string.player_sheet_saturation),
            min = -100,
            maxValue = 100,
            value = playerPrefs.saturationAdjustment,
            label = { "$it%" }
        ) { value ->
            playerPrefs.saturationAdjustment = value
        }
        addChoiceRow(
            title = context.getString(R.string.settings_rotation),
            value = "${playerPrefs.rotation}°",
            options = listOf("0°", "90°", "180°", "270°")
        ) { selected ->
            playerPrefs.rotation = selected.removeSuffix("°").toIntOrNull() ?: 0
        }
        addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.settings_mirror),
            checked = playerPrefs.mirror
        ) { checked ->
            playerPrefs.mirror = checked
        }
        addChoiceRow(
            title = context.getString(R.string.player_sheet_progress_style),
            value = progressStyleLabel(playerPrefs.progressStyle),
            options = listOf(
                context.getString(R.string.player_sheet_default),
                context.getString(R.string.player_sheet_modern),
                context.getString(R.string.player_sheet_thin)
            )
        ) { selected ->
            playerPrefs.progressStyle = when (selected) {
                context.getString(R.string.player_sheet_modern) -> "modern"
                context.getString(R.string.player_sheet_thin) -> "thin"
                else -> "default"
            }
        }
        addSeekRow(
            title = context.getString(R.string.player_sheet_controls_opacity),
            min = 30,
            maxValue = 100,
            value = playerPrefs.controlsOpacity,
            label = { "$it%" }
        ) { value ->
            playerPrefs.controlsOpacity = value
        }
    }

    private fun buildMorePage() {
        addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.settings_skip_intro_outro),
            checked = playerPrefs.skipIntroOutro
        ) { checked ->
            playerPrefs.skipIntroOutro = checked
        }
        addChoiceRow(
            title = context.getString(R.string.settings_playback_speed),
            value = "${playerPrefs.speed}x",
            options = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")
        ) { selected ->
            val speed = selected.removeSuffix("x").toFloatOrNull() ?: 1f
            playerPrefs.speed = speed
            viewModel.setSpeed(speed, PlayerPlaybackSettings.pitchFor(speed, playerPrefs.speedPreservePitch))
        }
        addActionRow(context.getString(R.string.settings_reset_defaults)) {
            playerPrefs.resetToDefaults()
            showPrimaryPage()
            setupPrimarySwitches()
        }
    }

    private fun buildUnavailablePage() {
        detailContainer.addView(TextView(context).apply {
            text = context.getString(R.string.player_sheet_not_available)
            setTextColor(Color.rgb(176, 176, 176))
            textSize = 15f
            setPadding(0, dp(24), 0, dp(24))
        })
    }

    private fun addAspectRow(title: String, ratio: AspectRatio) {
        addRadioRow(
            title = title,
            checked = playerPrefs.aspectRatio == ratio
        ) {
            playerPrefs.aspectRatio = ratio
            viewModel.setAspectRatio(ratio)
            rebuildCurrentDetail(SettingsPage.ASPECT, context.getString(R.string.player_sheet_aspect_ratio))
        }
    }

    private fun rebuildCurrentDetail(page: SettingsPage, title: String) {
        showDetailPage(page, title)
    }

    private fun addSwitchRow(
        parent: LinearLayout,
        title: String,
        checked: Boolean,
        onChanged: (Boolean) -> Unit
    ) {
        parent.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(52)
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(SwitchMaterial(context).apply {
                isChecked = checked
                setOnCheckedChangeListener { _: CompoundButton, value: Boolean -> onChanged(value) }
            })
        })
        addDivider(parent)
    }

    private fun addRadioRow(
        title: String,
        checked: Boolean,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        detailContainer.addView(RadioButton(context).apply {
            text = title
            isChecked = checked
            isEnabled = enabled
            buttonTintList = context.getColorStateList(R.color.nav_item_tint)
            setTextColor(if (enabled) Color.WHITE else Color.rgb(85, 85, 85))
            textSize = 15f
            minHeight = dp(52)
            setOnClickListener { if (enabled) onClick() }
        })
        addDivider(detailContainer)
    }

    private fun addCheckboxRow(
        title: String,
        checked: Boolean,
        enabled: Boolean = true,
        onChanged: (Boolean) -> Unit
    ) {
        detailContainer.addView(CheckBox(context).apply {
            text = title
            isChecked = checked
            isEnabled = enabled
            buttonTintList = context.getColorStateList(R.color.nav_item_tint)
            setTextColor(if (enabled) Color.WHITE else Color.rgb(85, 85, 85))
            textSize = 15f
            minHeight = dp(52)
            setOnCheckedChangeListener { _, value -> onChanged(value) }
        })
        addDivider(detailContainer)
    }

    private fun addActionRow(title: String, onClick: () -> Unit) {
        detailContainer.addView(TextView(context).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(52)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        })
        addDivider(detailContainer)
    }

    private fun addChoiceRow(
        title: String,
        value: String,
        options: List<String>,
        onSelected: (String) -> Unit
    ) {
        detailContainer.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(54)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showChoicePopup(title, options, value) { selected ->
                    onSelected(selected)
                    (getChildAt(1) as? TextView)?.text = selected
                }
            }
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = value
                setTextColor(context.getColor(R.color.ov_accent_blue))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(5), dp(12), dp(5))
                background = context.getDrawable(R.drawable.bg_player_settings_value)
            })
        })
        addDivider(detailContainer)
    }

    private fun addSeekRow(
        title: String,
        min: Int,
        maxValue: Int,
        value: Int,
        label: (Int) -> String,
        onChanged: (Int) -> Unit
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val valueView = TextView(context).apply {
            text = label(value)
            setTextColor(context.getColor(R.color.ov_accent_blue))
            textSize = 14f
        }
        row.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(valueView)
        })
        row.addView(SeekBar(context).apply {
            max = maxValue - min
            progress = (value - min).coerceIn(0, maxValue - min)
            progressTintList = context.getColorStateList(R.color.ov_accent_blue)
            thumbTintList = context.getColorStateList(R.color.ov_accent_blue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val next = min + progress
                    valueView.text = label(next)
                    onChanged(next)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        })
        detailContainer.addView(row)
        addDivider(detailContainer)
    }

    private fun showChoicePopup(
        title: String,
        options: List<String>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        val popup = BottomSheetDialog(context)
        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = context.getDrawable(R.drawable.bg_player_settings_sheet)
            setPadding(dp(20), dp(16), dp(20), dp(20))
        }
        list.addView(TextView(context).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(12))
        })
        options.forEach { option ->
            list.addView(RadioButton(context).apply {
                text = option
                isChecked = option == selected
                buttonTintList = context.getColorStateList(R.color.nav_item_tint)
                setTextColor(if (option == selected) context.getColor(R.color.ov_accent_blue) else Color.WHITE)
                textSize = 15f
                minHeight = dp(48)
                setOnClickListener {
                    onSelected(option)
                    popup.dismiss()
                }
            })
        }
        popup.setContentView(list)
        popup.show()
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(View(context).apply {
            background = context.getDrawable(R.drawable.bg_player_settings_divider)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
        })
    }

    private fun shareVideoTitle() {
        val title = viewModel.uiState.value.title.ifBlank { context.getString(R.string.app_name) }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, title)
                },
                context.getString(R.string.player_sheet_share)
            )
        )
    }

    private fun showUnavailable() {
        Toast.makeText(context, R.string.player_sheet_not_available, Toast.LENGTH_SHORT).show()
    }

    private fun progressStyleLabel(value: String): String = when (value) {
        "modern" -> context.getString(R.string.player_sheet_modern)
        "thin" -> context.getString(R.string.player_sheet_thin)
        else -> context.getString(R.string.player_sheet_default)
    }

    private fun subtitleBgLabel(value: SubtitleBgStyle): String = when (value) {
        SubtitleBgStyle.NONE -> context.getString(R.string.settings_subtitle_bg_none)
        SubtitleBgStyle.SEMI_TRANSPARENT -> context.getString(R.string.settings_subtitle_bg_semi)
        SubtitleBgStyle.OPAQUE -> context.getString(R.string.settings_subtitle_bg_opaque)
    }

    private fun subtitleEncodingLabel(value: String): String =
        if (value == "auto") context.getString(R.string.settings_encoding_auto) else value

    private data class SubtitleColorOption(val label: String, val color: Int)

    private val subtitleColorOptions: Array<SubtitleColorOption> by lazy {
        arrayOf(
            SubtitleColorOption("白色", 0xFFFFFFFF.toInt()),
            SubtitleColorOption("黄色", 0xFFFFEB3B.toInt()),
            SubtitleColorOption("绿色", 0xFF8BC34A.toInt()),
            SubtitleColorOption("蓝色", 0xFF64B5F6.toInt())
        )
    }

    private fun subtitleColorOptionFor(color: Int): SubtitleColorOption =
        subtitleColorOptions.firstOrNull { it.color == color } ?: subtitleColorOptions.first()

    private fun <T : View> requireView(id: Int): T =
        findViewById<T>(id) ?: error("Missing player settings view: $id")

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private data class PrimaryItem(
        val page: SettingsPage,
        val titleRes: Int,
        val iconRes: Int
    )

    private enum class SettingsPage {
        AUDIO,
        SUBTITLE,
        ASPECT,
        DISPLAY,
        PLAYLIST,
        STREAM,
        INFO,
        SHARE,
        CUT,
        BOOKMARK,
        TUTORIAL,
        MORE
    }
}
