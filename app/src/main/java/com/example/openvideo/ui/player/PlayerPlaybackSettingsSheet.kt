package com.example.openvideo.ui.player

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.R
import com.example.openvideo.core.prefs.LoopMode
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.ui.AppleActionSheet

@AndroidEntryPoint
class PlayerPlaybackSettingsSheet : BaseSettingsSheet() {
    override val layoutResId: Int = R.layout.activity_player_playback_settings
    override fun settingsSheetDefaultFocusId(): Int = R.id.tv_speed_value

    @Inject lateinit var playerPrefs: PlayerPrefs

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvSpeed = view.findViewById<TextView>(R.id.tv_speed_value)
        val tvLoop = view.findViewById<TextView>(R.id.tv_loop_value)
        val tvPlaybackEnd = view.findViewById<TextView>(R.id.tv_playback_end_value)
        val tvSeek = view.findViewById<TextView>(R.id.tv_seek_value)
        val swRemember = view.findViewById<SwitchMaterial>(R.id.sw_remember)
        val swHw = view.findViewById<SwitchMaterial>(R.id.sw_hw_accel)
        val swPause = view.findViewById<SwitchMaterial>(R.id.sw_pause_on_exit)
        val swAutoNext = view.findViewById<SwitchMaterial>(R.id.sw_auto_next)
        val swBgAudio = view.findViewById<SwitchMaterial>(R.id.sw_bg_audio)

        fun speedLabel(speed: Float): String = "${speed}x"
        fun updateSpeedText() {
            tvSpeed.text = speedLabel(playerPrefs.speed)
        }
        updateSpeedText()
        tvSpeed.setOnClickListener {
            AppleActionSheet.showPicker(
                context = requireContext(),
                title = getString(R.string.settings_playback_speed),
                items = PlayerPlaybackSpeedOptions.entries.map { it to speedLabel(it) },
                selected = playerPrefs.speed
            ) { speed ->
                playerPrefs.speed = speed
                updateSpeedText()
            }
        }

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
                context = requireContext(),
                title = getString(R.string.settings_loop_mode),
                items = LoopMode.entries.map { it to loopLabel(it) },
                selected = playerPrefs.loopMode
            ) { mode ->
                playerPrefs.loopMode = mode
                updateLoopText()
            }
        }

        fun updatePlaybackEndText() {
            tvPlaybackEnd.text = PlayerPlaybackEndBehaviorUi.label(requireContext(), playerPrefs.playbackEndBehavior)
        }
        updatePlaybackEndText()
        tvPlaybackEnd.setOnClickListener {
            AppleActionSheet.showPicker(
                context = requireContext(),
                title = getString(R.string.settings_playback_end_behavior),
                items = PlayerPlaybackEndBehaviorUi.options().map {
                    it to PlayerPlaybackEndBehaviorUi.label(requireContext(), it)
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
                context = requireContext(),
                title = getString(R.string.settings_seek_interval),
                items = seekIntervals.map { it to "${it}s" },
                selected = playerPrefs.seekInterval
            ) { interval ->
                playerPrefs.seekInterval = interval
                updateSeekText()
            }
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
    }
}
