package com.example.openvideo.ui.player

/**
 * Chrome regions whose visibility is driven together when showing/hiding player controls.
 * Mirrors the view groups updated in [com.example.openvideo.ui.player.PlayerActivity.applyControlVisibility].
 */
enum class PlayerChromeRegion {
    TOP_SCRIM,
    BOTTOM_SCRIM,
    TOP_BAR,
    BOTTOM_PANEL,
    TOOL_ROW,
    LAND_RIGHT_FLOAT_COLUMN
}

/**
 * User actions that must be blocked while the screen is locked, except for lock/unlock and
 * briefly revealing the lock affordance.
 */
enum class PlayerLockedInteraction {
    LOCK_TOGGLE,
    REVEAL_LOCKED_CHROME,
    CHROME_TOGGLE,
    TRANSPORT,
    SETTINGS,
    SEEK_BAR,
    BACK,
    GESTURE_PLAYBACK
}

/**
 * Pure policy for which chrome is visible and which interactions are allowed in lock mode.
 */
object PlayerLockedControlsPolicy {

    fun visibility(isLocked: Boolean, controlsVisible: Boolean): ControlVisibility =
        ControlVisibility(
            chromeVisible = controlsVisible && !isLocked,
            lockButtonVisible = controlsVisible || isLocked,
            lockButtonSelected = isLocked
        )

    fun isChromeRegionVisible(region: PlayerChromeRegion, isLocked: Boolean, controlsVisible: Boolean): Boolean {
        val chromeVisible = visibility(isLocked, controlsVisible).chromeVisible
        return when (region) {
            PlayerChromeRegion.TOP_SCRIM,
            PlayerChromeRegion.BOTTOM_SCRIM,
            PlayerChromeRegion.TOP_BAR,
            PlayerChromeRegion.BOTTOM_PANEL,
            PlayerChromeRegion.TOOL_ROW,
            PlayerChromeRegion.LAND_RIGHT_FLOAT_COLUMN -> chromeVisible
        }
    }

    fun allows(interaction: PlayerLockedInteraction, isLocked: Boolean): Boolean = when {
        !isLocked -> true
        interaction == PlayerLockedInteraction.LOCK_TOGGLE -> true
        interaction == PlayerLockedInteraction.REVEAL_LOCKED_CHROME -> true
        else -> false
    }
}
