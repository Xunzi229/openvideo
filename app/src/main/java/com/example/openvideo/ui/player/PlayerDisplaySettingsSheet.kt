package com.example.openvideo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.AspectRatio

@AndroidEntryPoint
class PlayerDisplaySettingsSheet : BaseSettingsSheet() {
    override val layoutResId: Int = R.layout.activity_player_display_settings

    @Inject lateinit var playerPrefs: PlayerPrefs

    private val aspectRatios = AspectRatio.entries.toTypedArray()
    private val rotations = intArrayOf(0, 90, 180, 270)
    private var aspectIndex = 0
    private var rotationIndex = 0


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aspectIndex = aspectRatios.indexOf(playerPrefs.aspectRatio).takeIf { it >= 0 } ?: 0
        rotationIndex = rotations.indexOf(playerPrefs.rotation).takeIf { it >= 0 } ?: 0

        val tvAspect = view.findViewById<TextView>(R.id.tv_aspect_value)
        val tvRotation = view.findViewById<TextView>(R.id.tv_rotation_value)
        val swMirror = view.findViewById<SwitchMaterial>(R.id.sw_mirror)
        val swAutoOrientation = view.findViewById<SwitchMaterial>(R.id.sw_auto_orientation)

        fun updateAspectText() {
            val textRes = when (aspectRatios[aspectIndex]) {
                AspectRatio.FIT -> R.string.settings_ratio_fit
                AspectRatio.FILL -> R.string.settings_ratio_fill
                AspectRatio.CROP -> R.string.settings_ratio_crop
                AspectRatio.STRETCH -> R.string.settings_ratio_stretch
                AspectRatio.RATIO_4_3 -> R.string.settings_ratio_4_3
                AspectRatio.RATIO_16_9 -> R.string.settings_ratio_16_9
            }
            tvAspect.setText(textRes)
        }
        updateAspectText()

        tvAspect.setOnClickListener {
            aspectIndex = (aspectIndex + 1) % aspectRatios.size
            playerPrefs.aspectRatio = aspectRatios[aspectIndex]
            updateAspectText()
        }

        fun updateRotationText() {
            tvRotation.text = "${rotations[rotationIndex]}°"
        }
        updateRotationText()
        tvRotation.setOnClickListener {
            rotationIndex = (rotationIndex + 1) % rotations.size
            playerPrefs.rotation = rotations[rotationIndex]
            updateRotationText()
        }

        swMirror.isChecked = playerPrefs.mirror
        swMirror.setOnCheckedChangeListener { _, isChecked -> playerPrefs.mirror = isChecked }

        swAutoOrientation.isChecked = playerPrefs.autoOrientationByVideo
        swAutoOrientation.setOnCheckedChangeListener { _, isChecked -> playerPrefs.autoOrientationByVideo = isChecked }
    }
}
