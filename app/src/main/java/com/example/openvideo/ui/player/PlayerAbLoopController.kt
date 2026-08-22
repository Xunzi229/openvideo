package com.example.openvideo.ui.player

import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.openvideo.R
import com.example.openvideo.core.ui.AppleHud

class PlayerAbLoopController(
    private val activity: AppCompatActivity,
    private val buttonProvider: () -> ImageButton?,
    private val formatTime: (Long) -> String
) {
    var state = PlayerAbLoopState.IDLE
        private set
    var pointA: Long = -1
        private set
    var pointB: Long = -1
        private set

    fun toggle(currentPositionMs: Long) {
        applyResult(
            PlayerAbLoopPolicy.onToggle(
                state = state,
                pointA = pointA,
                pointB = pointB,
                currentPositionMs = currentPositionMs
            )
        )
    }

    fun reset(reset: PlayerVideoSwitchReset) {
        state = reset.abLoopState
        pointA = reset.abLoopPointA
        pointB = reset.abLoopPointB
        buttonProvider()?.clearColorFilter()
    }

    private fun applyResult(result: PlayerAbLoopResult) {
        state = result.state
        pointA = result.pointA
        pointB = result.pointB

        when (result.event) {
            PlayerAbLoopEvent.POINT_A_SET -> {
                if (PlayerAbLoopButtonStylePolicy.shouldHighlight(result.event)) {
                    buttonProvider()?.setColorFilter(ContextCompat.getColor(activity, R.color.player_accent))
                }
                AppleHud.show(activity, activity.getString(R.string.player_ab_point_a_set, formatTime(pointA)))
            }
            PlayerAbLoopEvent.LOOP_STARTED -> {
                if (PlayerAbLoopButtonStylePolicy.shouldHighlight(result.event)) {
                    buttonProvider()?.setColorFilter(ContextCompat.getColor(activity, R.color.player_accent))
                }
                AppleHud.show(activity, R.string.player_ab_loop_started)
            }
            PlayerAbLoopEvent.INVALID_POINT_B -> {
                if (PlayerAbLoopButtonStylePolicy.shouldClearHighlight(result.event)) {
                    buttonProvider()?.clearColorFilter()
                }
                AppleHud.show(activity, R.string.player_ab_point_b_error)
            }
            PlayerAbLoopEvent.CANCELLED -> {
                if (PlayerAbLoopButtonStylePolicy.shouldClearHighlight(result.event)) {
                    buttonProvider()?.clearColorFilter()
                }
                AppleHud.show(activity, R.string.player_ab_loop_cancelled)
            }
        }
    }
}
