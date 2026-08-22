package com.example.openvideo.ui.player

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode
import com.example.openvideo.core.ui.AppleActionSheet

@AndroidEntryPoint
class PlayerDisplaySettingsActivity : ComponentActivity() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    private val aspectRatios = AspectRatio.entries.toTypedArray()
    private val rotations = listOf(0, 90, 180, 270)
    private var aspectIndex = 0
    private var contentFrameIndex = 0
    private var rotationIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_display_settings)

        aspectIndex = aspectRatios.indexOf(playerPrefs.aspectRatio).takeIf { it >= 0 } ?: 0
        contentFrameIndex = PlayerDisplayContentFrameControls.modes
            .indexOf(playerPrefs.contentFrameMode)
            .takeIf { it >= 0 } ?: 0
        rotationIndex = rotations.indexOf(playerPrefs.rotation).takeIf { it >= 0 } ?: 0

        val tvAspect = findViewById<TextView>(R.id.tv_aspect_value)
        val tvContentFrame = findViewById<TextView>(R.id.tv_content_frame_value)
        val tvRotation = findViewById<TextView>(R.id.tv_rotation_value)
        val swMirror = findViewById<SwitchMaterial>(R.id.sw_mirror)
        val swAutoOrientation = findViewById<SwitchMaterial>(R.id.sw_auto_orientation)

        tvAspect.post {
            tvAspect.requestFocus()
        }

        fun aspectLabel(ratio: AspectRatio): String = getString(
            when (ratio) {
                AspectRatio.FIT -> R.string.settings_ratio_fit
                AspectRatio.FILL -> R.string.settings_ratio_fill
                AspectRatio.CROP -> R.string.settings_ratio_crop
                AspectRatio.STRETCH -> R.string.settings_ratio_stretch
                AspectRatio.RATIO_4_3 -> R.string.settings_ratio_4_3
                AspectRatio.RATIO_16_9 -> R.string.settings_ratio_16_9
            }
        )
        fun updateAspectText() {
            tvAspect.text = aspectLabel(aspectRatios[aspectIndex])
        }
        updateAspectText()
        tvAspect.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                title = getString(R.string.settings_aspect_ratio),
                items = aspectRatios.map { it to aspectLabel(it) },
                selected = aspectRatios[aspectIndex]
            ) { ratio ->
                aspectIndex = aspectRatios.indexOf(ratio).coerceAtLeast(0)
                playerPrefs.aspectRatio = ratio
                if (!PlayerContentFramePolicy.allowsContentFrameAdjustment(playerPrefs.aspectRatio) &&
                    playerPrefs.contentFrameMode != ContentFrameMode.OFF
                ) {
                    playerPrefs.contentFrameMode = ContentFrameMode.OFF
                    contentFrameIndex = 0
                    tvContentFrame.setText(PlayerDisplayContentFrameControls.labelRes(ContentFrameMode.OFF))
                }
                updateAspectText()
            }
        }

        PlayerDisplayContentFrameControls.bind(
            tvValue = tvContentFrame,
            playerPrefs = playerPrefs,
            getIndex = { contentFrameIndex },
            setIndex = { contentFrameIndex = it }
        )

        fun updateRotationText() {
            tvRotation.text = "${rotations[rotationIndex]}°"
        }
        updateRotationText()
        tvRotation.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                title = getString(R.string.settings_rotation),
                items = rotations.map { it to "${it}°" },
                selected = rotations[rotationIndex]
            ) { rotation ->
                rotationIndex = rotations.indexOf(rotation).coerceAtLeast(0)
                playerPrefs.rotation = rotation
                updateRotationText()
            }
        }

        swMirror.isChecked = playerPrefs.mirror
        swMirror.setOnCheckedChangeListener { _, checked -> playerPrefs.mirror = checked }

        swAutoOrientation.isChecked = playerPrefs.autoOrientationByVideo
        swAutoOrientation.setOnCheckedChangeListener { _, checked -> playerPrefs.autoOrientationByVideo = checked }
    }
}
