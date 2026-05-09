package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
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
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.AudioChannel
import com.example.openvideo.core.prefs.DoubleTapAction
import com.example.openvideo.core.prefs.GestureAction
import com.example.openvideo.core.prefs.LongPressAction
import com.example.openvideo.core.prefs.LoopMode
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SubtitleBgStyle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial

class PlayerSettingsDialog(
    context: Context,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val onScreenBrightnessChanged: (Int) -> Unit = {},
    private val onRequestPickSubtitle: () -> Unit = {}
) : Dialog(context) {

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
            val density = context.resources.displayMetrics.density
            val bounds = PlayerSettingsLayoutPolicy.panelBounds(width, height, density)
            setLayout(bounds.width, bounds.height)
            setGravity(PlayerSettingsLayoutPolicy.panelGravity(width, height))
            attributes = attributes.apply {
                x = PlayerSettingsLayoutPolicy.landscapeMarginPx(width, height, density)
            }
            setDimAmount(0.18f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setBackgroundBlurRadius(dp(18))
            }
            setBackgroundDrawableResource(android.R.color.transparent)
            decorView.setPadding(0, 0, 0, 0)
            decorView.elevation = dp(20).toFloat()
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
            minimumHeight = dp(74)
            isClickable = true
            isFocusable = true
            setPadding(1, 4, 1, 2)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(1), dp(2), dp(1), dp(2))
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
            SettingsPage.SHARE -> shareVideoTitle()
            else -> showDetailPage(item.page, context.getString(item.titleRes))
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
            SettingsPage.PLAYLIST -> buildPlaylistPage()
            SettingsPage.STREAM -> buildStreamPage()
            SettingsPage.INFO -> buildInfoPage()
            SettingsPage.CUT -> buildCutPage()
            SettingsPage.BOOKMARK -> buildBookmarkPage()
            SettingsPage.TUTORIAL -> buildTutorialPage()
            SettingsPage.MORE -> buildMorePage()
            SettingsPage.SHARE -> shareVideoTitle()
        }
        primaryPage.visibility = View.GONE
        detailPage.visibility = View.VISIBLE
    }

    private fun buildAudioPage() {
        addRadioRow(
            title = context.getString(R.string.player_sheet_audio_track_english),
            checked = !playerPrefs.audioMuted
        ) {
            playerPrefs.audioMuted = false
            playerManager.setMuted(false)
            rebuildCurrentDetail(SettingsPage.AUDIO, context.getString(R.string.player_sheet_audio_track))
        }
        addRadioRow(
            title = context.getString(R.string.player_sheet_disable),
            checked = playerPrefs.audioMuted
        ) {
            playerPrefs.audioMuted = true
            playerManager.setMuted(true)
            rebuildCurrentDetail(SettingsPage.AUDIO, context.getString(R.string.player_sheet_audio_track))
        }
        addCheckboxRow(
            title = context.getString(R.string.player_sheet_software_audio_decoder),
            checked = playerPrefs.softwareAudioDecoder
        ) { checked ->
            playerPrefs.softwareAudioDecoder = checked
            viewModel.setDecodeMode(if (checked) DecodeMode.SOFT else DecodeMode.HARD)
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
            value = playerPrefs.subtitleDelayMs,
            label = { "${it}ms" },
            commitOnStop = true
        ) { value ->
            playerPrefs.subtitleDelayMs = value
        }
        addSeekRow(
            title = context.getString(R.string.settings_subtitle_size),
            min = 12,
            maxValue = 36,
            value = playerPrefs.subtitleSize,
            label = { "${it}sp" },
            commitOnStop = true
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
            min = 0,
            maxValue = 100,
            value = playerPrefs.brightnessAdjustment,
            label = { if (it == 0) context.getString(R.string.settings_theme_system) else "$it%" }
        ) { value ->
            playerPrefs.brightnessAdjustment = value
            onScreenBrightnessChanged(value)
        }
        addSeekRow(
            title = context.getString(R.string.player_sheet_contrast),
            min = -100,
            maxValue = 100,
            value = playerPrefs.contrastAdjustment,
            label = { "$it%" },
            commitOnStop = true
        ) { value ->
            playerPrefs.contrastAdjustment = value
            applyVideoAdjustmentsFromPrefs()
        }
        addSeekRow(
            title = context.getString(R.string.player_sheet_saturation),
            min = -100,
            maxValue = 100,
            value = playerPrefs.saturationAdjustment,
            label = { "$it%" },
            commitOnStop = true
        ) { value ->
            playerPrefs.saturationAdjustment = value
            applyVideoAdjustmentsFromPrefs()
        }
        addChoiceRow(
            title = context.getString(R.string.settings_rotation),
            value = "${playerPrefs.rotation}\u00B0",
            options = listOf("0\u00B0", "90\u00B0", "180\u00B0", "270\u00B0")
        ) { selected ->
            playerPrefs.rotation = selected.removeSuffix("\u00B0").toIntOrNull() ?: 0
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
            label = { "$it%" },
            commitOnStop = true
        ) { value ->
            playerPrefs.controlsOpacity = value
        }
    }

    private fun buildPlaylistPage() {
        addActionRow("Add current video") {
            viewModel.addCurrentVideoToDefaultPlaylist()
        }
        addChoiceRow(
            title = context.getString(R.string.settings_loop_mode),
            value = loopModeLabel(playerPrefs.loopMode),
            options = LoopMode.entries.map(::loopModeLabel)
        ) { selected ->
            val mode = LoopMode.entries.firstOrNull { loopModeLabel(it) == selected } ?: LoopMode.LIST
            playerPrefs.loopMode = mode
            viewModel.setRepeatMode(PlayerPlaybackSettings.repeatModeFor(mode))
        }
        addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.settings_auto_play_next),
            checked = playerPrefs.autoPlayNext
        ) { checked ->
            playerPrefs.autoPlayNext = checked
        }
        addChoiceRow(
            title = context.getString(R.string.settings_playback_speed),
            value = "${playerPrefs.speed}x",
            options = playbackSpeedOptions
        ) { selected ->
            setPlaybackSpeed(selected)
        }
        addChoiceRow(
            title = context.getString(R.string.settings_seek_interval),
            value = "${playerPrefs.seekInterval}s",
            options = listOf("5s", "10s", "15s", "30s")
        ) { selected ->
            playerPrefs.seekInterval = selected.removeSuffix("s").toIntOrNull() ?: 10
        }
        addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.settings_remember_progress),
            checked = playerPrefs.rememberProgress
        ) { checked ->
            playerPrefs.rememberProgress = checked
        }
    }

    private fun buildStreamPage() {
        addActionRow("Open network stream") {
            showStreamInput()
        }
        if (playerPrefs.lastStreamUrl.isNotBlank()) {
            addActionRow("Play last stream") {
                playStreamUrl(playerPrefs.lastStreamUrl)
            }
            addInfoRow("Last URL", playerPrefs.lastStreamUrl)
        } else {
            addInfoRow("Last URL", "None")
        }
    }

    private fun buildInfoPage() {
        val state = viewModel.uiState.value
        addInfoRow("Title", state.title.ifBlank { context.getString(R.string.app_name) })
        addInfoRow("Position", formatTime(playerManager.currentPosition))
        addInfoRow("Duration", formatTime(playerManager.duration))
        addInfoRow("Speed", "${state.speed}x")
        addInfoRow("Aspect", aspectLabel(playerPrefs.aspectRatio))
        addInfoRow("Source", viewModel.currentVideoSource().ifBlank { "None" })
    }

    private fun buildCutPage() {
        addInfoRow("Start", formatSavedTime(playerPrefs.clipStartMs))
        addInfoRow("End", formatSavedTime(playerPrefs.clipEndMs))
        addActionRow("Set clip start") {
            playerPrefs.clipStartMs = playerManager.currentPosition
            rebuildCurrentDetail(SettingsPage.CUT, context.getString(R.string.player_sheet_cut))
        }
        addActionRow("Set clip end") {
            playerPrefs.clipEndMs = playerManager.currentPosition
            rebuildCurrentDetail(SettingsPage.CUT, context.getString(R.string.player_sheet_cut))
        }
        addSwitchRow(
            parent = detailContainer,
            title = "Loop clip preview",
            checked = playerPrefs.clipLoopPreview
        ) { checked ->
            playerPrefs.clipLoopPreview = checked
        }
        addActionRow("Export clip") {
            exportClip()
        }
        addActionRow("Clear clip points") {
            playerPrefs.clipStartMs = -1L
            playerPrefs.clipEndMs = -1L
            playerPrefs.clipLoopPreview = false
            rebuildCurrentDetail(SettingsPage.CUT, context.getString(R.string.player_sheet_cut))
        }
    }

    private fun buildBookmarkPage() {
        val bookmark = playerPrefs.bookmarkPositionMs
        addInfoRow("Bookmark", formatSavedTime(bookmark))
        addActionRow("Save current position") {
            playerPrefs.bookmarkPositionMs = playerManager.currentPosition
            rebuildCurrentDetail(SettingsPage.BOOKMARK, context.getString(R.string.player_sheet_bookmark))
        }
        if (bookmark >= 0L) {
            addActionRow("Jump to bookmark") {
                viewModel.seekTo(bookmark)
                dismiss()
            }
            addActionRow("Clear bookmark") {
                playerPrefs.bookmarkPositionMs = -1L
                rebuildCurrentDetail(SettingsPage.BOOKMARK, context.getString(R.string.player_sheet_bookmark))
            }
        } else {
            addDisabledRow("Jump to bookmark")
            addDisabledRow("Clear bookmark")
        }
    }

    private fun buildTutorialPage() {
        addActionRow("Apply MX Player gestures") {
            playerPrefs.leftVerticalGesture = GestureAction.BRIGHTNESS
            playerPrefs.rightVerticalGesture = GestureAction.VOLUME
            playerPrefs.horizontalSwipeAction = GestureAction.SEEK
            playerPrefs.doubleTapAction = DoubleTapAction.FORWARD
            playerPrefs.longPressAction = LongPressAction.SPEED
            rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
        }
        addActionRow("Apply play/pause gestures") {
            playerPrefs.doubleTapAction = DoubleTapAction.PLAY_PAUSE
            playerPrefs.longPressAction = LongPressAction.SPEED
            rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
        }
        addChoiceRow(
            title = context.getString(R.string.settings_double_tap_action),
            value = doubleTapLabel(playerPrefs.doubleTapAction),
            options = DoubleTapAction.entries.map(::doubleTapLabel)
        ) { selected ->
            DoubleTapAction.entries.firstOrNull { doubleTapLabel(it) == selected }?.let {
                playerPrefs.doubleTapAction = it
            }
        }
        addChoiceRow(
            title = "Long press action",
            value = longPressLabel(playerPrefs.longPressAction),
            options = LongPressAction.entries.map(::longPressLabel)
        ) { selected ->
            LongPressAction.entries.firstOrNull { longPressLabel(it) == selected }?.let {
                playerPrefs.longPressAction = it
            }
        }
        addSwitchRow(
            parent = detailContainer,
            title = "Edge swipe back",
            checked = playerPrefs.edgeSwipeBack
        ) { checked ->
            playerPrefs.edgeSwipeBack = checked
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
            options = playbackSpeedOptions
        ) { selected ->
            setPlaybackSpeed(selected)
        }
        addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.settings_keep_screen_on),
            checked = playerPrefs.keepScreenOn
        ) { checked ->
            playerPrefs.keepScreenOn = checked
        }
        addChoiceRow(
            title = context.getString(R.string.settings_controls_auto_hide),
            value = if (playerPrefs.controlsAutoHide == 0) "Off" else "${playerPrefs.controlsAutoHide}s",
            options = listOf("Off", "3s", "5s", "8s")
        ) { selected ->
            playerPrefs.controlsAutoHide = selected.removeSuffix("s").toIntOrNull() ?: 0
        }
        addActionRow(context.getString(R.string.settings_reset_defaults)) {
            playerPrefs.resetToDefaults()
            showPrimaryPage()
            setupPrimarySwitches()
        }
    }

    private fun addInfoRow(title: String, value: String) {
        detailContainer.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(52)
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = value
                setTextColor(Color.rgb(176, 176, 176))
                textSize = 14f
                gravity = Gravity.END
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        })
        addDivider(detailContainer)
    }

    private fun exportClip() {
        val startMs = playerPrefs.clipStartMs
        val endMs = playerPrefs.clipEndMs
        if (startMs < 0L || endMs <= startMs) {
            Toast.makeText(context, "Set valid clip points first", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.exportClip(startMs, endMs) { success, path ->
            Toast.makeText(
                context,
                if (success) "Clip exported: $path" else "Clip export failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addDisabledRow(title: String) {
        detailContainer.addView(TextView(context).apply {
            text = title
            setTextColor(Color.rgb(85, 85, 85))
            textSize = 15f
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(52)
        })
        addDivider(detailContainer)
    }

    private fun showStreamInput() {
        val popup = BottomSheetDialog(context)
        val input = EditText(context).apply {
            setText(playerPrefs.lastStreamUrl)
            hint = "https://example.com/video.mp4"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(176, 176, 176))
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = context.getDrawable(R.drawable.bg_player_settings_sheet)
            setPadding(dp(20), dp(18), dp(20), dp(20))
            addView(TextView(context).apply {
                text = context.getString(R.string.player_sheet_stream)
                setTextColor(Color.WHITE)
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(input, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            })
            addView(TextView(context).apply {
            text = "Play"
                setTextColor(context.getColor(R.color.ov_accent_blue))
                textSize = 16f
                gravity = Gravity.CENTER
                minHeight = dp(48)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val url = input.text?.toString().orEmpty().trim()
                    if (url.isNotBlank()) {
                        playStreamUrl(url)
                        popup.dismiss()
                    }
                }
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            })
        }
        popup.setContentView(container)
        popup.show()
    }

    private fun playStreamUrl(url: String) {
        playerPrefs.lastStreamUrl = url
        viewModel.playStream(url)
        dismiss()
    }

    private fun setPlaybackSpeed(selected: String) {
        val speed = selected.removeSuffix("x").toFloatOrNull() ?: 1f
        playerPrefs.speed = speed
        viewModel.setSpeed(speed, PlayerPlaybackSettings.pitchFor(speed, playerPrefs.speedPreservePitch))
    }

    private fun applyVideoAdjustmentsFromPrefs() {
        playerManager.applyVideoAdjustments(
            0f,
            playerPrefs.contrastAdjustment / 100f,
            playerPrefs.saturationAdjustment / 100f
        )
    }

    private val playbackSpeedOptions = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")

    private fun loopModeLabel(value: LoopMode): String = when (value) {
        LoopMode.OFF -> context.getString(R.string.settings_loop_off)
        LoopMode.SINGLE -> context.getString(R.string.settings_loop_single)
        LoopMode.LIST -> context.getString(R.string.settings_loop_list)
    }

    private fun aspectLabel(value: AspectRatio): String = when (value) {
        AspectRatio.FIT -> context.getString(R.string.player_sheet_fit_screen)
        AspectRatio.FILL -> context.getString(R.string.player_sheet_fill_screen)
        AspectRatio.CROP -> context.getString(R.string.settings_ratio_crop)
        AspectRatio.STRETCH -> context.getString(R.string.settings_ratio_stretch)
        AspectRatio.RATIO_4_3 -> context.getString(R.string.settings_ratio_4_3)
        AspectRatio.RATIO_16_9 -> context.getString(R.string.settings_ratio_16_9)
    }

    private fun doubleTapLabel(value: DoubleTapAction): String = when (value) {
        DoubleTapAction.PLAY_PAUSE -> context.getString(R.string.settings_double_tap_pause)
        DoubleTapAction.FORWARD -> context.getString(R.string.settings_double_tap_forward)
        DoubleTapAction.BACKWARD -> context.getString(R.string.settings_double_tap_backward)
        DoubleTapAction.NONE -> context.getString(R.string.settings_double_tap_none)
    }

    private fun longPressLabel(value: LongPressAction): String = when (value) {
        LongPressAction.SPEED -> context.getString(R.string.settings_double_tap_playback)
        LongPressAction.NONE -> context.getString(R.string.settings_double_tap_none)
    }

    private fun formatSavedTime(ms: Long): String =
        if (ms >= 0L) formatTime(ms) else "None"

    private fun formatTime(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSec = safeMs / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
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
        commitOnStop: Boolean = false,
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
            var pendingValue = value.coerceIn(min, maxValue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val next = min + progress
                    pendingValue = next
                    valueView.text = label(next)
                    if (!commitOnStop) onChanged(next)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (commitOnStop) onChanged(pendingValue)
                }
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
        val title = viewModel.currentVideoShareText().ifBlank { context.getString(R.string.app_name) }
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
            SubtitleColorOption("White", 0xFFFFFFFF.toInt()),
            SubtitleColorOption("Yellow", 0xFFFFEB3B.toInt()),
            SubtitleColorOption("Green", 0xFF8BC34A.toInt()),
            SubtitleColorOption("Blue", 0xFF64B5F6.toInt())
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
