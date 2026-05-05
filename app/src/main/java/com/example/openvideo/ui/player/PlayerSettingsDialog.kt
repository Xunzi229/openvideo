package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.RadioGroup
import android.widget.TextView
import com.example.openvideo.R
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.player.PlayerManager
import com.google.android.material.switchmaterial.SwitchMaterial

class PlayerSettingsDialog(
    context: Context,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel
) : Dialog(context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
    private val loopModes = arrayOf("off", "single", "list")
    private var loopModeIndex = 0
    private val seekIntervals = intArrayOf(5, 10, 15)
    private var seekIntervalIndex = 1

    companion object {
        private const val PREFS_NAME = "player_settings"
        private const val KEY_SPEED = "speed"
        private const val KEY_LOOP_MODE = "loop_mode"
        private const val KEY_SKIP_INTRO_OUTRO = "skip_intro_outro"
        private const val KEY_SEEK_INTERVAL = "seek_interval"
        private const val KEY_REMEMBER_PROGRESS = "remember_progress"
        private const val KEY_HW_ACCEL = "hw_acceleration"
        private const val KEY_PAUSE_ON_EXIT = "pause_on_exit"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        private const val KEY_BG_AUDIO = "bg_audio"

        private const val DEFAULT_SPEED = 1.0f
        private const val DEFAULT_LOOP_MODE = "list"
        private const val DEFAULT_SEEK_INTERVAL = 10
        private const val DEFAULT_SKIP_INTRO_OUTRO = true
        private const val DEFAULT_REMEMBER_PROGRESS = true
        private const val DEFAULT_HW_ACCEL = true
        private const val DEFAULT_PAUSE_ON_EXIT = false
        private const val DEFAULT_AUTO_PLAY_NEXT = true
        private const val DEFAULT_BG_AUDIO = false
    }

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
        setupResetDefaults()

        setSelectedNav(R.id.nav_playback)
        showSection(R.id.section_playback)
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.nav_playback).setOnClickListener {
            setSelectedNav(R.id.nav_playback)
            showSection(R.id.section_playback)
        }
        findViewById<View>(R.id.nav_video).setOnClickListener {
            setSelectedNav(R.id.nav_video)
            showSection(R.id.section_video)
        }
        findViewById<View>(R.id.nav_audio).setOnClickListener {
            setSelectedNav(R.id.nav_audio)
            showSection(R.id.section_audio)
        }
        findViewById<View>(R.id.nav_subtitle).setOnClickListener {
            setSelectedNav(R.id.nav_subtitle)
            showSection(R.id.section_subtitle)
        }
        findViewById<View>(R.id.nav_gesture).setOnClickListener {
            setSelectedNav(R.id.nav_gesture)
            showSection(R.id.section_gesture)
        }
        findViewById<View>(R.id.nav_other).setOnClickListener {
            setSelectedNav(R.id.nav_other)
            showSection(R.id.section_other)
        }
    }

    private fun setupPlaybackSection() {
        setupSpeed()
        setupLoopRow()
        setupSeekIntervalRow()
        setupPlaybackSwitches()
    }

    private fun setupSpeed() {
        val speed = prefs.getFloat(KEY_SPEED, DEFAULT_SPEED)
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
            prefs.edit().putFloat(KEY_SPEED, selectedSpeed).apply()
            viewModel.setSpeed(selectedSpeed)
        }
    }

    private fun setupLoopRow() {
        val mode = prefs.getString(KEY_LOOP_MODE, DEFAULT_LOOP_MODE) ?: DEFAULT_LOOP_MODE
        loopModeIndex = loopModes.indexOf(mode).takeIf { it >= 0 } ?: 0
        val loopValue = findViewById<TextView>(R.id.tv_loop_value)
        val rowLabel = findViewById<TextView>(R.id.tv_loop_value)
        fun update() {
            val textRes = when (loopModes[loopModeIndex]) {
                "single" -> R.string.settings_loop_single
                "list" -> R.string.settings_loop_list
                else -> R.string.settings_loop_off
            }
            loopValue.setText(textRes)
        }
        update()
        rowLabel.setOnClickListener {
            loopModeIndex = (loopModeIndex + 1) % loopModes.size
            prefs.edit().putString(KEY_LOOP_MODE, loopModes[loopModeIndex]).apply()
            update()
        }
    }

    private fun setupSeekIntervalRow() {
        val interval = prefs.getInt(KEY_SEEK_INTERVAL, DEFAULT_SEEK_INTERVAL)
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
            prefs.edit().putInt(KEY_SEEK_INTERVAL, seekIntervals[seekIntervalIndex]).apply()
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

        var skipEnabled = prefs.getBoolean(KEY_SKIP_INTRO_OUTRO, DEFAULT_SKIP_INTRO_OUTRO)
        fun updateSkipText() {
            skipValue.setText(if (skipEnabled) R.string.settings_on else R.string.settings_off)
        }
        updateSkipText()
        skipValue.setOnClickListener {
            skipEnabled = !skipEnabled
            prefs.edit().putBoolean(KEY_SKIP_INTRO_OUTRO, skipEnabled).apply()
            updateSkipText()
        }

        bindSwitch(rememberSwitch, KEY_REMEMBER_PROGRESS, DEFAULT_REMEMBER_PROGRESS)
        bindSwitch(hwAccelSwitch, KEY_HW_ACCEL, DEFAULT_HW_ACCEL) { checked ->
            viewModel.setDecodeMode(if (checked) DecodeMode.HARD else DecodeMode.SOFT)
        }
        bindSwitch(pauseOnExitSwitch, KEY_PAUSE_ON_EXIT, DEFAULT_PAUSE_ON_EXIT)
        bindSwitch(autoNextSwitch, KEY_AUTO_PLAY_NEXT, DEFAULT_AUTO_PLAY_NEXT)
        bindSwitch(bgAudioSwitch, KEY_BG_AUDIO, DEFAULT_BG_AUDIO)
    }

    private fun setupResetDefaults() {
        findViewById<View>(R.id.tv_reset_defaults).setOnClickListener {
            prefs.edit()
                .putFloat(KEY_SPEED, DEFAULT_SPEED)
                .putString(KEY_LOOP_MODE, DEFAULT_LOOP_MODE)
                .putInt(KEY_SEEK_INTERVAL, DEFAULT_SEEK_INTERVAL)
                .putBoolean(KEY_SKIP_INTRO_OUTRO, DEFAULT_SKIP_INTRO_OUTRO)
                .putBoolean(KEY_REMEMBER_PROGRESS, DEFAULT_REMEMBER_PROGRESS)
                .putBoolean(KEY_HW_ACCEL, DEFAULT_HW_ACCEL)
                .putBoolean(KEY_PAUSE_ON_EXIT, DEFAULT_PAUSE_ON_EXIT)
                .putBoolean(KEY_AUTO_PLAY_NEXT, DEFAULT_AUTO_PLAY_NEXT)
                .putBoolean(KEY_BG_AUDIO, DEFAULT_BG_AUDIO)
                .apply()
            setupPlaybackSection()
        }
    }

    private fun bindSwitch(
        view: SwitchMaterial,
        key: String,
        defaultValue: Boolean,
        onChanged: ((Boolean) -> Unit)? = null
    ) {
        view.setOnCheckedChangeListener(null)
        view.isChecked = prefs.getBoolean(key, defaultValue)
        view.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(key, checked).apply()
            onChanged?.invoke(checked)
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
        sectionIds.forEach { id ->
            findViewById<View>(id).visibility = if (id == selectedId) View.VISIBLE else View.GONE
        }
    }
}
