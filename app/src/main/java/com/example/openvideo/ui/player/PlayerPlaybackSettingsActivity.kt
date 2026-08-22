package com.example.openvideo.ui.player

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.openvideo.R
import com.example.openvideo.core.prefs.LoopMode
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.ui.AppleActionSheet
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlayerPlaybackSettingsActivity : ComponentActivity() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_playback_settings)

        val tvSpeed = findViewById<TextView>(R.id.tv_speed_value)
        val tvLoop = findViewById<TextView>(R.id.tv_loop_value)
        val tvPlaybackEnd = findViewById<TextView>(R.id.tv_playback_end_value)
        val tvSeek = findViewById<TextView>(R.id.tv_seek_value)
        val swRemember = findViewById<SwitchMaterial>(R.id.sw_remember)
        val swHw = findViewById<SwitchMaterial>(R.id.sw_hw_accel)
        val swPauseOnExit = findViewById<SwitchMaterial>(R.id.sw_pause_on_exit)
        val swAutoNext = findViewById<SwitchMaterial>(R.id.sw_auto_next)
        val swBgAudio = findViewById<SwitchMaterial>(R.id.sw_bg_audio)

        fun speedLabel(speed: Float): String = "${speed}x"
        fun updateSpeedText() {
            tvSpeed.text = speedLabel(playerPrefs.speed)
        }
        updateSpeedText()
        tvSpeed.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                title = getString(R.string.settings_playback_speed),
                items = PlayerPlaybackSpeedOptions.entries.map { it to speedLabel(it) },
                selected = playerPrefs.speed
            ) { speed ->
                playerPrefs.speed = speed
                updateSpeedText()
            }
        }
        tvSpeed.post { tvSpeed.requestFocus() }

        fun loopLabel(mode: LoopMode): String = when (mode) {
            LoopMode.SINGLE -> getString(R.string.settings_loop_single)
            LoopMode.LIST -> getString(R.string.settings_loop_list)
            LoopMode.OFF -> getString(R.string.settings_loop_off)
        }
        fun updateLoopText() {
            tvLoop.text = loopLabel(playerPrefs.loopMode)
        }
        updateLoopText()
        tvLoop.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                title = getString(R.string.settings_loop_mode),
                items = LoopMode.entries.map { it to loopLabel(it) },
                selected = playerPrefs.loopMode
            ) { mode ->
                playerPrefs.loopMode = mode
                updateLoopText()
            }
        }

        fun updatePlaybackEndText() {
            tvPlaybackEnd.text = PlayerPlaybackEndBehaviorUi.label(this, playerPrefs.playbackEndBehavior)
        }
        updatePlaybackEndText()
        tvPlaybackEnd.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                title = getString(R.string.settings_playback_end_behavior),
                items = PlayerPlaybackEndBehaviorUi.options().map {
                    it to PlayerPlaybackEndBehaviorUi.label(this, it)
                },
                selected = playerPrefs.playbackEndBehavior
            ) { behavior ->
                playerPrefs.playbackEndBehavior = behavior
                updatePlaybackEndText()
            }
        }

        val seekIntervals = listOf(5, 10, 15)
        fun updateSeekText() {
            tvSeek.text = "${playerPrefs.seekInterval}s"
        }
        updateSeekText()
        tvSeek.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                title = getString(R.string.settings_seek_interval),
                items = seekIntervals.map { it to "${it}s" },
                selected = playerPrefs.seekInterval
            ) { interval ->
                playerPrefs.seekInterval = interval
                updateSeekText()
            }
        }

        swRemember.isChecked = playerPrefs.rememberProgress
        swRemember.setOnCheckedChangeListener { _, checked -> playerPrefs.rememberProgress = checked }

        swHw.isChecked = playerPrefs.hwAcceleration
        swHw.setOnCheckedChangeListener { _, checked -> playerPrefs.hwAcceleration = checked }

        swPauseOnExit.isChecked = playerPrefs.pauseOnExit
        swPauseOnExit.setOnCheckedChangeListener { _, checked -> playerPrefs.pauseOnExit = checked }

        swAutoNext.isChecked = playerPrefs.autoPlayNext
        swAutoNext.setOnCheckedChangeListener { _, checked -> playerPrefs.autoPlayNext = checked }

        swBgAudio.isChecked = playerPrefs.bgAudio
        swBgAudio.setOnCheckedChangeListener { _, checked -> playerPrefs.bgAudio = checked }
    }
}
