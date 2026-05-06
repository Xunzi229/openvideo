package com.example.openvideo.ui.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.openvideo.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.core.prefs.PlayerPrefs
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.TextView
import android.widget.SeekBar

@AndroidEntryPoint
class PlayerAudioSettingsActivity : ComponentActivity() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_audio_settings)

        val swPitch = findViewById<SwitchMaterial>(R.id.sw_preserve_pitch)
        val swBoost = findViewById<SwitchMaterial>(R.id.sw_volume_boost)
        val tvChannel = findViewById<TextView>(R.id.tv_channel_value)
        val tvDelay = findViewById<TextView>(R.id.tv_delay_value)

        swPitch.isChecked = playerPrefs.speedPreservePitch
        swPitch.setOnCheckedChangeListener { _, checked -> playerPrefs.speedPreservePitch = checked }

        swBoost.isChecked = playerPrefs.volumeBoost
        swBoost.setOnCheckedChangeListener { _, checked -> playerPrefs.volumeBoost = checked }

        fun updateChannel() {
            tvChannel.text = when (playerPrefs.audioChannel) {
                com.example.openvideo.core.prefs.AudioChannel.LEFT -> getString(R.string.settings_audio_left)
                com.example.openvideo.core.prefs.AudioChannel.RIGHT -> getString(R.string.settings_audio_right)
                else -> getString(R.string.settings_audio_stereo)
            }
        }
        updateChannel()
        tvChannel.setOnClickListener {
            val next = when (playerPrefs.audioChannel) {
                com.example.openvideo.core.prefs.AudioChannel.STEREO -> com.example.openvideo.core.prefs.AudioChannel.LEFT
                com.example.openvideo.core.prefs.AudioChannel.LEFT -> com.example.openvideo.core.prefs.AudioChannel.RIGHT
                else -> com.example.openvideo.core.prefs.AudioChannel.STEREO
            }
            playerPrefs.audioChannel = next
            updateChannel()
        }

        fun updateDelay() {
            tvDelay.text = "${playerPrefs.audioDelay}ms"
        }
        updateDelay()
        tvDelay.setOnClickListener {
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
}
