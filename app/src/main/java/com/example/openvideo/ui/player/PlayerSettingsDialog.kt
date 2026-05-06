package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
    private val onRequestPickSubtitle: () -> Unit = {}
) : Dialog(context) {

    private val navIds = intArrayOf(
        R.id.nav_playback,
        R.id.nav_video,
        R.id.nav_audio,
        R.id.nav_subtitle,
        R.id.nav_gesture,
        R.id.nav_other
    )

    private val sectionIds = intArrayOf(
        R.id.section_playback,
        R.id.section_video,
        R.id.section_audio,
        R.id.section_subtitle,
        R.id.section_gesture,
        R.id.section_other
    )

    private val speedOptions = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private val loopOptions = LoopMode.entries.toTypedArray()
    private val seekOptions = intArrayOf(5, 10, 15)
    private val aspectOptions = AspectRatio.entries.toTypedArray()
    private val rotationOptions = intArrayOf(0, 90, 180, 270)
    private val audioChannelOptions = AudioChannel.entries.toTypedArray()
    private val subtitleBgOptions = SubtitleBgStyle.entries.toTypedArray()
    private val subtitleEncodingOptions = arrayOf("auto", "UTF-8", "GBK", "GB2312", "Big5", "Shift_JIS", "EUC-KR")
    private val gestureOptions = GestureAction.entries.toTypedArray()
    private val doubleTapOptions = DoubleTapAction.entries.toTypedArray()
    private val longPressOptions = LongPressAction.entries.toTypedArray()
    private val sensitivityOptions = intArrayOf(1, 2, 3)
    private val autoHideOptions = intArrayOf(3, 5, 8, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_player_settings)

        window?.apply {
            val bounds = PlayerSettingsLayoutPolicy.dialogBounds(
                context.resources.displayMetrics.widthPixels,
                context.resources.displayMetrics.heightPixels
            )
            setLayout(bounds.width, bounds.height)
            setBackgroundDrawableResource(android.R.color.transparent)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        runCatching {
            setupNavigation()
            setupRowLabels()
            bindAllSections()
            setupResetDefaults()
            setSelectedNav(R.id.nav_playback)
            showSection(R.id.section_playback)
        }.onFailure { error ->
            CrashLogger.logPlayerError(context, error)
        }
    }

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
            row(navId)?.setOnClickListener {
                setSelectedNav(navId)
                showSection(sectionId)
            }
        }
    }

    private fun setupRowLabels() {
        setValueRow(R.id.row_speed, R.string.settings_playback_speed)
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

    private fun bindAllSections() {
        setupPlaybackSection()
        setupVideoSection()
        setupAudioSection()
        setupSubtitleSection()
        setupGestureSection()
        setupOtherSection()
        bindDualRememberSwitches()
        bindDualAutoNextSwitches()
    }

    private fun setupPlaybackSection() {
        setupSpeedRow()
        bindValueChoice(
            rowId = R.id.row_loop,
            options = loopOptions,
            current = { playerPrefs.loopMode },
            label = ::loopLabel,
            onSelected = { mode ->
                playerPrefs.loopMode = mode
                viewModel.setRepeatMode(PlayerPlaybackSettings.repeatModeFor(mode))
            }
        )
        bindBooleanValue(R.id.row_skip, playerPrefs.skipIntroOutro) { playerPrefs.skipIntroOutro = it }
        bindValueChoice(
            rowId = R.id.row_seek,
            options = seekOptions.toTypedArray(),
            current = { playerPrefs.seekInterval },
            label = { "${it}s" },
            onSelected = { playerPrefs.seekInterval = it }
        )
        bindSwitch(switch(R.id.row_hw), playerPrefs.hwAcceleration) { checked ->
            playerPrefs.hwAcceleration = checked
            viewModel.setDecodeMode(if (checked) DecodeMode.HARD else DecodeMode.SOFT)
        }
        bindSwitch(switch(R.id.row_pause), playerPrefs.pauseOnExit) { playerPrefs.pauseOnExit = it }
        bindSwitch(switch(R.id.row_bg_audio), playerPrefs.bgAudio) { playerPrefs.bgAudio = it }
    }

    private fun setupSpeedRow() {
        bindValueChoice(
            rowId = R.id.row_speed,
            options = speedOptions.toTypedArray(),
            current = { playerPrefs.speed },
            label = { "${it}x" },
            onSelected = { speed ->
                playerPrefs.speed = speed
                viewModel.setSpeed(speed, PlayerPlaybackSettings.pitchFor(speed, playerPrefs.speedPreservePitch))
            }
        )
    }

    private fun setupVideoSection() {
        bindValueChoice(
            rowId = R.id.row_aspect,
            options = aspectOptions,
            current = { playerPrefs.aspectRatio },
            label = ::aspectLabel,
            onSelected = { playerPrefs.aspectRatio = it }
        )
        bindValueChoice(
            rowId = R.id.row_rotation,
            options = rotationOptions.toTypedArray(),
            current = { playerPrefs.rotation },
            label = { "$it°" },
            onSelected = { playerPrefs.rotation = it }
        )
        bindSwitch(switch(R.id.row_mirror), playerPrefs.mirror) { playerPrefs.mirror = it }
    }

    private fun setupAudioSection() {
        bindSwitch(switch(R.id.row_pitch), playerPrefs.speedPreservePitch) { checked ->
            playerPrefs.speedPreservePitch = checked
            viewModel.setSpeed(
                playerPrefs.speed,
                PlayerPlaybackSettings.pitchFor(playerPrefs.speed, checked)
            )
        }
        bindSwitch(switch(R.id.row_boost), playerPrefs.volumeBoost) { checked ->
            playerPrefs.volumeBoost = checked
            viewModel.setVolumeBoost(checked)
        }
        bindValueChoice(
            rowId = R.id.row_channel,
            options = audioChannelOptions,
            current = { playerPrefs.audioChannel },
            label = ::audioChannelLabel,
            onSelected = { playerPrefs.audioChannel = it }
        )
        bindValueChoice(
            rowId = R.id.row_delay,
            options = (-1000..1000 step 250).toList().toTypedArray(),
            current = { playerPrefs.audioDelay },
            label = { "${it}ms" },
            onSelected = { playerPrefs.audioDelay = it }
        )
    }

    private fun setupSubtitleSection() {
        action(R.id.row_load_subtitle).setOnClickListener {
            dismiss()
            onRequestPickSubtitle()
        }
        bindSeekRow(
            rowId = R.id.row_subtitle_size,
            min = 12,
            max = 36,
            current = { playerPrefs.subtitleSize },
            label = { "${it}sp" },
            onChanged = { playerPrefs.subtitleSize = it }
        )
        bindValueChoice(
            rowId = R.id.row_subtitle_color,
            options = subtitleColorOptions,
            current = { subtitleColorOptionFor(playerPrefs.subtitleColor) },
            label = { it.label },
            onSelected = { playerPrefs.subtitleColor = it.color }
        )
        bindValueChoice(
            rowId = R.id.row_subtitle_bg,
            options = subtitleBgOptions,
            current = { playerPrefs.subtitleBgStyle },
            label = ::subtitleBgLabel,
            onSelected = { playerPrefs.subtitleBgStyle = it }
        )
        bindSeekRow(
            rowId = R.id.row_subtitle_position,
            min = 0,
            max = 100,
            current = { (playerPrefs.subtitlePosition.coerceIn(0f, 1f) * 100).toInt() },
            label = { "$it%" },
            onChanged = { playerPrefs.subtitlePosition = it / 100f }
        )
        bindValueChoice(
            rowId = R.id.row_subtitle_encoding,
            options = subtitleEncodingOptions,
            current = { playerPrefs.subtitleEncoding },
            label = { if (it == "auto") context.getString(R.string.settings_encoding_auto) else it },
            onSelected = { playerPrefs.subtitleEncoding = it }
        )
    }

    private fun setupGestureSection() {
        bindValueChoice(R.id.row_left_vertical, gestureOptions, { playerPrefs.leftVerticalGesture }, ::gestureLabel) {
            playerPrefs.leftVerticalGesture = it
        }
        bindValueChoice(R.id.row_right_vertical, gestureOptions, { playerPrefs.rightVerticalGesture }, ::gestureLabel) {
            playerPrefs.rightVerticalGesture = it
        }
        bindValueChoice(R.id.row_double_tap, doubleTapOptions, { playerPrefs.doubleTapAction }, ::doubleTapLabel) {
            playerPrefs.doubleTapAction = it
        }
        bindValueChoice(R.id.row_long_press, longPressOptions, { playerPrefs.longPressAction }, ::longPressLabel) {
            playerPrefs.longPressAction = it
        }
        bindValueChoice(R.id.row_horizontal, gestureOptions, { playerPrefs.horizontalSwipeAction }, ::gestureLabel) {
            playerPrefs.horizontalSwipeAction = it
        }
        bindValueChoice(
            rowId = R.id.row_sensitivity,
            options = sensitivityOptions.toTypedArray(),
            current = { playerPrefs.gestureSensitivity },
            label = ::sensitivityLabel,
            onSelected = { playerPrefs.gestureSensitivity = it }
        )
    }

    private fun setupOtherSection() {
        bindSwitch(switch(R.id.row_keep_screen), playerPrefs.keepScreenOn) { playerPrefs.keepScreenOn = it }
        bindValueChoice(
            rowId = R.id.row_auto_hide,
            options = autoHideOptions.toTypedArray(),
            current = { playerPrefs.controlsAutoHide },
            label = ::autoHideLabel,
            onSelected = { playerPrefs.controlsAutoHide = it }
        )
        bindValueChoice(
            rowId = R.id.row_intro,
            options = (0..300 step 30).toList().toTypedArray(),
            current = { playerPrefs.introSeconds },
            label = { "${it}s" },
            onSelected = { playerPrefs.introSeconds = it }
        )
        bindValueChoice(
            rowId = R.id.row_outro,
            options = (0..300 step 30).toList().toTypedArray(),
            current = { playerPrefs.outroSeconds },
            label = { "${it}s" },
            onSelected = { playerPrefs.outroSeconds = it }
        )
    }

    private fun setupResetDefaults() {
        row(R.id.tv_reset_defaults)?.setOnClickListener {
            playerPrefs.resetToDefaults()
            bindAllSections()
        }
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
        sectionIds.forEach { id ->
            row(id)?.visibility = if (id == selectedId) View.VISIBLE else View.GONE
        }
    }

    private fun bindDualRememberSwitches() {
        bindDualSwitch(
            primary = switch(R.id.row_remember),
            mirror = switch(R.id.row_remember_other),
            current = { playerPrefs.rememberProgress },
            onChanged = { playerPrefs.rememberProgress = it }
        )
    }

    private fun bindDualAutoNextSwitches() {
        bindDualSwitch(
            primary = switch(R.id.row_auto_next),
            mirror = switch(R.id.row_auto_next_other),
            current = { playerPrefs.autoPlayNext },
            onChanged = { playerPrefs.autoPlayNext = it }
        )
    }

    private fun bindDualSwitch(
        primary: SwitchMaterial,
        mirror: SwitchMaterial,
        current: () -> Boolean,
        onChanged: (Boolean) -> Unit
    ) {
        lateinit var listener: CompoundButton.OnCheckedChangeListener
        listener = CompoundButton.OnCheckedChangeListener { _, checked ->
            onChanged(checked)
            primary.setOnCheckedChangeListener(null)
            mirror.setOnCheckedChangeListener(null)
            primary.isChecked = checked
            mirror.isChecked = checked
            primary.setOnCheckedChangeListener(listener)
            mirror.setOnCheckedChangeListener(listener)
        }
        primary.setOnCheckedChangeListener(null)
        mirror.setOnCheckedChangeListener(null)
        primary.isChecked = current()
        mirror.isChecked = current()
        primary.setOnCheckedChangeListener(listener)
        mirror.setOnCheckedChangeListener(listener)
    }

    private fun <T> bindValueChoice(
        rowId: Int,
        options: Array<T>,
        current: () -> T,
        label: (T) -> String,
        onSelected: (T) -> Unit
    ) {
        val valueView = value(rowId)
        fun refresh() {
            valueView.text = label(current())
        }
        refresh()
        row(rowId)?.setOnClickListener {
            showChoiceDialog(
                title = title(rowId),
                options = options,
                selected = current(),
                label = label
            ) { selected ->
                onSelected(selected)
                refresh()
            }
        }
    }

    private fun bindBooleanValue(
        rowId: Int,
        initialValue: Boolean,
        onChanged: (Boolean) -> Unit
    ) {
        var valueState = initialValue
        val valueView = value(rowId)
        fun refresh() {
            valueView.setText(if (valueState) R.string.settings_on else R.string.settings_off)
        }
        refresh()
        row(rowId)?.setOnClickListener {
            valueState = !valueState
            onChanged(valueState)
            refresh()
        }
    }

    private fun bindSeekRow(
        rowId: Int,
        min: Int,
        max: Int,
        current: () -> Int,
        label: (Int) -> String,
        onChanged: (Int) -> Unit
    ) {
        val seekBar = seekbar(rowId)
        val valueView = value(rowId)
        val range = max - min
        fun refresh(value: Int) {
            seekBar.progress = (value - min).coerceIn(0, range)
            valueView.text = label(value)
        }
        seekBar.max = range
        refresh(current())
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val value = min + progress
                valueView.text = label(value)
                onChanged(value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    }

    private fun <T> showChoiceDialog(
        title: String,
        options: Array<T>,
        selected: T,
        label: (T) -> String,
        onSelected: (T) -> Unit
    ) {
        val labels = options.map(label).toTypedArray()
        val checked = options.indexOf(selected).coerceAtLeast(0)
        AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                onSelected(options[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun bindSwitch(
        view: SwitchMaterial,
        initialValue: Boolean,
        onChanged: (Boolean) -> Unit
    ) {
        view.setOnCheckedChangeListener(null)
        view.isChecked = initialValue
        view.setOnCheckedChangeListener { _, checked -> onChanged(checked) }
    }

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

    private fun title(rowId: Int): String {
        return row(rowId)?.findViewById<TextView>(R.id.row_title)?.text?.toString().orEmpty()
    }

    private fun value(rowId: Int): TextView =
        row(rowId)?.findViewById(R.id.row_value) ?: TextView(context)

    private fun switch(rowId: Int): SwitchMaterial =
        row(rowId)?.findViewById(R.id.row_switch) ?: SwitchMaterial(context)

    private fun seekbar(rowId: Int): SeekBar =
        row(rowId)?.findViewById(R.id.row_seekbar) ?: SeekBar(context)

    private fun action(rowId: Int): TextView =
        row(rowId) as? TextView ?: TextView(context)

    private fun loopLabel(value: LoopMode): String = when (value) {
        LoopMode.OFF -> context.getString(R.string.settings_loop_off)
        LoopMode.SINGLE -> context.getString(R.string.settings_loop_single)
        LoopMode.LIST -> context.getString(R.string.settings_loop_list)
    }

    private fun aspectLabel(value: AspectRatio): String = when (value) {
        AspectRatio.FIT -> context.getString(R.string.settings_ratio_fit)
        AspectRatio.FILL -> context.getString(R.string.settings_ratio_fill)
        AspectRatio.CROP -> context.getString(R.string.settings_ratio_crop)
        AspectRatio.STRETCH -> context.getString(R.string.settings_ratio_stretch)
        AspectRatio.RATIO_4_3 -> context.getString(R.string.settings_ratio_4_3)
        AspectRatio.RATIO_16_9 -> context.getString(R.string.settings_ratio_16_9)
    }

    private fun audioChannelLabel(value: AudioChannel): String = when (value) {
        AudioChannel.STEREO -> context.getString(R.string.settings_audio_stereo)
        AudioChannel.LEFT -> context.getString(R.string.settings_audio_left)
        AudioChannel.RIGHT -> context.getString(R.string.settings_audio_right)
    }

    private fun subtitleBgLabel(value: SubtitleBgStyle): String = when (value) {
        SubtitleBgStyle.NONE -> context.getString(R.string.settings_subtitle_bg_none)
        SubtitleBgStyle.SEMI_TRANSPARENT -> context.getString(R.string.settings_subtitle_bg_semi)
        SubtitleBgStyle.OPAQUE -> context.getString(R.string.settings_subtitle_bg_opaque)
    }

    private fun gestureLabel(value: GestureAction): String = when (value) {
        GestureAction.BRIGHTNESS -> context.getString(R.string.settings_action_brightness)
        GestureAction.VOLUME -> context.getString(R.string.settings_action_volume)
        GestureAction.SEEK -> context.getString(R.string.settings_action_seek)
        GestureAction.NONE -> context.getString(R.string.settings_action_none)
    }

    private fun doubleTapLabel(value: DoubleTapAction): String = when (value) {
        DoubleTapAction.PLAY_PAUSE -> context.getString(R.string.settings_double_tap_pause)
        DoubleTapAction.FORWARD -> context.getString(R.string.settings_double_tap_forward)
        DoubleTapAction.BACKWARD -> context.getString(R.string.settings_double_tap_backward)
        DoubleTapAction.NONE -> context.getString(R.string.settings_double_tap_none)
    }

    private fun longPressLabel(value: LongPressAction): String = when (value) {
        LongPressAction.SPEED -> context.getString(R.string.settings_double_tap_playback)
        LongPressAction.NONE -> context.getString(R.string.settings_action_none)
    }

    private fun sensitivityLabel(value: Int): String = when (value) {
        1 -> context.getString(R.string.settings_sensitivity_low)
        3 -> context.getString(R.string.settings_sensitivity_high)
        else -> context.getString(R.string.settings_sensitivity_medium)
    }

    private fun autoHideLabel(value: Int): String = when (value) {
        3 -> context.getString(R.string.settings_hide_3s)
        5 -> context.getString(R.string.settings_hide_5s)
        8 -> context.getString(R.string.settings_hide_8s)
        else -> context.getString(R.string.settings_hide_never)
    }

    private data class SubtitleColorOption(val label: String, val color: Int)

    private val subtitleColorOptions: Array<SubtitleColorOption> by lazy {
        arrayOf(
            SubtitleColorOption("白色", 0xFFFFFFFF.toInt()),
            SubtitleColorOption("黄色", 0xFFFFEB3B.toInt()),
            SubtitleColorOption("绿色", 0xFF8BC34A.toInt()),
            SubtitleColorOption("蓝色", 0xFF64B5F6.toInt())
        )
    }

    private fun subtitleColorOptionFor(color: Int): SubtitleColorOption {
        return subtitleColorOptions.firstOrNull { it.color == color } ?: subtitleColorOptions.first()
    }
}
