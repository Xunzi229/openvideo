package com.example.openvideo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs

@AndroidEntryPoint
class PlayerGestureSettingsSheet : BaseSettingsSheet() {
    override val layoutResId: Int = R.layout.activity_player_gesture_settings
    override fun settingsSheetDefaultFocusId(): Int = R.id.tv_left_action

    @Inject lateinit var playerPrefs: PlayerPrefs


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvLeft = view.findViewById<TextView>(R.id.tv_left_action)
        val tvRight = view.findViewById<TextView>(R.id.tv_right_action)
        val tvDoubleTap = view.findViewById<TextView>(R.id.tv_double_tap_action)
        val tvLongPress = view.findViewById<TextView>(R.id.tv_long_press_action)
        val tvHorizontal = view.findViewById<TextView>(R.id.tv_horizontal_action)
        val tvSensitivity = view.findViewById<TextView>(R.id.tv_sensitivity_value)

        val gestureActions = com.example.openvideo.core.prefs.GestureAction.entries.toTypedArray()
        val doubleTapActions = com.example.openvideo.core.prefs.DoubleTapAction.entries.toTypedArray()
        val longPressActions = com.example.openvideo.core.prefs.LongPressAction.entries.toTypedArray()

        fun updateAllTexts() {
            tvLeft.text = playerPrefs.leftVerticalGesture.key
            tvRight.text = playerPrefs.rightVerticalGesture.key
            tvDoubleTap.text = playerPrefs.doubleTapAction.key
            tvLongPress.text = playerPrefs.longPressAction.key
            tvHorizontal.text = playerPrefs.horizontalSwipeAction.key
            tvSensitivity.text = when (playerPrefs.gestureSensitivity) {
                1 -> getString(R.string.settings_sensitivity_low)
                2 -> getString(R.string.settings_sensitivity_medium)
                else -> getString(R.string.settings_sensitivity_high)
            }
        }
        updateAllTexts()

        tvLeft.setOnClickListener {
            val idx = gestureActions.indexOf(playerPrefs.leftVerticalGesture)
            playerPrefs.leftVerticalGesture = gestureActions[(idx + 1) % gestureActions.size]
            updateAllTexts()
        }

        tvRight.setOnClickListener {
            val idx = gestureActions.indexOf(playerPrefs.rightVerticalGesture)
            playerPrefs.rightVerticalGesture = gestureActions[(idx + 1) % gestureActions.size]
            updateAllTexts()
        }

        tvDoubleTap.setOnClickListener {
            val idx = doubleTapActions.indexOf(playerPrefs.doubleTapAction)
            playerPrefs.doubleTapAction = doubleTapActions[(idx + 1) % doubleTapActions.size]
            updateAllTexts()
        }

        tvLongPress.setOnClickListener {
            val idx = longPressActions.indexOf(playerPrefs.longPressAction)
            playerPrefs.longPressAction = longPressActions[(idx + 1) % longPressActions.size]
            updateAllTexts()
        }

        tvHorizontal.setOnClickListener {
            val idx = gestureActions.indexOf(playerPrefs.horizontalSwipeAction)
            playerPrefs.horizontalSwipeAction = gestureActions[(idx + 1) % gestureActions.size]
            updateAllTexts()
        }

        tvSensitivity.setOnClickListener {
            playerPrefs.gestureSensitivity = (playerPrefs.gestureSensitivity % 3) + 1
            updateAllTexts()
        }
    }
}
