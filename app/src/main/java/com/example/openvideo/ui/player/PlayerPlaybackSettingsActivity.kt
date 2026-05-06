package com.example.openvideo.ui.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.openvideo.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.core.prefs.PlayerPrefs
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.TextView
import android.widget.RadioGroup

@AndroidEntryPoint
class PlayerPlaybackSettingsActivity : ComponentActivity() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_playback_settings)

        val rgSpeed = findViewById<RadioGroup>(R.id.rg_speed_settings)
        val tvLoop = findViewById<TextView>(R.id.tv_loop_value)
        val tvSeek = findViewById<TextView>(R.id.tv_seek_value)
        val swRemember = findViewById<SwitchMaterial>(R.id.sw_remember)
        val swHw = findViewById<SwitchMaterial>(R.id.sw_hw_accel)
        val swPauseOnExit = findViewById<SwitchMaterial>(R.id.sw_pause_on_exit)
        val swAutoNext = findViewById<SwitchMaterial>(R.id.sw_auto_next)
        val swBgAudio = findViewById<SwitchMaterial>(R.id.sw_bg_audio)

        // Speed
        rgSpeed.check(
            when (playerPrefs.speed) {
                0.5f -> R.id.rb_speed_0_5
                0.75f -> R.id.rb_speed_0_75
                1.25f -> R.id.rb_speed_1_25
                1.5f -> R.id.rb_speed_1_5
                2.0f -> R.id.rb_speed_2_0
                else -> R.id.rb_speed_1_0
            }
        )
        rgSpeed.setOnCheckedChangeListener { _, checkedId ->
            val speed = when (checkedId) {
                R.id.rb_speed_0_5 -> 0.5f
                R.id.rb_speed_0_75 -> 0.75f
                R.id.rb_speed_1_25 -> 1.25f
                R.id.rb_speed_1_5 -> 1.5f
                R.id.rb_speed_2_0 -> 2.0f
                else -> 1.0f
            }
            playerPrefs.speed = speed
        }

        // Loop mode
        fun updateLoopText() {
            tvLoop.text = when (playerPrefs.loopMode) {
                com.example.openvideo.core.prefs.LoopMode.SINGLE -> getString(R.string.settings_loop_single)
                com.example.openvideo.core.prefs.LoopMode.LIST -> getString(R.string.settings_loop_list)
                else -> getString(R.string.settings_loop_off)
            }
        }
        updateLoopText()
        tvLoop.setOnClickListener {
            playerPrefs.loopMode = when (playerPrefs.loopMode) {
                com.example.openvideo.core.prefs.LoopMode.OFF -> com.example.openvideo.core.prefs.LoopMode.LIST
                com.example.openvideo.core.prefs.LoopMode.LIST -> com.example.openvideo.core.prefs.LoopMode.SINGLE
                else -> com.example.openvideo.core.prefs.LoopMode.OFF
            }
            updateLoopText()
        }

        // Seek interval
        val seekIntervals = intArrayOf(5, 10, 15)
        var seekIndex = seekIntervals.indexOf(playerPrefs.seekInterval).takeIf { it >= 0 } ?: 1
        fun updateSeekText() {
            tvSeek.text = "${seekIntervals[seekIndex]}s"
        }
        updateSeekText()
        tvSeek.setOnClickListener {
            seekIndex = (seekIndex + 1) % seekIntervals.size
            playerPrefs.seekInterval = seekIntervals[seekIndex]
            updateSeekText()
        }

        // Switches
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
