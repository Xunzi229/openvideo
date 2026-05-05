package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import com.example.openvideo.R
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.*
import com.google.android.material.switchmaterial.SwitchMaterial

class PlayerSettingsDialog(
    context: Context,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs
) : Dialog(context) {

    private val navIds = intArrayOf(
        R.id.nav_playback, R.id.nav_video, R.id.nav_audio,
        R.id.nav_subtitle, R.id.nav_gesture, R.id.nav_other
    )
    private val sectionIds = intArrayOf(
        R.id.section_playback, R.id.section_video, R.id.section_audio,
        R.id.section_subtitle, R.id.section_gesture, R.id.section_other
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
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.92f).toInt(),
                (context.resources.displayMetrics.heightPixels * 0.78f).toInt()
            )
            setBackgroundDrawableResource(android.R.color.transparent)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        setupNavigation()
        setupPlaybackSection()
        setupVideoSection()
        setupAudioSection()
        setupSubtitleSection()
        setupGestureSection()
        setupOtherSection()
        setupResetDefaults()

        setSelectedNav(R.id.nav_playback)
        showSection(R.id.section_playback)
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
            findViewById<View>(navId).setOnClickListener {
                setSelectedNav(navId)
                showSection(sectionId)
            }
        }
    }

    private fun setSelectedNav(selectedId: Int) {
        navIds.forEach { id ->
            val nav = findViewById<TextView>(id)
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
            findViewById<View>(id).visibility = if (id == selectedId) View.VISIBLE else View.GONE
        }
    }

    // ── 播放分组 ──

    private fun setupPlaybackSection() {
        setupSpeed()
        setupLoopRow()
        setupSeekIntervalRow()
        setupPlaybackSwitches()
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
        viewModel.setSpeed(speed)
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
            viewModel.setSpeed(selectedSpeed)
        }
    }

    private fun setupLoopRow() {
        val mode = playerPrefs.loopMode
        loopModeIndex = loopModes.indexOf(mode.key).takeIf { it >= 0 } ?: 0
        val loopValue = findViewById<TextView>(R.id.tv_loop_value)
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
            update()
        }
    }

    private fun setupSeekIntervalRow() {
        val interval = playerPrefs.seekInterval
        seekIntervalIndex = seekIntervals.indexOf(interval).takeIf { it >= 0 } ?: 1
        val seekValue = findViewById<TextView>(R.id.tv_seek_interval_value)
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
        val rememberSwitch = findViewById<SwitchMaterial>(R.id.switch_remember_progress)
        val hwAccelSwitch = findViewById<SwitchMaterial>(R.id.switch_hardware_accel)
        val pauseOnExitSwitch = findViewById<SwitchMaterial>(R.id.switch_pause_on_exit)
        val autoNextSwitch = findViewById<SwitchMaterial>(R.id.switch_auto_play_next)
        val bgAudioSwitch = findViewById<SwitchMaterial>(R.id.switch_bg_audio)
        val skipValue = findViewById<TextView>(R.id.tv_skip_value)

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

        bindSwitch(rememberSwitch, playerPrefs.rememberProgress) { playerPrefs.rememberProgress = it }
        bindSwitch(hwAccelSwitch, playerPrefs.hwAcceleration) { checked ->
            playerPrefs.hwAcceleration = checked
            viewModel.setDecodeMode(if (checked) DecodeMode.HARD else DecodeMode.SOFT)
        }
        bindSwitch(pauseOnExitSwitch, playerPrefs.pauseOnExit) { playerPrefs.pauseOnExit = it }
        bindSwitch(autoNextSwitch, playerPrefs.autoPlayNext) { playerPrefs.autoPlayNext = it }
        bindSwitch(bgAudioSwitch, playerPrefs.bgAudio) { playerPrefs.bgAudio = it }
    }

    // ── 画面分组 ──

    private fun setupVideoSection() {
        // 画面比例
        aspectRatioIndex = aspectRatios.indexOf(playerPrefs.aspectRatio).takeIf { it >= 0 } ?: 0
        val aspectValue = findViewById<TextView>(R.id.tv_aspect_ratio_value)
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
            aspectRatioIndex = (aspectRatioIndex + 1) % aspectRatios.size
            playerPrefs.aspectRatio = aspectRatios[aspectRatioIndex]
            updateAspect()
        }

        // 画面旋转
        rotationIndex = rotations.indexOf(playerPrefs.rotation).takeIf { it >= 0 } ?: 0
        val rotationValue = findViewById<TextView>(R.id.tv_rotation_value)
        fun updateRotation() {
            rotationValue.text = "${rotations[rotationIndex]}°"
        }
        updateRotation()
        rotationValue.setOnClickListener {
            rotationIndex = (rotationIndex + 1) % rotations.size
            playerPrefs.rotation = rotations[rotationIndex]
            updateRotation()
        }

        // 画面镜像
        val mirrorSwitch = findViewById<SwitchMaterial>(R.id.switch_mirror)
        bindSwitch(mirrorSwitch, playerPrefs.mirror) { playerPrefs.mirror = it }
    }

    // ── 声音分组 ──

    private fun setupAudioSection() {
        // 倍速不变调
        val pitchSwitch = findViewById<SwitchMaterial>(R.id.switch_speed_pitch)
        bindSwitch(pitchSwitch, playerPrefs.speedPreservePitch) { playerPrefs.speedPreservePitch = it }

        // 音量增强
        val boostSwitch = findViewById<SwitchMaterial>(R.id.switch_volume_boost)
        bindSwitch(boostSwitch, playerPrefs.volumeBoost) { playerPrefs.volumeBoost = it }

        // 声道选择
        audioChannelIndex = audioChannels.indexOf(playerPrefs.audioChannel).takeIf { it >= 0 } ?: 0
        val channelValue = findViewById<TextView>(R.id.tv_audio_channel_value)
        fun updateChannel() {
            val textRes = when (audioChannels[audioChannelIndex]) {
                AudioChannel.STEREO -> R.string.settings_audio_stereo
                AudioChannel.LEFT -> R.string.settings_audio_left
                AudioChannel.RIGHT -> R.string.settings_audio_right
            }
            channelValue.setText(textRes)
        }
        updateChannel()
        channelValue.setOnClickListener {
            audioChannelIndex = (audioChannelIndex + 1) % audioChannels.size
            playerPrefs.audioChannel = audioChannels[audioChannelIndex]
            updateChannel()
        }

        // 声音延迟
        val delayValue = findViewById<TextView>(R.id.tv_audio_delay_value)
        fun updateDelay() {
            delayValue.text = "${playerPrefs.audioDelay}ms"
        }
        updateDelay()
        delayValue.setOnClickListener {
            // Cycle: -500, -250, 0, 250, 500
            val current = playerPrefs.audioDelay
            val next = when {
                current < -250 -> -250
                current < 0 -> 0
                current < 250 -> 250
                current < 500 -> 500
                else -> -500
            }
            playerPrefs.audioDelay = next
            updateDelay()
        }
    }

    // ── 字幕分组 ──

    private fun setupSubtitleSection() {
        // 加载外挂字幕
        val loadSubtitle = findViewById<TextView>(R.id.tv_load_subtitle)
        loadSubtitle.setOnClickListener {
            // Will be wired to file picker in PlayerActivity
        }

        // 字幕大小
        val sizeSeekbar = findViewById<SeekBar>(R.id.seekbar_subtitle_size)
        val sizeValue = findViewById<TextView>(R.id.tv_subtitle_size_value)
        val currentSize = playerPrefs.subtitleSize
        sizeSeekbar.progress = (currentSize - 14) / 2
        sizeValue.text = "${currentSize}sp"
        sizeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val size = 14 + progress * 2
                sizeValue.text = "${size}sp"
                if (fromUser) playerPrefs.subtitleSize = size
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // 字幕背景
        subtitleBgIndex = subtitleBgStyles.indexOf(playerPrefs.subtitleBgStyle).takeIf { it >= 0 } ?: 1
        val bgValue = findViewById<TextView>(R.id.tv_subtitle_bg_value)
        fun updateBg() {
            val textRes = when (subtitleBgStyles[subtitleBgIndex]) {
                SubtitleBgStyle.NONE -> R.string.settings_subtitle_bg_none
                SubtitleBgStyle.SEMI_TRANSPARENT -> R.string.settings_subtitle_bg_semi
                SubtitleBgStyle.OPAQUE -> R.string.settings_subtitle_bg_opaque
            }
            bgValue.setText(textRes)
        }
        updateBg()
        bgValue.setOnClickListener {
            subtitleBgIndex = (subtitleBgIndex + 1) % subtitleBgStyles.size
            playerPrefs.subtitleBgStyle = subtitleBgStyles[subtitleBgIndex]
            updateBg()
        }

        // 字幕位置
        val posSeekbar = findViewById<SeekBar>(R.id.seekbar_subtitle_position)
        posSeekbar.progress = (playerPrefs.subtitlePosition * 100).toInt()
        posSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) playerPrefs.subtitlePosition = progress / 100f
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // 字幕编码
        encodingIndex = encodings.indexOf(playerPrefs.subtitleEncoding).takeIf { it >= 0 } ?: 0
        val encodingValue = findViewById<TextView>(R.id.tv_subtitle_encoding_value)
        fun updateEncoding() {
            encodingValue.text = if (encodingIndex == 0)
                context.getString(R.string.settings_encoding_auto)
            else encodings[encodingIndex]
        }
        updateEncoding()
        encodingValue.setOnClickListener {
            encodingIndex = (encodingIndex + 1) % encodings.size
            playerPrefs.subtitleEncoding = encodings[encodingIndex]
            updateEncoding()
        }
    }

    // ── 手势分组 ──

    private fun setupGestureSection() {
        // 左侧上下滑动
        leftGestureIndex = gestureActions.indexOf(playerPrefs.leftVerticalGesture).takeIf { it >= 0 } ?: 0
        val leftValue = findViewById<TextView>(R.id.tv_left_vertical_value)
        fun updateLeft() {
            leftValue.setText(when (gestureActions[leftGestureIndex]) {
                GestureAction.BRIGHTNESS -> R.string.settings_action_brightness
                GestureAction.VOLUME -> R.string.settings_action_volume
                GestureAction.SEEK -> R.string.settings_action_seek
                GestureAction.NONE -> R.string.settings_action_none
            })
        }
        updateLeft()
        leftValue.setOnClickListener {
            leftGestureIndex = (leftGestureIndex + 1) % gestureActions.size
            playerPrefs.leftVerticalGesture = gestureActions[leftGestureIndex]
            updateLeft()
        }

        // 右侧上下滑动
        rightGestureIndex = gestureActions.indexOf(playerPrefs.rightVerticalGesture).takeIf { it >= 0 } ?: 1
        val rightValue = findViewById<TextView>(R.id.tv_right_vertical_value)
        fun updateRight() {
            rightValue.setText(when (gestureActions[rightGestureIndex]) {
                GestureAction.BRIGHTNESS -> R.string.settings_action_brightness
                GestureAction.VOLUME -> R.string.settings_action_volume
                GestureAction.SEEK -> R.string.settings_action_seek
                GestureAction.NONE -> R.string.settings_action_none
            })
        }
        updateRight()
        rightValue.setOnClickListener {
            rightGestureIndex = (rightGestureIndex + 1) % gestureActions.size
            playerPrefs.rightVerticalGesture = gestureActions[rightGestureIndex]
            updateRight()
        }

        // 双击操作
        doubleTapIndex = doubleTapActions.indexOf(playerPrefs.doubleTapAction).takeIf { it >= 0 } ?: 0
        val doubleTapValue = findViewById<TextView>(R.id.tv_double_tap_value)
        fun updateDoubleTap() {
            doubleTapValue.setText(when (doubleTapActions[doubleTapIndex]) {
                DoubleTapAction.PLAY_PAUSE -> R.string.settings_double_tap_pause
                DoubleTapAction.FORWARD -> R.string.settings_double_tap_forward
                DoubleTapAction.BACKWARD -> R.string.settings_double_tap_backward
                DoubleTapAction.NONE -> R.string.settings_double_tap_none
            })
        }
        updateDoubleTap()
        doubleTapValue.setOnClickListener {
            doubleTapIndex = (doubleTapIndex + 1) % doubleTapActions.size
            playerPrefs.doubleTapAction = doubleTapActions[doubleTapIndex]
            updateDoubleTap()
        }

        // 长按操作
        longPressIndex = longPressActions.indexOf(playerPrefs.longPressAction).takeIf { it >= 0 } ?: 0
        val longPressValue = findViewById<TextView>(R.id.tv_long_press_value)
        fun updateLongPress() {
            longPressValue.setText(when (longPressActions[longPressIndex]) {
                LongPressAction.SPEED -> R.string.settings_double_tap_playback
                LongPressAction.NONE -> R.string.settings_action_none
            })
        }
        updateLongPress()
        longPressValue.setOnClickListener {
            longPressIndex = (longPressIndex + 1) % longPressActions.size
            playerPrefs.longPressAction = longPressActions[longPressIndex]
            updateLongPress()
        }

        // 水平滑动
        horizontalIndex = gestureActions.indexOf(playerPrefs.horizontalSwipeAction).takeIf { it >= 0 } ?: 2
        val horizontalValue = findViewById<TextView>(R.id.tv_horizontal_swipe_value)
        fun updateHorizontal() {
            horizontalValue.setText(when (gestureActions[horizontalIndex]) {
                GestureAction.BRIGHTNESS -> R.string.settings_action_brightness
                GestureAction.VOLUME -> R.string.settings_action_volume
                GestureAction.SEEK -> R.string.settings_action_seek
                GestureAction.NONE -> R.string.settings_action_none
            })
        }
        updateHorizontal()
        horizontalValue.setOnClickListener {
            horizontalIndex = (horizontalIndex + 1) % gestureActions.size
            playerPrefs.horizontalSwipeAction = gestureActions[horizontalIndex]
            updateHorizontal()
        }

        // 灵敏度
        sensitivityIndex = (playerPrefs.gestureSensitivity - 1).coerceIn(0, 2)
        val sensitivityValue = findViewById<TextView>(R.id.tv_sensitivity_value)
        fun updateSensitivity() {
            sensitivityValue.setText(when (sensitivityIndex) {
                0 -> R.string.settings_sensitivity_low
                1 -> R.string.settings_sensitivity_medium
                else -> R.string.settings_sensitivity_high
            })
        }
        updateSensitivity()
        sensitivityValue.setOnClickListener {
            sensitivityIndex = (sensitivityIndex + 1) % 3
            playerPrefs.gestureSensitivity = sensitivityIndex + 1
            updateSensitivity()
        }
    }

    // ── 其他分组 ──

    private fun setupOtherSection() {
        // 记住播放进度 (read-only mirror of playback section)
        val rememberSwitch = findViewById<SwitchMaterial>(R.id.switch_remember_progress_other)
        rememberSwitch.setOnCheckedChangeListener(null)
        rememberSwitch.isChecked = playerPrefs.rememberProgress

        // 自动播放下一个 (read-only mirror of playback section)
        val autoNextSwitch = findViewById<SwitchMaterial>(R.id.switch_auto_play_next_other)
        autoNextSwitch.setOnCheckedChangeListener(null)
        autoNextSwitch.isChecked = playerPrefs.autoPlayNext

        // 屏幕常亮
        val keepScreenSwitch = findViewById<SwitchMaterial>(R.id.switch_keep_screen_on)
        bindSwitch(keepScreenSwitch, playerPrefs.keepScreenOn) { playerPrefs.keepScreenOn = it }

        // 控制栏自动隐藏
        autoHideIndex = autoHideOptions.indexOf(playerPrefs.controlsAutoHide).takeIf { it >= 0 } ?: 0
        val autoHideValue = findViewById<TextView>(R.id.tv_auto_hide_value)
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
        val introValue = findViewById<TextView>(R.id.tv_intro_seconds_value)
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
        val outroValue = findViewById<TextView>(R.id.tv_outro_seconds_value)
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
        findViewById<View>(R.id.tv_reset_defaults).setOnClickListener {
            playerPrefs.resetToDefaults()
            // Re-setup all sections
            setupPlaybackSection()
            setupVideoSection()
            setupAudioSection()
            setupSubtitleSection()
            setupGestureSection()
            setupOtherSection()
        }
    }

    // ── Utility ──

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
