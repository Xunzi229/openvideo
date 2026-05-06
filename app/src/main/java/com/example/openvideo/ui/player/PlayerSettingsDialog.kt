package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import com.example.openvideo.R
import com.example.openvideo.core.diagnostics.CrashLogger
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.*
import com.google.android.material.switchmaterial.SwitchMaterial

class PlayerSettingsDialog(
    context: Context,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val onRequestPickSubtitle: () -> Unit = {}
) : Dialog(context) {

    private val navIds = intArrayOf(
        R.id.nav_playback, R.id.nav_video, R.id.nav_audio,
        R.id.nav_subtitle, R.id.nav_gesture, R.id.nav_other
    )
    private val loopModes = arrayOf("off", "single", "list")
    private var loopModeIndex = 0
    private val seekIntervals = intArrayOf(5, 10, 15)
    private var seekIntervalIndex = 1
    private val autoHideOptions = intArrayOf(3, 5, 8, 0)
    private var autoHideIndex = 0

    // 画面
    private val aspectRatios = AspectRatio.entries.toTypedArray()
    private var aspectRatioIndex = 0
    private val rotations = intArrayOf(0, 90, 180, 270)
    private var rotationIndex = 0

    // 声道
    private val audioChannels = AudioChannel.entries.toTypedArray()
    private var audioChannelIndex = 0

    // 字幕
    private val subtitleBgStyles = SubtitleBgStyle.entries.toTypedArray()
    private var subtitleBgIndex = 0
    private val encodings = arrayOf("auto", "UTF-8", "GBK", "GB2312", "Big5", "Shift_JIS", "EUC-KR")
    private var encodingIndex = 0

    // 手势
    private val gestureActions = GestureAction.entries.toTypedArray()
    private val doubleTapActions = DoubleTapAction.entries.toTypedArray()
    private val longPressActions = LongPressAction.entries.toTypedArray()
    private var leftGestureIndex = 0
    private var rightGestureIndex = 0
    private var doubleTapIndex = 0
    private var longPressIndex = 0
    private var horizontalIndex = 0
    private var sensitivityIndex = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_player_settings)

        window?.apply {
            val bounds = PlayerSettingsLayoutPolicy.dialogBounds(
                context.resources.displayMetrics.widthPixels,
                context.resources.displayMetrics.heightPixels
            )
            setLayout(
                bounds.width,
                bounds.height
            )
            setBackgroundDrawableResource(android.R.color.transparent)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        runCatching {
            setupNavigation()
            setupRowLabels()
            setupPlaybackSection()
            setupVideoSection()
            setupAudioSection()
            setupSubtitleSection()
            setupGestureSection()
            setupOtherSection()
            setupResetDefaults()

            bindDualRememberSwitches()
            bindDualAutoNextSwitches()

            setSelectedNav(R.id.nav_playback)
            showSection(R.id.section_playback)
        }.onFailure { error ->
            CrashLogger.logPlayerError(context, error)
        }
    }

    // ── Navigation ──

    private fun setupNavigation() {
        val navSectionMap = mapOf(
            R.id.nav_playback to R.id.section_playback,
            R.id.nav_video to R.id.section_video,
            R.id.nav_audio to R.id.section_audio,
            R.id.nav_subtitle to R.id.section_subtitle,
            R.id.nav_gesture to R.id.section_gesture,
            R.id.nav_other to R.id.section_other
        )
        navSectionMap.forEach { (navId, sectionId) ->
            findViewById<View>(navId)?.setOnClickListener {
                setSelectedNav(navId)
                showSection(sectionId)
            }
        }
    }

    private fun setupRowLabels() {
        setValueRow(R.id.row_loop, R.string.settings_loop_mode)
        setValueRow(R.id.row_skip, R.string.settings_skip_intro_outro)
        setValueRow(R.id.row_seek, R.string.settings_seek_interval)
        setSwitchRow(R.id.row_remember, R.string.settings_remember_progress)
        setSwitchRow(R.id.row_hw, R.string.settings_hardware_acceleration)
        setSwitchRow(R.id.row_pause, R.string.settings_pause_on_exit)
        setSwitchRow(R.id.row_auto_next, R.string.settings_auto_play_next)
        setSwitchRow(R.id.row_bg_audio, R.string.settings_bg_audio)

        setValueRow(R.id.row_aspect, R.string.settings_aspect_ratio_label)
        setValueRow(R.id.row_rotation, R.string.settings_rotation)
        setSwitchRow(R.id.row_mirror, R.string.settings_mirror)

        setSwitchRow(R.id.row_pitch, R.string.settings_speed_preserve_pitch)
        setSwitchRow(R.id.row_boost, R.string.settings_volume_boost)
        setValueRow(R.id.row_channel, R.string.settings_audio_channel_label)
        setValueRow(R.id.row_delay, R.string.settings_audio_delay)

        action(R.id.row_load_subtitle).setText(R.string.settings_load_subtitle)
        setSeekRow(R.id.row_subtitle_size, R.string.settings_subtitle_size)
        setValueRow(R.id.row_subtitle_color, R.string.settings_subtitle_color)
        row(R.id.row_subtitle_color)?.visibility = View.GONE
        setValueRow(R.id.row_subtitle_bg, R.string.settings_subtitle_bg)
        setSeekRow(R.id.row_subtitle_position, R.string.settings_subtitle_position)
        setValueRow(R.id.row_subtitle_encoding, R.string.settings_subtitle_encoding)

        setValueRow(R.id.row_left_vertical, R.string.settings_left_vertical)
        setValueRow(R.id.row_right_vertical, R.string.settings_right_vertical)
        setValueRow(R.id.row_double_tap, R.string.settings_double_tap_action)
        setValueRow(R.id.row_long_press, R.string.settings_double_tap_playback)
        setValueRow(R.id.row_horizontal, R.string.settings_horizontal_swipe)
        setValueRow(R.id.row_sensitivity, R.string.settings_gesture_sensitivity)

        setSwitchRow(R.id.row_remember_other, R.string.settings_remember_progress)
        setSwitchRow(R.id.row_auto_next_other, R.string.settings_auto_play_next)
        setSwitchRow(R.id.row_keep_screen, R.string.settings_keep_screen_on)
        setValueRow(R.id.row_auto_hide, R.string.settings_controls_auto_hide)
        setValueRow(R.id.row_intro, R.string.settings_skip_intro_seconds)
        setValueRow(R.id.row_outro, R.string.settings_skip_outro_seconds)
    }

    private fun setSelectedNav(selectedId: Int) {
        navIds.forEach { id ->
            val nav = findViewById<TextView>(id) ?: return@forEach
            val selected = id == selectedId
            nav.setBackgroundResource(if (selected) R.drawable.bg_settings_nav_selected else android.R.color.transparent)
            nav.setTextColor(context.getColor(if (selected) R.color.ov_text_primary else R.color.ov_text_secondary))
        }
    }

    private fun showSection(selectedId: Int) {
        val allSections = intArrayOf(
            R.id.section_playback, R.id.section_video,
            R.id.section_audio, R.id.section_subtitle,
            R.id.section_gesture, R.id.section_other
        )
        allSections.forEach { id ->
            findViewById<View>(id)?.visibility = if (id == selectedId) View.VISIBLE else View.GONE
        }
    }

    // ── 播放分组 ──

    private fun setupPlaybackSection() {
        // Hide inline detailed controls; expose a single launcher row to open the dedicated playback settings page
        findViewById<android.widget.RadioGroup?>(R.id.rg_speed)?.visibility = View.GONE
        row(R.id.row_loop)?.findViewById<TextView>(R.id.row_title)?.text = context.getString(R.string.settings_nav_playback)
        row(R.id.row_loop)?.setOnClickListener {
            val fa = context as? androidx.fragment.app.FragmentActivity
            fa?.let {
                val sheet = PlayerPlaybackSettingsSheet()
                sheet.show(it.supportFragmentManager, "player_playback_settings")
            }
        }
        // hide other inline rows
        row(R.id.row_skip)?.visibility = View.GONE
        row(R.id.row_seek)?.visibility = View.GONE
        row(R.id.row_remember)?.visibility = View.GONE
        row(R.id.row_hw)?.visibility = View.GONE
        row(R.id.row_pause)?.visibility = View.GONE
        row(R.id.row_auto_next)?.visibility = View.GONE
        row(R.id.row_bg_audio)?.visibility = View.GONE
    }

    private fun setupSpeed() {
        val speed = playerPrefs.speed
        val speedGroup = findViewById<RadioGroup>(R.id.rg_speed)
        speedGroup.check(
            when (speed) {
                0.5f -> R.id.rb_speed_0_5
                0.75f -> R.id.rb_speed_0_75
                1.25f -> R.id.rb_speed_1_25
                1.5f -> R.id.rb_speed_1_5
                2.0f -> R.id.rb_speed_2_0
                else -> R.id.rb_speed_1_0
            }
        )
        viewModel.setSpeed(speed, PlayerPlaybackSettings.pitchFor(speed, playerPrefs.speedPreservePitch))
        speedGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedSpeed = when (checkedId) {
                R.id.rb_speed_0_5 -> 0.5f
                R.id.rb_speed_0_75 -> 0.75f
                R.id.rb_speed_1_25 -> 1.25f
                R.id.rb_speed_1_5 -> 1.5f
                R.id.rb_speed_2_0 -> 2.0f
                else -> 1.0f
            }
            playerPrefs.speed = selectedSpeed
            viewModel.setSpeed(
                selectedSpeed,
                PlayerPlaybackSettings.pitchFor(selectedSpeed, playerPrefs.speedPreservePitch)
            )
        }
    }

    private fun setupLoopRow() {
        val mode = playerPrefs.loopMode
        loopModeIndex = loopModes.indexOf(mode.key).takeIf { it >= 0 } ?: 0
        val loopValue = value(R.id.row_loop)
        fun update() {
            val textRes = when (loopModes[loopModeIndex]) {
                "single" -> R.string.settings_loop_single
                "list" -> R.string.settings_loop_list
                else -> R.string.settings_loop_off
            }
            loopValue.setText(textRes)
        }
        update()
        loopValue.setOnClickListener {
            loopModeIndex = (loopModeIndex + 1) % loopModes.size
            playerPrefs.loopMode = LoopMode.fromKey(loopModes[loopModeIndex])
            viewModel.setRepeatMode(PlayerPlaybackSettings.repeatModeFor(playerPrefs.loopMode))
            update()
        }
    }

    private fun setupSeekIntervalRow() {
        val interval = playerPrefs.seekInterval
        seekIntervalIndex = seekIntervals.indexOf(interval).takeIf { it >= 0 } ?: 1
        val seekValue = value(R.id.row_seek)
        fun update() {
            val textRes = when (seekIntervals[seekIntervalIndex]) {
                5 -> R.string.settings_seek_5s
                15 -> R.string.settings_seek_15s
                else -> R.string.settings_seek_10s
            }
            seekValue.setText(textRes)
        }
        update()
        seekValue.setOnClickListener {
            seekIntervalIndex = (seekIntervalIndex + 1) % seekIntervals.size
            playerPrefs.seekInterval = seekIntervals[seekIntervalIndex]
            update()
        }
    }

    private fun setupPlaybackSwitches() {
        val hwAccelSwitch = switch(R.id.row_hw)
        val pauseOnExitSwitch = switch(R.id.row_pause)
        val bgAudioSwitch = switch(R.id.row_bg_audio)
        val skipValue = value(R.id.row_skip)

        var skipEnabled = playerPrefs.skipIntroOutro
        fun updateSkipText() {
            skipValue.setText(if (skipEnabled) R.string.settings_on else R.string.settings_off)
        }
        updateSkipText()
        skipValue.setOnClickListener {
            skipEnabled = !skipEnabled
            playerPrefs.skipIntroOutro = skipEnabled
            updateSkipText()
        }

        bindSwitch(hwAccelSwitch, playerPrefs.hwAcceleration) { checked ->
            playerPrefs.hwAcceleration = checked
            viewModel.setDecodeMode(if (checked) DecodeMode.HARD else DecodeMode.SOFT)
        }
        bindSwitch(pauseOnExitSwitch, playerPrefs.pauseOnExit) { playerPrefs.pauseOnExit = it }
        bindSwitch(bgAudioSwitch, playerPrefs.bgAudio) { playerPrefs.bgAudio = it }
    }

    // ── 画面分组 ──

    private fun setupVideoSection() {
        // 打开独立的“显示设置”页面以逐层配置（画面比例 / 旋转 / 镜像 / 自动方向）
        aspectRatioIndex = aspectRatios.indexOf(playerPrefs.aspectRatio).takeIf { it >= 0 } ?: 0
        val aspectValue = value(R.id.row_aspect)
        fun updateAspect() {
            val textRes = when (aspectRatios[aspectRatioIndex]) {
                AspectRatio.FIT -> R.string.settings_ratio_fit
                AspectRatio.FILL -> R.string.settings_ratio_fill
                AspectRatio.CROP -> R.string.settings_ratio_crop
                AspectRatio.STRETCH -> R.string.settings_ratio_stretch
                AspectRatio.RATIO_4_3 -> R.string.settings_ratio_4_3
                AspectRatio.RATIO_16_9 -> R.string.settings_ratio_16_9
            }
            aspectValue.setText(textRes)
        }
        updateAspect()
        aspectValue.setOnClickListener {
            // 打开独立页面进行详尽配置
            val fa = context as? androidx.fragment.app.FragmentActivity
            fa?.let {
                val sheet = PlayerDisplaySettingsSheet()
                sheet.show(it.supportFragmentManager, "player_display_settings")
            }
        }

        // 旋转/镜像等项在独立页面中配置，隐藏当前行的直接控制，避免重复
        row(R.id.row_rotation)?.visibility = View.GONE
        row(R.id.row_mirror)?.visibility = View.GONE
    }

    // ── 声音分组 ──

    private fun setupAudioSection() {
        // Hide inline audio controls; repurpose channel row to open dedicated audio settings
        row(R.id.row_pitch)?.visibility = View.GONE
        row(R.id.row_boost)?.visibility = View.GONE
        val channelValue = value(R.id.row_channel)
        channelValue.setText(R.string.settings_nav_audio)
        row(R.id.row_channel)?.setOnClickListener {
            val fa = context as? androidx.fragment.app.FragmentActivity
            fa?.let {
                val sheet = PlayerAudioSettingsSheet()
                sheet.show(it.supportFragmentManager, "player_audio_settings")
            }
        }
        row(R.id.row_delay)?.visibility = View.GONE
    }

    // ── 字幕分组 ──

    private fun setupSubtitleSection() {
        // Hide inline subtitle controls; repurpose the load row to open subtitle detail page
        row(R.id.row_subtitle_size)?.visibility = View.GONE
        row(R.id.row_subtitle_color)?.visibility = View.GONE
        row(R.id.row_subtitle_bg)?.visibility = View.GONE
        row(R.id.row_subtitle_position)?.visibility = View.GONE
        row(R.id.row_subtitle_encoding)?.visibility = View.GONE

        val loadSubtitle = action(R.id.row_load_subtitle)
        loadSubtitle.text = context.getString(R.string.settings_nav_subtitle)
        loadSubtitle.setOnClickListener {
            val fa = context as? androidx.fragment.app.FragmentActivity
            fa?.let {
                val sheet = PlayerSubtitleSettingsSheet()
                sheet.show(it.supportFragmentManager, "player_subtitle_settings")
            }
        }
    }

    // ── 手势分组 ──

    private fun setupGestureSection() {
        // Hide inline gesture controls; repurpose a row to open detailed gesture settings page
        row(R.id.row_left_vertical)?.visibility = View.GONE
        row(R.id.row_right_vertical)?.visibility = View.GONE
        row(R.id.row_double_tap)?.visibility = View.GONE
        row(R.id.row_long_press)?.visibility = View.GONE
        row(R.id.row_horizontal)?.visibility = View.GONE
        row(R.id.row_sensitivity)?.visibility = View.GONE

        val launcher = value(R.id.row_left_vertical)
        launcher.text = context.getString(R.string.settings_nav_gesture)
        row(R.id.row_left_vertical)?.setOnClickListener {
            val fa = context as? androidx.fragment.app.FragmentActivity
            fa?.let {
                val sheet = PlayerGestureSettingsSheet()
                sheet.show(it.supportFragmentManager, "player_gesture_settings")
            }
        }
    }

    // ── 其他分组 ──

    private fun setupOtherSection() {
        // 屏幕常亮
        val keepScreenSwitch = switch(R.id.row_keep_screen)
        bindSwitch(keepScreenSwitch, playerPrefs.keepScreenOn) { playerPrefs.keepScreenOn = it }

        // 控制栏自动隐藏
        autoHideIndex = autoHideOptions.indexOf(playerPrefs.controlsAutoHide).takeIf { it >= 0 } ?: 0
        val autoHideValue = value(R.id.row_auto_hide)
        fun updateAutoHide() {
            val textRes = when (autoHideOptions[autoHideIndex]) {
                3 -> R.string.settings_hide_3s
                5 -> R.string.settings_hide_5s
                8 -> R.string.settings_hide_8s
                else -> R.string.settings_hide_never
            }
            autoHideValue.setText(textRes)
        }
        updateAutoHide()
        autoHideValue.setOnClickListener {
            autoHideIndex = (autoHideIndex + 1) % autoHideOptions.size
            playerPrefs.controlsAutoHide = autoHideOptions[autoHideIndex]
            updateAutoHide()
        }

        // 跳过片头
        val introValue = value(R.id.row_intro)
        fun updateIntro() {
            introValue.text = "${playerPrefs.introSeconds}"
        }
        updateIntro()
        introValue.setOnClickListener {
            val current = playerPrefs.introSeconds
            playerPrefs.introSeconds = if (current >= 300) 0 else current + 30
            updateIntro()
        }

        // 跳过片尾
        val outroValue = value(R.id.row_outro)
        fun updateOutro() {
            outroValue.text = "${playerPrefs.outroSeconds}"
        }
        updateOutro()
        outroValue.setOnClickListener {
            val current = playerPrefs.outroSeconds
            playerPrefs.outroSeconds = if (current >= 300) 0 else current + 30
            updateOutro()
        }
    }

    // ── 恢复默认 ──

    private fun setupResetDefaults() {
        findViewById<View>(R.id.tv_reset_defaults)?.setOnClickListener {
            playerPrefs.resetToDefaults()
            // Re-setup all sections
            setupPlaybackSection()
            setupVideoSection()
            setupAudioSection()
            setupSubtitleSection()
            setupGestureSection()
            setupOtherSection()
            bindDualRememberSwitches()
            bindDualAutoNextSwitches()
        }
    }

    private fun bindDualRememberSwitches() {
        val primary = switch(R.id.row_remember)
        val mirror = switch(R.id.row_remember_other)
        lateinit var listener: CompoundButton.OnCheckedChangeListener
        listener = CompoundButton.OnCheckedChangeListener { _, checked ->
            playerPrefs.rememberProgress = checked
            primary.setOnCheckedChangeListener(null)
            mirror.setOnCheckedChangeListener(null)
            primary.isChecked = checked
            mirror.isChecked = checked
            primary.setOnCheckedChangeListener(listener)
            mirror.setOnCheckedChangeListener(listener)
        }
        primary.setOnCheckedChangeListener(null)
        mirror.setOnCheckedChangeListener(null)
        primary.isChecked = playerPrefs.rememberProgress
        mirror.isChecked = playerPrefs.rememberProgress
        primary.setOnCheckedChangeListener(listener)
        mirror.setOnCheckedChangeListener(listener)
    }

    private fun bindDualAutoNextSwitches() {
        val primary = switch(R.id.row_auto_next)
        val mirror = switch(R.id.row_auto_next_other)
        lateinit var listener: CompoundButton.OnCheckedChangeListener
        listener = CompoundButton.OnCheckedChangeListener { _, checked ->
            playerPrefs.autoPlayNext = checked
            primary.setOnCheckedChangeListener(null)
            mirror.setOnCheckedChangeListener(null)
            primary.isChecked = checked
            mirror.isChecked = checked
            primary.setOnCheckedChangeListener(listener)
            mirror.setOnCheckedChangeListener(listener)
        }
        primary.setOnCheckedChangeListener(null)
        mirror.setOnCheckedChangeListener(null)
        primary.isChecked = playerPrefs.autoPlayNext
        mirror.isChecked = playerPrefs.autoPlayNext
        primary.setOnCheckedChangeListener(listener)
        mirror.setOnCheckedChangeListener(listener)
    }

    // ── Utility ──

    private fun row(rowId: Int): View? = findViewById(rowId)

    private fun setValueRow(rowId: Int, titleRes: Int) {
        row(rowId)?.findViewById<TextView>(R.id.row_title)?.setText(titleRes)
    }

    private fun setSwitchRow(rowId: Int, titleRes: Int) {
        row(rowId)?.findViewById<TextView>(R.id.row_title)?.setText(titleRes)
    }

    private fun setSeekRow(rowId: Int, titleRes: Int) {
        row(rowId)?.findViewById<TextView>(R.id.row_title)?.setText(titleRes)
    }

    private fun value(rowId: Int): TextView =
        row(rowId)?.findViewById(R.id.row_value) ?: TextView(context)

    private fun switch(rowId: Int): SwitchMaterial =
        row(rowId)?.findViewById(R.id.row_switch) ?: SwitchMaterial(context)

    private fun seekbar(rowId: Int): SeekBar =
        row(rowId)?.findViewById(R.id.row_seekbar) ?: SeekBar(context)

    private fun action(rowId: Int): TextView = row(rowId) as? TextView ?: TextView(context)

    private fun bindSwitch(
        view: SwitchMaterial,
        initialValue: Boolean,
        onChanged: (Boolean) -> Unit
    ) {
        view.setOnCheckedChangeListener(null)
        view.isChecked = initialValue
        view.setOnCheckedChangeListener { _, checked -> onChanged(checked) }
    }
}
