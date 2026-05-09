package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
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

    private data class SpeedChoice(val speed: Float, val labelRes: Int)

    private fun speedChoices(): List<SpeedChoice> = listOf(
        SpeedChoice(0.5f, R.string.settings_speed_0_5),
        SpeedChoice(0.75f, R.string.settings_speed_0_75),
        SpeedChoice(1f, R.string.settings_speed_1_0),
        SpeedChoice(1.25f, R.string.settings_speed_1_25),
        SpeedChoice(1.5f, R.string.settings_speed_1_5),
        SpeedChoice(2f, R.string.settings_speed_2_0)
    )

    private fun playbackSpeedLabelFor(speed: Float): String =
        speedChoices().firstOrNull { it.speed == speed }?.let { context.getString(it.labelRes) }
            ?: context.getString(R.string.player_settings_speed_fallback, speed)

    private data class SeekIntervalChoice(val seconds: Int, val labelRes: Int)

    private fun seekIntervalChoices(): List<SeekIntervalChoice> = listOf(
        SeekIntervalChoice(5, R.string.settings_seek_5s),
        SeekIntervalChoice(10, R.string.settings_seek_10s),
        SeekIntervalChoice(15, R.string.settings_seek_15s),
        SeekIntervalChoice(30, R.string.settings_seek_30s)
    )

    private fun seekIntervalLabelFor(seconds: Int): String =
        seekIntervalChoices().firstOrNull { it.seconds == seconds }?.let { context.getString(it.labelRes) }
            ?: context.getString(R.string.player_settings_seek_interval_seconds, seconds)

    private val rotationDegrees = listOf(0, 90, 180, 270)

    private fun rotationLabel(deg: Int): String = when (deg) {
        0 -> context.getString(R.string.settings_rotation_0)
        90 -> context.getString(R.string.settings_rotation_90)
        180 -> context.getString(R.string.settings_rotation_180)
        270 -> context.getString(R.string.settings_rotation_270)
        else -> context.getString(R.string.settings_rotation_0)
    }

    private fun controlsAutoHideChoiceList(): List<Pair<Int, Int>> = listOf(
        0 to R.string.settings_hide_never,
        3 to R.string.settings_hide_3s,
        5 to R.string.settings_hide_5s,
        8 to R.string.settings_hide_8s
    )

    private fun controlsAutoHideLabel(seconds: Int): String =
        controlsAutoHideChoiceList().firstOrNull { it.first == seconds }?.let { (_, res) ->
            context.getString(res)
        } ?: context.getString(R.string.player_settings_seek_interval_seconds, seconds)

    private lateinit var primaryPage: View
    private lateinit var detailPage: View
    private lateinit var grid: GridLayout
    private lateinit var primarySwitches: LinearLayout
    private lateinit var detailTitle: TextView
    private lateinit var detailContainer: LinearLayout
    private lateinit var subdetailPage: View
    private lateinit var subdetailTitle: TextView
    private lateinit var subdetailContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_player_settings)
        setCanceledOnTouchOutside(true)
        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP &&
                ::subdetailPage.isInitialized && subdetailPage.visibility == View.VISIBLE
            ) {
                hideSubDetailPage()
                true
            } else {
                false
            }
        }

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
            subdetailPage = requireView(R.id.settings_subdetail_page)
            subdetailTitle = requireView(R.id.settings_subdetail_title)
            subdetailContainer = requireView(R.id.settings_subdetail_container)

            requireView<ImageButton>(R.id.settings_detail_back).setOnClickListener {
                showPrimaryPage()
            }
            requireView<ImageButton>(R.id.settings_subdetail_back).setOnClickListener {
                hideSubDetailPage()
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
        subdetailPage.visibility = View.GONE
    }

    private fun hideSubDetailPage() {
        subdetailPage.visibility = View.GONE
    }

    private fun showSubDetailPage(title: String, build: (LinearLayout) -> Unit) {
        subdetailTitle.text = title
        subdetailContainer.removeAllViews()
        build(subdetailContainer)
        subdetailPage.visibility = View.VISIBLE
    }

    private fun showDetailPage(page: SettingsPage, title: String) {
        hideSubDetailPage()
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
            label = { ms -> context.getString(R.string.player_settings_unit_ms, ms) },
            commitOnStop = true
        ) { value ->
            playerPrefs.subtitleDelayMs = value
        }
        addSeekRow(
            title = context.getString(R.string.settings_subtitle_size),
            min = 12,
            maxValue = 36,
            value = playerPrefs.subtitleSize,
            label = { sp -> context.getString(R.string.player_settings_unit_sp, sp) },
            commitOnStop = true
        ) { value ->
            playerPrefs.subtitleSize = value
        }
        addChoiceRow(
            title = context.getString(R.string.settings_subtitle_color),
            value = context.getString(subtitleColorOptionFor(playerPrefs.subtitleColor).labelRes),
            options = subtitleColorOptions.map { context.getString(it.labelRes) }
        ) { selected ->
            subtitleColorOptions.firstOrNull { context.getString(it.labelRes) == selected }?.let {
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
            value = rotationLabel(playerPrefs.rotation),
            options = rotationDegrees.map(::rotationLabel)
        ) { selected ->
            playerPrefs.rotation = rotationDegrees.firstOrNull { rotationLabel(it) == selected }
                ?: playerPrefs.rotation
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
        addActionRow(context.getString(R.string.player_settings_add_current_to_playlist)) {
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
            value = playbackSpeedLabelFor(playerPrefs.speed),
            options = speedChoices().map { context.getString(it.labelRes) }
        ) { selected ->
            setPlaybackSpeedFromChoiceLabel(selected)
        }
        addChoiceRow(
            title = context.getString(R.string.settings_seek_interval),
            value = seekIntervalLabelFor(playerPrefs.seekInterval),
            options = seekIntervalChoices().map { context.getString(it.labelRes) }
        ) { selected ->
            setSeekIntervalFromChoiceLabel(selected)
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
        addActionRow(context.getString(R.string.player_settings_open_network_stream)) {
            showStreamInput()
        }
        if (playerPrefs.lastStreamUrl.isNotBlank()) {
            addActionRow(context.getString(R.string.player_settings_play_last_stream)) {
                playStreamUrl(playerPrefs.lastStreamUrl)
            }
            addInfoRow(context.getString(R.string.player_settings_last_stream_url), playerPrefs.lastStreamUrl)
        } else {
            addInfoRow(
                context.getString(R.string.player_settings_last_stream_url),
                context.getString(R.string.player_settings_value_none)
            )
        }
    }

    private fun buildInfoPage() {
        videoInfoRows().forEach { (label, value) -> addInfoRow(label, value) }
        addActionRow(context.getString(R.string.player_settings_info_copy)) {
            copyVideoInfoToClipboard()
        }
    }

    private fun videoInfoRows(): List<Pair<String, String>> {
        val state = viewModel.uiState.value
        return listOf(
            context.getString(R.string.player_settings_info_title) to
                state.title.ifBlank { context.getString(R.string.app_name) },
            context.getString(R.string.player_settings_info_position) to formatTime(playerManager.currentPosition),
            context.getString(R.string.player_settings_info_duration) to formatTime(playerManager.duration),
            context.getString(R.string.player_settings_info_speed) to playbackSpeedLabelFor(state.speed),
            context.getString(R.string.player_settings_info_aspect) to aspectLabel(playerPrefs.aspectRatio),
            context.getString(R.string.player_settings_info_source) to
                viewModel.currentVideoSource().ifBlank { context.getString(R.string.player_settings_value_none) }
        )
    }

    private fun copyVideoInfoToClipboard() {
        val text = videoInfoRows().joinToString(separator = "\n") { "${it.first}: ${it.second}" }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("openvideo_video_info", text))
        Toast.makeText(context, context.getString(R.string.player_settings_info_copied), Toast.LENGTH_SHORT).show()
    }

    private fun buildCutPage() {
        addInfoRow(context.getString(R.string.player_settings_clip_start), formatSavedTime(playerPrefs.clipStartMs))
        addInfoRow(context.getString(R.string.player_settings_clip_end), formatSavedTime(playerPrefs.clipEndMs))
        addActionRow(context.getString(R.string.player_settings_clip_set_start)) {
            playerPrefs.clipStartMs = playerManager.currentPosition
            rebuildCurrentDetail(SettingsPage.CUT, context.getString(R.string.player_sheet_cut))
        }
        addActionRow(context.getString(R.string.player_settings_clip_set_end)) {
            playerPrefs.clipEndMs = playerManager.currentPosition
            rebuildCurrentDetail(SettingsPage.CUT, context.getString(R.string.player_sheet_cut))
        }
        addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.player_settings_clip_loop_preview),
            checked = playerPrefs.clipLoopPreview
        ) { checked ->
            playerPrefs.clipLoopPreview = checked
        }
        addActionRow(context.getString(R.string.player_settings_clip_export)) {
            exportClip()
        }
        addActionRow(context.getString(R.string.player_settings_clip_clear)) {
            playerPrefs.clipStartMs = -1L
            playerPrefs.clipEndMs = -1L
            playerPrefs.clipLoopPreview = false
            rebuildCurrentDetail(SettingsPage.CUT, context.getString(R.string.player_sheet_cut))
        }
    }

    private fun buildBookmarkPage() {
        val bookmark = playerPrefs.bookmarkPositionMs
        addInfoRow(context.getString(R.string.player_settings_bookmark_label), formatSavedTime(bookmark))
        addActionRow(context.getString(R.string.player_settings_bookmark_save)) {
            playerPrefs.bookmarkPositionMs = playerManager.currentPosition
            rebuildCurrentDetail(SettingsPage.BOOKMARK, context.getString(R.string.player_sheet_bookmark))
        }
        if (bookmark >= 0L) {
            addActionRow(context.getString(R.string.player_settings_bookmark_jump)) {
                viewModel.seekTo(bookmark)
                dismiss()
            }
            addActionRow(context.getString(R.string.player_settings_bookmark_clear)) {
                playerPrefs.bookmarkPositionMs = -1L
                rebuildCurrentDetail(SettingsPage.BOOKMARK, context.getString(R.string.player_sheet_bookmark))
            }
        } else {
            addDisabledRow(context.getString(R.string.player_settings_bookmark_jump))
            addDisabledRow(context.getString(R.string.player_settings_bookmark_clear))
        }
    }

    private fun buildTutorialPage() {
        addActionRow(context.getString(R.string.player_sheet_tutorial_apply_mx)) {
            playerPrefs.leftVerticalGesture = GestureAction.BRIGHTNESS
            playerPrefs.rightVerticalGesture = GestureAction.VOLUME
            playerPrefs.horizontalSwipeAction = GestureAction.SEEK
            playerPrefs.doubleTapAction = DoubleTapAction.FORWARD
            playerPrefs.longPressAction = LongPressAction.SPEED
            rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
        }
        addActionRow(context.getString(R.string.player_sheet_tutorial_apply_play_pause)) {
            playerPrefs.doubleTapAction = DoubleTapAction.PLAY_PAUSE
            playerPrefs.longPressAction = LongPressAction.SPEED
            rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
        }
        addActionRow(
            title = context.getString(R.string.settings_double_tap_action),
            value = doubleTapLabel(playerPrefs.doubleTapAction)
        ) {
            showDoubleTapActionPage()
        }
        addActionRow(
            title = context.getString(R.string.settings_long_press_action),
            value = longPressLabel(playerPrefs.longPressAction)
        ) {
            showLongPressActionPage()
        }
        addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.settings_edge_swipe_back),
            checked = playerPrefs.edgeSwipeBack
        ) { checked ->
            playerPrefs.edgeSwipeBack = checked
        }
    }

    private fun showDoubleTapActionPage() {
        detailTitle.text = context.getString(R.string.settings_double_tap_action)
        detailContainer.removeAllViews()
        DoubleTapAction.entries.forEach { action ->
            addRadioRow(
                title = doubleTapLabel(action),
                checked = playerPrefs.doubleTapAction == action
            ) {
                playerPrefs.doubleTapAction = action
                rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
            }
        }
    }

    private fun showLongPressActionPage() {
        detailTitle.text = context.getString(R.string.settings_long_press_action)
        detailContainer.removeAllViews()
        LongPressAction.entries.forEach { action ->
            addRadioRow(
                title = longPressLabel(action),
                checked = playerPrefs.longPressAction == action
            ) {
                playerPrefs.longPressAction = action
                rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
            }
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
            value = playbackSpeedLabelFor(playerPrefs.speed),
            options = speedChoices().map { context.getString(it.labelRes) }
        ) { selected ->
            setPlaybackSpeedFromChoiceLabel(selected)
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
            value = controlsAutoHideLabel(playerPrefs.controlsAutoHide),
            options = controlsAutoHideChoiceList().map { (_, res) -> context.getString(res) }
        ) { selected ->
            playerPrefs.controlsAutoHide = controlsAutoHideChoiceList()
                .firstOrNull { context.getString(it.second) == selected }?.first ?: 0
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
            Toast.makeText(context, context.getString(R.string.player_settings_toast_clip_need_points), Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.exportClip(startMs, endMs) { success, path ->
            Toast.makeText(
                context,
                if (success) {
                    context.getString(R.string.player_settings_toast_clip_exported, path)
                } else {
                    context.getString(R.string.player_settings_toast_clip_export_failed)
                },
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
        val input = EditText(context).apply {
            setText(playerPrefs.lastStreamUrl)
            hint = context.getString(R.string.player_settings_stream_url_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(176, 176, 176))
        }
        showSubDetailPage(context.getString(R.string.player_sheet_stream)) { container ->
            container.addView(
                input,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(4)
                }
            )
            addDivider(container)
            addAccentActionRow(container, context.getString(R.string.player_action_play)) {
                val url = input.text?.toString().orEmpty().trim()
                if (url.isNotBlank()) playStreamUrl(url)
            }
        }
    }

    private fun playStreamUrl(url: String) {
        playerPrefs.lastStreamUrl = url
        viewModel.playStream(url)
        dismiss()
    }

    private fun setPlaybackSpeedFromChoiceLabel(selected: String) {
        val choice = speedChoices().firstOrNull { context.getString(it.labelRes) == selected } ?: return
        playerPrefs.speed = choice.speed
        viewModel.setSpeed(
            choice.speed,
            PlayerPlaybackSettings.pitchFor(choice.speed, playerPrefs.speedPreservePitch)
        )
    }

    private fun setSeekIntervalFromChoiceLabel(selected: String) {
        val choice = seekIntervalChoices().firstOrNull { context.getString(it.labelRes) == selected } ?: return
        playerPrefs.seekInterval = choice.seconds
    }

    private fun applyVideoAdjustmentsFromPrefs() {
        playerManager.applyVideoAdjustments(
            0f,
            playerPrefs.contrastAdjustment / 100f,
            playerPrefs.saturationAdjustment / 100f
        )
    }

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
        if (ms >= 0L) formatTime(ms) else context.getString(R.string.player_settings_value_none)

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
        detailContainer.addView(
            TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                minHeight = dp(52)
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        )
        addDivider(detailContainer)
    }

    private fun addAccentActionRow(parent: LinearLayout, title: String, onClick: () -> Unit) {
        parent.addView(
            TextView(context).apply {
                text = title
                setTextColor(context.getColor(R.color.ov_accent_blue))
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                minHeight = dp(52)
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        )
        addDivider(parent)
    }

    private fun addActionRow(title: String, value: String, onClick: () -> Unit) {
        detailContainer.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(54)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
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
        showSubDetailPage(title) { container ->
            options.forEach { option ->
                container.addView(
                    RadioButton(context).apply {
                        text = option
                        isChecked = option == selected
                        buttonTintList = context.getColorStateList(R.color.nav_item_tint)
                        setTextColor(
                            if (option == selected) context.getColor(R.color.ov_accent_blue) else Color.WHITE
                        )
                        textSize = 15f
                        minHeight = dp(52)
                        setOnClickListener {
                            onSelected(option)
                            hideSubDetailPage()
                        }
                    }
                )
                addDivider(container)
            }
        }
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

    private data class SubtitleColorOption(val labelRes: Int, val color: Int)

    private val subtitleColorOptions: Array<SubtitleColorOption> by lazy {
        arrayOf(
            SubtitleColorOption(R.string.player_settings_subtitle_color_white, 0xFFFFFFFF.toInt()),
            SubtitleColorOption(R.string.player_settings_subtitle_color_yellow, 0xFFFFEB3B.toInt()),
            SubtitleColorOption(R.string.player_settings_subtitle_color_green, 0xFF8BC34A.toInt()),
            SubtitleColorOption(R.string.player_settings_subtitle_color_blue, 0xFF64B5F6.toInt())
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
