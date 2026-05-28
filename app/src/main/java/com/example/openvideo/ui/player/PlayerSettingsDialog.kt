package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.text.InputType
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
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
import com.example.openvideo.core.prefs.PlaybackEndBehavior
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SubtitleBgStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.ArrayDeque
class PlayerSettingsDialog(
    context: Context,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val onScreenBrightnessChanged: (Int) -> Unit = {},
    private val onRequestPickSubtitle: () -> Unit = {},
    private val onAspectRatioChanged: () -> Unit = {},
    /** Invoked after [PlayerPrefs.resetToDefaults] so playback/UI re-sync from cleared prefs. */
    private val onPlayerPrefsReset: () -> Unit = {}
) : Dialog(context) {

    private val primaryItems by lazy {
        listOf(
            PlayerSettingsPrimaryItem(SettingsPage.AUDIO, R.string.player_sheet_audio_track, R.drawable.ic_audio_track),
            PlayerSettingsPrimaryItem(SettingsPage.SUBTITLE, R.string.player_sheet_subtitles, R.drawable.ic_subtitles),
            PlayerSettingsPrimaryItem(SettingsPage.ASPECT, R.string.player_sheet_aspect_ratio, R.drawable.ic_aspect_ratio),
            PlayerSettingsPrimaryItem(SettingsPage.DISPLAY, R.string.player_sheet_display, R.drawable.ic_display_settings),
            PlayerSettingsPrimaryItem(SettingsPage.PLAYLIST, R.string.player_sheet_playlist, R.drawable.ic_playlist),
            PlayerSettingsPrimaryItem(SettingsPage.STREAM, R.string.player_sheet_stream, R.drawable.ic_stream),
            PlayerSettingsPrimaryItem(SettingsPage.INFO, R.string.player_sheet_info, R.drawable.ic_info),
            PlayerSettingsPrimaryItem(SettingsPage.SHARE, R.string.player_sheet_share, R.drawable.ic_share),
            PlayerSettingsPrimaryItem(SettingsPage.CUT, R.string.player_sheet_cut, R.drawable.ic_cut),
            PlayerSettingsPrimaryItem(SettingsPage.BOOKMARK, R.string.player_sheet_bookmark, R.drawable.ic_bookmark),
            PlayerSettingsPrimaryItem(SettingsPage.TUTORIAL, R.string.player_sheet_tutorial, R.drawable.ic_tutorial),
            PlayerSettingsPrimaryItem(SettingsPage.MORE, R.string.player_sheet_more, R.drawable.ic_more_vert)
        )
    }

    private val subtitleBgOptions = SubtitleBgStyle.entries.toTypedArray()
    private val subtitleEncodingOptions = arrayOf("auto", "UTF-8", "GBK", "GB2312", "Big5", "Shift_JIS", "EUC-KR")
    private val formatter by lazy { PlayerSettingsFormatter(context) }

    private lateinit var primaryPage: View
    private lateinit var detailPage: View
    private lateinit var grid: GridLayout
    private lateinit var primarySwitches: LinearLayout
    private lateinit var detailTitle: TextView
    private lateinit var detailContainer: LinearLayout
    private lateinit var subdetailPage: View
    private lateinit var subdetailTitle: TextView
    private lateinit var subdetailContainer: LinearLayout
    private lateinit var settingsPanelRoot: View
    private val dialogScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private data class DetailBackState(val page: SettingsPage, val title: String)

    /** Nested screens inside [settings_detail_page] (same pattern as tutorial double-tap). */
    private val detailBackStack = ArrayDeque<DetailBackState>()
    private var currentRootDetailPage: SettingsPage? = null
    private val rows by lazy {
        PlayerSettingsRowBuilder(
            context = context,
            playerPrefs = playerPrefs,
            detailContainerProvider = { detailContainer },
            openNestedDetailScreen = ::openNestedDetailScreen,
            onNestedChoiceSelected = {
                detailBackStack.lastOrNull()?.let { prev ->
                    rebuildCurrentDetail(prev.page, prev.title)
                }
            }
        )
    }
    private val infoController by lazy {
        PlayerSettingsInfoController(
            context = context,
            playerManager = playerManager,
            viewModel = viewModel,
            playerPrefs = playerPrefs,
            scope = dialogScope,
            isInfoPageVisible = {
                currentRootDetailPage == SettingsPage.INFO &&
                    ::detailPage.isInitialized &&
                    detailPage.visibility == View.VISIBLE
            },
            onInfoChanged = {
                rebuildCurrentDetail(SettingsPage.INFO, context.getString(R.string.player_sheet_info))
            },
            formatTime = formatter::formatTime,
            playbackSpeedLabelFor = formatter::playbackSpeedLabelFor,
            aspectLabel = formatter::aspectLabel
        )
    }
    private val primaryGridBuilder by lazy {
        PlayerSettingsPrimaryGridBuilder(context, ::handlePrimaryClick)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_player_settings)
        setCanceledOnTouchOutside(true)
        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (::subdetailPage.isInitialized && subdetailPage.visibility == View.VISIBLE) {
                    hideSubDetailPage()
                    return@setOnKeyListener true
                }
                if (::detailPage.isInitialized && detailPage.visibility == View.VISIBLE && detailBackStack.isNotEmpty()) {
                    popDetailNested()
                    return@setOnKeyListener true
                }
            }
            false
        }

        window?.let {
            PlayerSettingsSheetChrome.applyWindowLayout(
                it,
                context.resources.displayMetrics.widthPixels,
                context.resources.displayMetrics.heightPixels,
                context.resources.displayMetrics.density
            )
            PlayerSettingsSheetChrome.applyBackdrop(it, playerPrefs, context.resources.displayMetrics.density)
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
            settingsPanelRoot = requireView(R.id.settings_panel_root)
            applySettingsSheetOpacity()

            requireView<ImageButton>(R.id.settings_detail_back).setOnClickListener {
                if (detailBackStack.isNotEmpty()) popDetailNested() else showPrimaryPage()
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

    override fun onStart() {
        super.onStart()
        applySettingsSheetOpacity()
        window?.let { PlayerSettingsSheetChrome.applyBackdrop(it, playerPrefs, context.resources.displayMetrics.density) }
    }

    override fun dismiss() {
        dialogScope.cancel()
        super.dismiss()
    }

    private fun setupPrimaryGrid() {
        grid.removeAllViews()
        primaryItems.forEach { item ->
            grid.addView(primaryGridBuilder.createPrimaryItemView(item))
        }
    }

    private fun setupPrimarySwitches() {
        primarySwitches.removeAllViews()
        rows.addSwitchRows(primarySwitchSpecs(), primarySwitches)
    }

    private fun primarySwitchSpecs(): List<PlayerSettingsSwitchSpec> = listOf(
        PlayerSettingsSwitchSpec(
            title = context.getString(R.string.player_sheet_video_display),
            checked = playerPrefs.videoDisplayEnabled
        ) { checked ->
            playerPrefs.videoDisplayEnabled = checked
        },
        PlayerSettingsSwitchSpec(
            title = context.getString(R.string.player_sheet_keyboard_shortcuts),
            checked = playerPrefs.keyboardShortcuts
        ) { checked ->
            playerPrefs.keyboardShortcuts = checked
        },
        PlayerSettingsSwitchSpec(
            title = context.getString(R.string.player_sheet_bg_playback),
            checked = playerPrefs.bgAudio
        ) { checked ->
            playerPrefs.bgAudio = checked
        },
        PlayerSettingsSwitchSpec(
            title = context.getString(R.string.player_sheet_remember_position),
            checked = playerPrefs.rememberProgress
        ) { checked ->
            playerPrefs.rememberProgress = checked
        }
    )

    /**
     * 婵犲﹥鍨靛锛勨偓娑欘焽濞堟垿寮伴妯峰亾鐏炶偐鐟濋梺顐㈢箲濡叉垶鎯旈敂琛″亾瀹ュ洦顏ラ柛鎺戞閻︻噣鏁?00%=閻庣懓鑻崣蹇旂▔瀹ュ鍋撹箛鏃€顫栭柨?%=闁哄牃鍋撻梺顐㈢箲濡叉垿濡?     * `alpha` 濞戞挸绨肩粭澶愭焻韫囨梹顫栭幖杈剧細缁旀挳鎳涙潏鍓х濞戞挸绉崇粭宀勫Υ瀹€鍕ㄥ亾韫囨梹顫栭幖杈捐礋閳ь剙绉村浠嬪触閹存瑢鍋?     */

    private fun applySettingsSheetOpacity() {
        if (!::settingsPanelRoot.isInitialized) return
        PlayerSettingsSheetChrome.applyPanelOpacity(settingsPanelRoot, playerPrefs)
    }

    private fun applySheetWindowBackdrop() {
        window?.let {
            PlayerSettingsSheetChrome.applyBackdrop(it, playerPrefs, context.resources.displayMetrics.density)
        }
    }

    private fun handlePrimaryClick(item: PlayerSettingsPrimaryItem) {
        when (item.page) {
            SettingsPage.SHARE -> shareVideoTitle()
            else -> showDetailPage(item.page, context.getString(item.titleRes))
        }
    }

    private fun showPrimaryPage() {
        primaryPage.visibility = View.VISIBLE
        detailPage.visibility = View.GONE
        subdetailPage.visibility = View.GONE
        detailBackStack.clear()
        currentRootDetailPage = null
    }

    private fun hideSubDetailPage() {
        subdetailPage.visibility = View.GONE
    }

    private fun populateDetailBody(page: SettingsPage) {
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
            SettingsPage.SHARE -> Unit
        }
    }

    /** Opens a second-level screen (toolbar stays; title + list update like tutorial double-tap). */
    private fun openNestedDetailScreen(subTitle: String, build: () -> Unit) {
        val page = currentRootDetailPage ?: return
        detailBackStack.addLast(DetailBackState(page, detailTitle.text.toString()))
        detailTitle.text = subTitle
        detailContainer.removeAllViews()
        build()
    }

    private fun popDetailNested() {
        hideSubDetailPage()
        if (detailBackStack.isEmpty()) return
        val prev = detailBackStack.removeLast()
        detailTitle.text = prev.title
        detailContainer.removeAllViews()
        populateDetailBody(prev.page)
    }

    private fun showDetailPage(page: SettingsPage, title: String) {
        hideSubDetailPage()
        detailBackStack.clear()
        currentRootDetailPage = page
        detailTitle.text = title
        detailContainer.removeAllViews()
        populateDetailBody(page)
        primaryPage.visibility = View.GONE
        detailPage.visibility = View.VISIBLE
    }

    private fun buildAudioPage() {
        val audioTitle = context.getString(R.string.player_sheet_audio_track)
        val audioTracks = viewModel.audioTracks()
        val selectedAudioTrack = viewModel.selectedAudioTrack()
        rows.addActionRow(
            title = audioTitle,
            value = if (playerPrefs.audioMuted) {
                context.getString(R.string.player_sheet_disable)
            } else {
                selectedAudioTrack?.let(infoController::audioTrackLabel)
                    ?: context.getString(R.string.settings_audio_track_auto)
            }
        ) {
            openNestedDetailScreen(audioTitle) {
                if (audioTracks.isEmpty()) {
                    rows.addDisabledRow(context.getString(R.string.player_settings_audio_track_none))
                }
                audioTracks.forEach { track ->
                    rows.addRadioRow(
                        title = infoController.audioTrackLabel(track),
                        checked = !playerPrefs.audioMuted && track.selected,
                        enabled = track.supported
                    ) {
                        viewModel.selectAudioTrack(track)
                        detailBackStack.lastOrNull()?.let { rebuildCurrentDetail(it.page, it.title) }
                    }
                }
                rows.addRadioRow(
                    title = context.getString(R.string.player_sheet_disable),
                    checked = playerPrefs.audioMuted
                ) {
                    viewModel.disableAudioTrack()
                    detailBackStack.lastOrNull()?.let { rebuildCurrentDetail(it.page, it.title) }
                }
            }
        }
        rows.addCheckboxRow(
            title = context.getString(R.string.player_sheet_software_audio_decoder),
            checked = playerPrefs.softwareAudioDecoder
        ) { checked ->
            playerPrefs.softwareAudioDecoder = checked
            viewModel.setDecodeMode(if (checked) DecodeMode.SOFT else DecodeMode.HARD)
        }
        rows.addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.player_sheet_enable),
            checked = !playerPrefs.pauseOnExit
        ) { checked ->
            playerPrefs.pauseOnExit = !checked
        }
        val stereoTitle = context.getString(R.string.player_sheet_stereo_mode)
        val syncTitle = context.getString(R.string.player_sheet_sync)
        rows.addActionRow(
            title = stereoTitle,
            value = if (playerPrefs.audioChannel == AudioChannel.STEREO) stereoTitle else syncTitle
        ) {
            openNestedDetailScreen(stereoTitle) {
                rows.addRadioRow(
                    title = stereoTitle,
                    checked = playerPrefs.audioChannel == AudioChannel.STEREO
                ) {
                    playerPrefs.audioChannel = AudioChannel.STEREO
                    detailBackStack.lastOrNull()?.let { rebuildCurrentDetail(it.page, it.title) }
                }
                rows.addRadioRow(
                    title = syncTitle,
                    checked = playerPrefs.audioDelay == 0
                ) {
                    playerPrefs.audioDelay = 0
                    detailBackStack.lastOrNull()?.let { rebuildCurrentDetail(it.page, it.title) }
                }
            }
        }
        rows.addCheckboxRow(
            title = context.getString(R.string.player_sheet_av_sync),
            checked = playerPrefs.audioSyncEnabled
        ) { checked ->
            playerPrefs.audioSyncEnabled = checked
        }
    }

    private fun buildSubtitlePage() {
        rows.addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.player_sheet_subtitle_switch),
            checked = playerPrefs.subtitlesEnabled
        ) { checked ->
            playerPrefs.subtitlesEnabled = checked
        }
        rows.addActionRow(context.getString(R.string.player_sheet_select_subtitle_file)) {
            dismiss()
            onRequestPickSubtitle()
        }
        rows.addSeekRow(
            title = context.getString(R.string.player_sheet_subtitle_delay),
            min = -5000,
            maxValue = 5000,
            value = playerPrefs.subtitleDelayMs,
            label = { ms -> context.getString(R.string.player_settings_unit_ms, ms) },
            commitOnStop = true
        ) { value ->
            playerPrefs.subtitleDelayMs = value
        }
        rows.addSeekRow(
            title = context.getString(R.string.settings_subtitle_size),
            min = 12,
            maxValue = 36,
            value = playerPrefs.subtitleSize,
            label = { sp -> context.getString(R.string.player_settings_unit_sp, sp) },
            commitOnStop = true
        ) { value ->
            playerPrefs.subtitleSize = value
        }
        rows.addSubtitleColorSwatchRow()
        rows.addChoiceRow(
            title = context.getString(R.string.settings_subtitle_bg),
            value = formatter.subtitleBgLabel(playerPrefs.subtitleBgStyle),
            options = subtitleBgOptions.map(formatter::subtitleBgLabel)
        ) { selected ->
            subtitleBgOptions.firstOrNull { formatter.subtitleBgLabel(it) == selected }?.let {
                playerPrefs.subtitleBgStyle = it
            }
        }
        rows.addChoiceRow(
            title = context.getString(R.string.settings_subtitle_encoding),
            value = formatter.subtitleEncodingLabel(playerPrefs.subtitleEncoding),
            options = subtitleEncodingOptions.map(formatter::subtitleEncodingLabel)
        ) { selected ->
            val index = subtitleEncodingOptions.map(formatter::subtitleEncodingLabel).indexOf(selected)
            if (index >= 0) playerPrefs.subtitleEncoding = subtitleEncodingOptions[index]
        }
    }

    private fun buildAspectPage() {
        PlayerAspectRatioOptions.entries.forEach { option ->
            addAspectRow(context.getString(option.labelRes), option.ratio)
        }
    }

    private fun buildDisplayPage() {
        rows.addSeekRow(
            title = context.getString(R.string.player_sheet_brightness),
            min = 0,
            maxValue = 100,
            value = playerPrefs.brightnessAdjustment,
            label = { if (it == 0) context.getString(R.string.settings_theme_system) else "$it%" }
        ) { value ->
            playerPrefs.brightnessAdjustment = value
            onScreenBrightnessChanged(value)
        }
        rows.addSeekRow(
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
        rows.addSeekRow(
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
        rows.addChoiceRow(
            title = context.getString(R.string.settings_rotation),
            value = formatter.rotationLabel(playerPrefs.rotation),
            options = formatter.rotationDegrees.map(formatter::rotationLabel)
        ) { selected ->
            playerPrefs.rotation = formatter.rotationDegrees.firstOrNull { formatter.rotationLabel(it) == selected }
                ?: playerPrefs.rotation
        }
        rows.addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.settings_mirror),
            checked = playerPrefs.mirror
        ) { checked ->
            playerPrefs.mirror = checked
        }
        rows.addChoiceRow(
            title = context.getString(R.string.player_sheet_progress_style),
            value = formatter.progressStyleLabel(playerPrefs.progressStyle),
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
        rows.addSeekRow(
            title = context.getString(R.string.player_sheet_controls_opacity),
            min = 30,
            maxValue = 100,
            value = playerPrefs.controlsOpacity,
            label = { "$it%" },
            commitOnStop = true
        ) { value ->
            playerPrefs.controlsOpacity = value
        }
        rows.addSeekRow(
            title = context.getString(R.string.player_sheet_settings_backdrop_dim),
            min = 0,
            maxValue = 100,
            value = playerPrefs.settingsSheetBackdropDimPercent,
            label = { "$it%" },
            commitOnStop = false
        ) { value ->
            playerPrefs.settingsSheetBackdropDimPercent = value
            applySheetWindowBackdrop()
        }
        rows.addSeekRow(
            title = context.getString(R.string.player_sheet_settings_backdrop_blur),
            min = 0,
            maxValue = 64,
            value = playerPrefs.settingsSheetBackdropBlurDp,
            label = { "${it}dp" },
            commitOnStop = false
        ) { value ->
            playerPrefs.settingsSheetBackdropBlurDp = value
            applySheetWindowBackdrop()
        }
    }

    private fun buildPlaylistPage() {
        rows.addActionRow(context.getString(R.string.player_settings_add_current_to_playlist)) {
            viewModel.addCurrentVideoToDefaultPlaylist()
        }
        rows.addChoiceRow(
            title = context.getString(R.string.settings_loop_mode),
            value = formatter.loopModeLabel(playerPrefs.loopMode),
            options = LoopMode.entries.map(formatter::loopModeLabel)
        ) { selected ->
            val mode = LoopMode.entries.firstOrNull { formatter.loopModeLabel(it) == selected } ?: LoopMode.LIST
            playerPrefs.loopMode = mode
            viewModel.setRepeatMode(PlayerPlaybackSettings.repeatModeFor(mode))
        }
        rows.addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.settings_auto_play_next),
            checked = playerPrefs.autoPlayNext
        ) { checked ->
            playerPrefs.autoPlayNext = checked
        }
        rows.addChoiceRow(
            title = context.getString(R.string.settings_playback_end_behavior),
            value = formatter.playbackEndBehaviorLabel(playerPrefs.playbackEndBehavior),
            options = PlayerPlaybackEndBehaviorUi.options().map(formatter::playbackEndBehaviorLabel)
        ) { selected ->
            val behavior = PlayerPlaybackEndBehaviorUi.options()
                .firstOrNull { formatter.playbackEndBehaviorLabel(it) == selected }
                ?: PlaybackEndBehavior.FOLLOW_SETTINGS
            playerPrefs.playbackEndBehavior = behavior
        }
        addPlaybackSpeedSeekRow()
        rows.addChoiceRow(
            title = context.getString(R.string.settings_seek_interval),
            value = formatter.seekIntervalLabelFor(playerPrefs.seekInterval),
            options = formatter.seekIntervalChoices().map { context.getString(it.labelRes) }
        ) { selected ->
            setSeekIntervalFromChoiceLabel(selected)
        }
        rows.addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.settings_remember_progress),
            checked = playerPrefs.rememberProgress
        ) { checked ->
            playerPrefs.rememberProgress = checked
        }
    }

    private fun buildStreamPage() {
        val streamInput = EditText(context).apply {
            setText(playerPrefs.lastStreamUrl)
            hint = context.getString(R.string.player_settings_stream_url_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(176, 176, 176))
        }
        detailContainer.addView(
            streamInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
        )
        rows.addDivider(detailContainer)
        rows.addAccentActionRow(detailContainer, context.getString(R.string.player_action_play)) {
            val url = streamInput.text?.toString().orEmpty().trim()
            if (url.isNotBlank()) playStreamUrl(url)
        }
        if (playerPrefs.lastStreamUrl.isNotBlank()) {
            rows.addActionRow(context.getString(R.string.player_settings_play_last_stream)) {
                playStreamUrl(playerPrefs.lastStreamUrl)
            }
            rows.addInfoRow(context.getString(R.string.player_settings_last_stream_url), playerPrefs.lastStreamUrl)
        } else {
            rows.addInfoRow(
                context.getString(R.string.player_settings_last_stream_url),
                context.getString(R.string.player_settings_value_none)
            )
        }
    }

    private fun buildInfoPage() {
        infoController.loadMediaInfoAsync()
        infoController.videoInfoRows().forEach { (label, value) -> rows.addInfoRow(label, value) }
        rows.addActionRow(context.getString(R.string.player_settings_info_copy)) {
            infoController.copyVideoInfoToClipboard()
        }
    }

    private fun buildCutPage() {
        rows.addInfoRow(context.getString(R.string.player_settings_clip_start), formatter.formatSavedTime(playerPrefs.clipStartMs))
        rows.addInfoRow(context.getString(R.string.player_settings_clip_end), formatter.formatSavedTime(playerPrefs.clipEndMs))
        rows.addActionRow(context.getString(R.string.player_settings_clip_set_start)) {
            playerPrefs.clipStartMs = playerManager.currentPosition
            rebuildCurrentDetail(SettingsPage.CUT, context.getString(R.string.player_sheet_cut))
        }
        rows.addActionRow(context.getString(R.string.player_settings_clip_set_end)) {
            playerPrefs.clipEndMs = playerManager.currentPosition
            rebuildCurrentDetail(SettingsPage.CUT, context.getString(R.string.player_sheet_cut))
        }
        rows.addSwitchRow(
            parent = detailContainer,
            title = context.getString(R.string.player_settings_clip_loop_preview),
            checked = playerPrefs.clipLoopPreview
        ) { checked ->
            playerPrefs.clipLoopPreview = checked
        }
        rows.addActionRow(context.getString(R.string.player_settings_clip_export)) {
            exportClip()
        }
        rows.addActionRow(context.getString(R.string.player_settings_clip_clear)) {
            playerPrefs.clipStartMs = -1L
            playerPrefs.clipEndMs = -1L
            playerPrefs.clipLoopPreview = false
            rebuildCurrentDetail(SettingsPage.CUT, context.getString(R.string.player_sheet_cut))
        }
    }

    private fun buildBookmarkPage() {
        val bookmark = playerPrefs.bookmarkPositionMs
        rows.addInfoRow(context.getString(R.string.player_settings_bookmark_label), formatter.formatSavedTime(bookmark))
        rows.addActionRow(context.getString(R.string.player_settings_bookmark_save)) {
            playerPrefs.bookmarkPositionMs = playerManager.currentPosition
            rebuildCurrentDetail(SettingsPage.BOOKMARK, context.getString(R.string.player_sheet_bookmark))
        }
        if (bookmark >= 0L) {
            rows.addActionRow(context.getString(R.string.player_settings_bookmark_jump)) {
                viewModel.seekTo(bookmark)
                dismiss()
            }
            rows.addActionRow(context.getString(R.string.player_settings_bookmark_clear)) {
                playerPrefs.bookmarkPositionMs = -1L
                rebuildCurrentDetail(SettingsPage.BOOKMARK, context.getString(R.string.player_sheet_bookmark))
            }
        } else {
            rows.addDisabledRow(context.getString(R.string.player_settings_bookmark_jump))
            rows.addDisabledRow(context.getString(R.string.player_settings_bookmark_clear))
        }
    }

    private fun buildTutorialPage() {
        /*
        rows.addActionRow(context.getString(R.string.player_sheet_tutorial_apply_mx)) {
            playerPrefs.leftVerticalGesture = GestureAction.BRIGHTNESS
            playerPrefs.rightVerticalGesture = GestureAction.VOLUME
            playerPrefs.horizontalSwipeAction = GestureAction.SEEK
            playerPrefs.doubleTapAction = DoubleTapAction.FORWARD
            playerPrefs.longPressAction = LongPressAction.SPEED
            rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
        }
        rows.addActionRow(context.getString(R.string.player_sheet_tutorial_apply_play_pause)) {
            playerPrefs.doubleTapAction = DoubleTapAction.PLAY_PAUSE
            playerPrefs.longPressAction = LongPressAction.SPEED
            rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
        }
        */
        rows.addActionRows(gesturePresetActionSpecs())
        rows.addActionRows(tutorialActionSpecs())
        rows.addSwitchRows(tutorialSwitchSpecs(), detailContainer)
    }

    private fun gesturePresetActionSpecs(): List<PlayerSettingsActionSpec> =
        PlayerGesturePreset.entries.map { preset ->
            PlayerSettingsActionSpec(
                title = formatter.gesturePresetLabel(preset),
                value = null
            ) {
                applyGesturePreset(preset)
                rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
            }
        }

    private fun applyGesturePreset(preset: PlayerGesturePreset) {
        val settings = PlayerGesturePresetPolicy.settingsFor(preset)
        playerPrefs.leftVerticalGesture = settings.leftVerticalGesture
        playerPrefs.rightVerticalGesture = settings.rightVerticalGesture
        playerPrefs.horizontalSwipeAction = settings.horizontalSwipeAction
        playerPrefs.doubleTapAction = settings.doubleTapAction
        playerPrefs.longPressAction = settings.longPressAction
        playerPrefs.gestureSensitivity = settings.gestureSensitivity
        playerPrefs.edgeSwipeBack = settings.edgeSwipeBack
    }

    private fun showDoubleTapActionPage() {
        openNestedDetailScreen(context.getString(R.string.settings_double_tap_action)) {
            DoubleTapAction.entries.forEach { action ->
                rows.addRadioRow(
                    title = formatter.doubleTapLabel(action),
                    checked = playerPrefs.doubleTapAction == action
                ) {
                    playerPrefs.doubleTapAction = action
                    rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
                }
            }
        }
    }

    private fun showLongPressActionPage() {
        openNestedDetailScreen(context.getString(R.string.settings_long_press_action)) {
            LongPressAction.entries.forEach { action ->
                rows.addRadioRow(
                    title = formatter.longPressLabel(action),
                    checked = playerPrefs.longPressAction == action
                ) {
                    playerPrefs.longPressAction = action
                    rebuildCurrentDetail(SettingsPage.TUTORIAL, context.getString(R.string.player_sheet_tutorial))
                }
            }
        }
    }

    private fun buildMorePage() {
        rows.addSwitchRows(moreIntroSwitchSpecs(), detailContainer)
        addPlaybackSpeedSeekRow()
        rows.addSwitchRows(moreScreenSwitchSpecs(), detailContainer)
        rows.addChoiceRow(
            title = context.getString(R.string.settings_controls_auto_hide),
            value = formatter.controlsAutoHideLabel(playerPrefs.controlsAutoHide),
            options = formatter.controlsAutoHideChoiceList().map { (_, res) -> context.getString(res) }
        ) { selected ->
            playerPrefs.controlsAutoHide = formatter.controlsAutoHideChoiceList()
                .firstOrNull { context.getString(it.second) == selected }?.first ?: 0
        }
        rows.addSeekRow(
            title = context.getString(R.string.player_sheet_settings_panel_opacity),
            min = 0,
            maxValue = 100,
            value = playerPrefs.settingsPanelOpacity,
            label = { "$it%" },
            commitOnStop = false
        ) { value ->
            playerPrefs.settingsPanelOpacity = value
            applySettingsSheetOpacity()
        }
        rows.addActionRows(moreActionSpecs())
    }

    private fun tutorialActionSpecs(): List<PlayerSettingsActionSpec> = listOf(
        PlayerSettingsActionSpec(
            title = context.getString(R.string.settings_double_tap_action),
            value = formatter.doubleTapLabel(playerPrefs.doubleTapAction),
            onClick = ::showDoubleTapActionPage
        ),
        PlayerSettingsActionSpec(
            title = context.getString(R.string.settings_long_press_action),
            value = formatter.longPressLabel(playerPrefs.longPressAction),
            onClick = ::showLongPressActionPage
        )
    )

    private fun tutorialSwitchSpecs(): List<PlayerSettingsSwitchSpec> = listOf(
        PlayerSettingsSwitchSpec(
            title = context.getString(R.string.settings_edge_swipe_back),
            checked = playerPrefs.edgeSwipeBack
        ) { checked ->
            playerPrefs.edgeSwipeBack = checked
        }
    )

    private fun moreIntroSwitchSpecs(): List<PlayerSettingsSwitchSpec> = listOf(
        PlayerSettingsSwitchSpec(
            title = context.getString(R.string.settings_skip_intro_outro),
            checked = playerPrefs.skipIntroOutro
        ) { checked ->
            playerPrefs.skipIntroOutro = checked
        }
    )

    private fun moreScreenSwitchSpecs(): List<PlayerSettingsSwitchSpec> = listOf(
        PlayerSettingsSwitchSpec(
            title = context.getString(R.string.settings_keep_screen_on),
            checked = playerPrefs.keepScreenOn
        ) { checked ->
            playerPrefs.keepScreenOn = checked
        }
    )

    private fun moreActionSpecs(): List<PlayerSettingsActionSpec> = listOf(
        PlayerSettingsActionSpec(
            title = context.getString(R.string.settings_reset_defaults)
        ) {
            playerPrefs.resetToDefaults()
            onPlayerPrefsReset()
            showPrimaryPage()
            setupPrimarySwitches()
            applySettingsSheetOpacity()
            applySheetWindowBackdrop()
        }
    )

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

    private fun playStreamUrl(url: String) {
        playerPrefs.lastStreamUrl = url
        viewModel.playStream(url)
        dismiss()
    }

    private fun addPlaybackSpeedSeekRow() {
        rows.addSeekRow(
            title = context.getString(R.string.settings_playback_speed),
            min = 0,
            maxValue = formatter.speedToProgress(PlayerSettingsFormatter.SPEED_MAX),
            value = formatter.speedToProgress(playerPrefs.speed),
            label = { progress -> formatter.playbackSpeedLabelFor(formatter.progressToSpeed(progress)) },
            commitOnStop = true
        ) { progress ->
            val speed = formatter.progressToSpeed(progress)
            playerPrefs.speed = speed
            viewModel.setSpeed(
                speed,
                PlayerPlaybackSettings.pitchFor(speed, playerPrefs.speedPreservePitch)
            )
        }
    }

    private fun setSeekIntervalFromChoiceLabel(selected: String) {
        val choice = formatter.seekIntervalChoices().firstOrNull { context.getString(it.labelRes) == selected } ?: return
        playerPrefs.seekInterval = choice.seconds
    }

    private fun applyVideoAdjustmentsFromPrefs() {
        playerManager.applyVideoAdjustments(
            0f,
            playerPrefs.contrastAdjustment / 100f,
            playerPrefs.saturationAdjustment / 100f
        )
    }

    private fun addAspectRow(title: String, ratio: AspectRatio) {
        rows.addRadioRow(
            title = title,
            checked = playerPrefs.aspectRatio == ratio
        ) {
            val selection = PlayerContentFrameSettingsPolicy.onAspectRatioSelected(
                aspectRatio = ratio,
                currentContentFrameMode = playerPrefs.contentFrameMode
            )
            playerPrefs.aspectRatio = selection.aspectRatio
            selection.contentFrameOverride?.let { playerPrefs.contentFrameMode = it }
            viewModel.setAspectRatio(selection.aspectRatio)
            onAspectRatioChanged()
            rebuildCurrentDetail(SettingsPage.ASPECT, context.getString(R.string.player_sheet_aspect_ratio))
        }
    }

    private fun rebuildCurrentDetail(page: SettingsPage, title: String) {
        showDetailPage(page, title)
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

    private fun <T : View> requireView(id: Int): T =
        findViewById<T>(id) ?: error("Missing player settings view: $id")

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

}
