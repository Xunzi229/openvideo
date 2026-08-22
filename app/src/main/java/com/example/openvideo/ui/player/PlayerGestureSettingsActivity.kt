package com.example.openvideo.ui.player

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.openvideo.R
import com.example.openvideo.core.prefs.DoubleTapAction
import com.example.openvideo.core.prefs.GestureAction
import com.example.openvideo.core.prefs.LongPressAction
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.ui.AppleActionSheet
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlayerGestureSettingsActivity : ComponentActivity() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_gesture_settings)

        val tvLeft = findViewById<TextView>(R.id.tv_left_action)
        val tvRight = findViewById<TextView>(R.id.tv_right_action)
        val tvDoubleTap = findViewById<TextView>(R.id.tv_double_tap_action)
        val tvLongPress = findViewById<TextView>(R.id.tv_long_press_action)
        val tvHorizontal = findViewById<TextView>(R.id.tv_horizontal_action)
        val tvSensitivity = findViewById<TextView>(R.id.tv_sensitivity_value)

        tvLeft.post {
            tvLeft.requestFocus()
        }

        fun sensitivityLabel(level: Int): String = when (level) {
            1 -> getString(R.string.settings_sensitivity_low)
            2 -> getString(R.string.settings_sensitivity_medium)
            else -> getString(R.string.settings_sensitivity_high)
        }
        fun updateAllTexts() {
            tvLeft.text = playerPrefs.leftVerticalGesture.key
            tvRight.text = playerPrefs.rightVerticalGesture.key
            tvDoubleTap.text = playerPrefs.doubleTapAction.key
            tvLongPress.text = playerPrefs.longPressAction.key
            tvHorizontal.text = playerPrefs.horizontalSwipeAction.key
            tvSensitivity.text = sensitivityLabel(playerPrefs.gestureSensitivity)
        }
        updateAllTexts()

        tvLeft.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                items = GestureAction.entries.map { it to it.key },
                selected = playerPrefs.leftVerticalGesture
            ) { action ->
                playerPrefs.leftVerticalGesture = action
                updateAllTexts()
            }
        }
        tvRight.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                items = GestureAction.entries.map { it to it.key },
                selected = playerPrefs.rightVerticalGesture
            ) { action ->
                playerPrefs.rightVerticalGesture = action
                updateAllTexts()
            }
        }
        tvDoubleTap.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                items = DoubleTapAction.entries.map { it to it.key },
                selected = playerPrefs.doubleTapAction
            ) { action ->
                playerPrefs.doubleTapAction = action
                updateAllTexts()
            }
        }
        tvLongPress.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                items = LongPressAction.entries.map { it to it.key },
                selected = playerPrefs.longPressAction
            ) { action ->
                playerPrefs.longPressAction = action
                updateAllTexts()
            }
        }
        tvHorizontal.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                items = GestureAction.entries.map { it to it.key },
                selected = playerPrefs.horizontalSwipeAction
            ) { action ->
                playerPrefs.horizontalSwipeAction = action
                updateAllTexts()
            }
        }
        tvSensitivity.setOnClickListener {
            AppleActionSheet.showPicker(
                context = this,
                title = getString(R.string.settings_gesture_sensitivity),
                items = listOf(1, 2, 3).map { it to sensitivityLabel(it) },
                selected = playerPrefs.gestureSensitivity
            ) { level ->
                playerPrefs.gestureSensitivity = level
                updateAllTexts()
            }
        }
    }
}
