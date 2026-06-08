package com.example.openvideo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.R
import androidx.appcompat.app.AlertDialog
import com.example.openvideo.core.prefs.PlayerPrefs

@AndroidEntryPoint
class PlayerPlaybackSettingsSheet : BaseSettingsSheet() {
    override val layoutResId: Int = R.layout.activity_player_playback_settings
    override fun settingsSheetDefaultFocusId(): Int = when (playerPrefs.speed) {
        0.5f -> R.id.rb_speed_0_5
        0.75f -> R.id.rb_speed_0_75
        1.25f -> R.id.rb_speed_1_25
        1.5f -> R.id.rb_speed_1_5
        2.0f -> R.id.rb_speed_2_0
        else -> R.id.rb_speed_1_0
    }

    @Inject lateinit var playerPrefs: PlayerPrefs


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val speedGroup = view.findViewById<RadioGroup>(R.id.rg_speed_settings)
        val swRemember = view.findViewById<SwitchMaterial>(R.id.sw_remember)
        val swHw = view.findViewById<SwitchMaterial>(R.id.sw_hw_accel)
        val swPause = view.findViewById<SwitchMaterial>(R.id.sw_pause_on_exit)
        val swAutoNext = view.findViewById<SwitchMaterial>(R.id.sw_auto_next)
        val swBgAudio = view.findViewById<SwitchMaterial>(R.id.sw_bg_audio)
        val tvPlaybackEnd = view.findViewById<TextView>(R.id.tv_playback_end_value)

        // Speed
        val speed = playerPrefs.speed
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
        }

        swRemember.isChecked = playerPrefs.rememberProgress
        swRemember.setOnCheckedChangeListener { _, isChecked -> playerPrefs.rememberProgress = isChecked }

        swHw.isChecked = playerPrefs.hwAcceleration
        swHw.setOnCheckedChangeListener { _, isChecked -> playerPrefs.hwAcceleration = isChecked }

        swPause.isChecked = playerPrefs.pauseOnExit
        swPause.setOnCheckedChangeListener { _, isChecked -> playerPrefs.pauseOnExit = isChecked }

        swAutoNext.isChecked = playerPrefs.autoPlayNext
        swAutoNext.setOnCheckedChangeListener { _, isChecked -> playerPrefs.autoPlayNext = isChecked }

        swBgAudio.isChecked = playerPrefs.bgAudio
        swBgAudio.setOnCheckedChangeListener { _, isChecked -> playerPrefs.bgAudio = isChecked }

        fun updatePlaybackEndText() {
            tvPlaybackEnd.text = PlayerPlaybackEndBehaviorUi.label(requireContext(), playerPrefs.playbackEndBehavior)
        }
        updatePlaybackEndText()
        tvPlaybackEnd.setOnClickListener {
            val options = PlayerPlaybackEndBehaviorUi.options()
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_playback_end_behavior)
                .setItems(options.map { PlayerPlaybackEndBehaviorUi.label(requireContext(), it) }.toTypedArray()) { _, which ->
                    playerPrefs.playbackEndBehavior = options[which]
                    updatePlaybackEndText()
                }
                .show()
            dialog.listView?.post {
                dialog.listView?.requestFocus()
            }
        }
    }
}
