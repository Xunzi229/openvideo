package com.example.openvideo.ui.player

import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.openvideo.R

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
                Toast.makeText(
                    activity,
                    activity.getString(R.string.player_ab_point_a_set, formatTime(pointA)),
                    Toast.LENGTH_SHORT
                ).show()
            }
            PlayerAbLoopEvent.LOOP_STARTED -> {
                if (PlayerAbLoopButtonStylePolicy.shouldHighlight(result.event)) {
                    buttonProvider()?.setColorFilter(ContextCompat.getColor(activity, R.color.player_accent))
                }
                Toast.makeText(activity, activity.getString(R.string.player_ab_loop_started), Toast.LENGTH_SHORT).show()
            }
            PlayerAbLoopEvent.INVALID_POINT_B -> {
                if (PlayerAbLoopButtonStylePolicy.shouldClearHighlight(result.event)) {
                    buttonProvider()?.clearColorFilter()
                }
                Toast.makeText(activity, activity.getString(R.string.player_ab_point_b_error), Toast.LENGTH_SHORT).show()
            }
            PlayerAbLoopEvent.CANCELLED -> {
                if (PlayerAbLoopButtonStylePolicy.shouldClearHighlight(result.event)) {
                    buttonProvider()?.clearColorFilter()
                }
                Toast.makeText(activity, activity.getString(R.string.player_ab_loop_cancelled), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
