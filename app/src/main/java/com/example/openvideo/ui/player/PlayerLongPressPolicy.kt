package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.LongPressAction

data class PlayerLongPressStartDecision(
    val startLongPress: Boolean,
    val targetSpeed: Float?,
    val restoreSpeed: Float
)

data class PlayerLongPressReleaseDecision(
    val shouldRestoreSpeed: Boolean,
    val restoreSpeed: Float?
)

object PlayerLongPressPolicy {
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 5.0f
    const val NORMAL_SPEED = 1.0f
    const val DEFAULT_LONG_PRESS_SPEED = 2.0f

    fun onPress(
        action: LongPressAction,
        requestedSpeed: Float,
        restoreSpeed: Float
    ): PlayerLongPressStartDecision {
        if (action != LongPressAction.SPEED) {
            return PlayerLongPressStartDecision(
                startLongPress = false,
                targetSpeed = null,
                restoreSpeed = restoreSpeed
            )
        }
        return PlayerLongPressStartDecision(
            startLongPress = true,
            targetSpeed = safeSpeed(requestedSpeed),
            restoreSpeed = safeRestoreSpeed(restoreSpeed)
        )
    }

    fun onRelease(isLongPressing: Boolean, restoreSpeed: Float): PlayerLongPressReleaseDecision {
        if (!isLongPressing) {
            return PlayerLongPressReleaseDecision(shouldRestoreSpeed = false, restoreSpeed = null)
        }
        return PlayerLongPressReleaseDecision(
            shouldRestoreSpeed = true,
            restoreSpeed = safeRestoreSpeed(restoreSpeed)
        )
    }

    private fun safeSpeed(speed: Float): Float {
        return if (speed.isFinite()) {
            speed.coerceIn(MIN_SPEED, MAX_SPEED)
        } else {
            DEFAULT_LONG_PRESS_SPEED
        }
    }

    private fun safeRestoreSpeed(speed: Float): Float {
        return if (speed.isFinite()) {
            speed.coerceIn(MIN_SPEED, MAX_SPEED)
        } else {
            NORMAL_SPEED
        }
    }
}
