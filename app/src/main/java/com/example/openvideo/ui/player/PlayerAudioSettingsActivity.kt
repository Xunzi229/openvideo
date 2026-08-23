package com.example.openvideo.ui.player

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.openvideo.R
import com.example.openvideo.core.prefs.AudioChannel
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionSheet
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlayerAudioSettingsActivity : ComponentActivity() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_audio_settings)
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tv_back_title).setText(R.string.settings_page_title)

        val swPitch = findViewById<SwitchMaterial>(R.id.sw_preserve_pitch)
        val swBoost = findViewById<SwitchMaterial>(R.id.sw_volume_boost)
        val tvChannel = findViewById<TextView>(R.id.tv_channel_value)
        val tvDelay = findViewById<TextView>(R.id.tv_delay_value)

        swPitch.post {
            swPitch.requestFocus()
        }

        swPitch.isChecked = playerPrefs.speedPreservePitch
        swPitch.setOnCheckedChangeListener { _, checked -> playerPrefs.speedPreservePitch = checked }

        swBoost.isChecked = playerPrefs.volumeBoost
        swBoost.setOnCheckedChangeListener { _, checked -> playerPrefs.volumeBoost = checked }

        fun channelLabel(channel: AudioChannel): String = when (channel) {
            AudioChannel.LEFT -> getString(R.string.settings_audio_left)
            AudioChannel.RIGHT -> getString(R.string.settings_audio_right)
            AudioChannel.STEREO -> getString(R.string.settings_audio_stereo)
        }
        fun updateChannel() {
            tvChannel.text = channelLabel(playerPrefs.audioChannel)
        }
        updateChannel()
        tvChannel.setOnClickListener {
            AppleActionSheet.show(
                context = this,
                title = getString(R.string.settings_audio_channel),
                actions = AudioChannel.entries.map { channel ->
                    AppleAction(
                        title = channelLabel(channel),
                        selected = channel == playerPrefs.audioChannel,
                        onClick = {
                            playerPrefs.audioChannel = channel
                            updateChannel()
                        }
                    )
                },
                defaultFocusCancel = false
            )
        }

        val delayOptions = listOf(-500, -250, 0, 250, 500)
        fun updateDelay() {
            tvDelay.text = "${playerPrefs.audioDelay}ms"
        }
        updateDelay()
        tvDelay.setOnClickListener {
            AppleActionSheet.show(
                context = this,
                title = getString(R.string.settings_audio_delay),
                actions = delayOptions.map { delay ->
                    AppleAction(
                        title = "${delay}ms",
                        selected = delay == playerPrefs.audioDelay,
                        onClick = {
                            playerPrefs.audioDelay = delay
                            updateDelay()
                        }
                    )
                },
                defaultFocusCancel = false
            )
        }
    }
}
