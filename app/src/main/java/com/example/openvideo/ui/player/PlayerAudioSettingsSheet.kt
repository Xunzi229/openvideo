package com.example.openvideo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.AudioChannel

@AndroidEntryPoint
class PlayerAudioSettingsSheet : BaseSettingsSheet() {
    override val layoutResId: Int = R.layout.activity_player_audio_settings

    @Inject lateinit var playerPrefs: PlayerPrefs


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swPreserve = view.findViewById<SwitchMaterial>(R.id.sw_preserve_pitch)
        val swBoost = view.findViewById<SwitchMaterial>(R.id.sw_volume_boost)
        val tvChannel = view.findViewById<TextView>(R.id.tv_channel_value)
        val sbDelay = view.findViewById<SeekBar>(R.id.sb_audio_delay)

        swPreserve.isChecked = playerPrefs.speedPreservePitch
        swPreserve.setOnCheckedChangeListener { _, isChecked -> playerPrefs.speedPreservePitch = isChecked }

        swBoost.isChecked = playerPrefs.volumeBoost
        swBoost.setOnCheckedChangeListener { _, isChecked -> playerPrefs.volumeBoost = isChecked }

        val channels = AudioChannel.entries.toTypedArray()
        var chIndex = channels.indexOf(playerPrefs.audioChannel).takeIf { it >= 0 } ?: 0
        fun updateChannelText() { tvChannel.text = channels[chIndex].key }
        updateChannelText()
        tvChannel.setOnClickListener {
            chIndex = (chIndex + 1) % channels.size
            playerPrefs.audioChannel = channels[chIndex]
            updateChannelText()
        }

        var pendingAudioDelay = playerPrefs.audioDelay
        sbDelay.progress = pendingAudioDelay
        sbDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) pendingAudioDelay = progress
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.audioDelay = pendingAudioDelay
            }
        })
    }
}
