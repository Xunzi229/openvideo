package com.example.openvideo.ui.player

/**
 * Pre-computed landscape-mode control geometry in pixels.
 *
 * All values are already in pixels relative to the controls container; callers should write
 * them straight into `ConstraintLayout.LayoutParams` / `LinearLayout.LayoutParams` without
 * any further scaling or clamping.
 */
data class PlayerLandscapeGeometry(
    /** Horizontal inset for the top bar, bottom panel, and right-floating column. */
    val containerHorizontalMarginPx: Int,
    /** Top margin for the top bar, proportional to container height. */
    val topBarTopMarginPx: Int,
    /** Bottom margin for the bottom panel, proportional to container height. */
    val bottomPanelBottomMarginPx: Int,
    /** Start margin for the lock button (slightly larger than the container inset). */
    val lockMarginStartPx: Int,
    /** Square side for transport icons (prev / next / land seek / fullscreen). */
    val iconSidePx: Int,
    /** Square side for the primary play/pause button. */
    val playSidePx: Int,
    /** Horizontal margin on both sides of the play/pause button (transport gap). */
    val transportGapPx: Int,
    /** Horizontal margin between prev/next icons and adjacent transport buttons. */
    val innerGapPx: Int
)

/**
 * Pure layout policy that computes [PlayerLandscapeGeometry] for the landscape player chrome.
 *
 * The previous implementation lived inline in `PlayerActivity.applyLandscapePlayerGeometry`
 * with six magic ratios and four `coerceIn(min*density, max*density)` calls. Extracting it
 * keeps the design ratios (matched to `design/横屏播放界面` mockups) in one place and lets
 * unit tests cover the small-screen min clamp and large-screen max clamp without inflating
 * any views.
 */
object PlayerLandscapeGeometryPolicy {

    // Horizontal/vertical insets relative to the controls container.
    private const val CONTAINER_HORIZONTAL_RATIO = 0.022f
    private const val TOP_BAR_TOP_RATIO = 0.028f
    private const val BOTTOM_PANEL_BOTTOM_RATIO = 0.032f
    private const val LOCK_MARGIN_START_RATIO = 0.026f

    // Width-relative transport sizing, all clamped to dp ranges for tablets and 21:9 phones.
    private const val ICON_SIDE_RATIO = 0.049f
    private const val ICON_SIDE_MIN_DP = 40f
    private const val ICON_SIDE_MAX_DP = 52f

    private const val PLAY_SIDE_RATIO = 0.060f
    private const val PLAY_SIDE_MIN_DP = 52f
    private const val PLAY_SIDE_MAX_DP = 64f

    private const val TRANSPORT_GAP_RATIO = 0.02f
    private const val TRANSPORT_GAP_MIN_DP = 14f
    private const val TRANSPORT_GAP_MAX_DP = 22f

    private const val INNER_GAP_RATIO = 0.009f
    private const val INNER_GAP_MIN_DP = 6f
    private const val INNER_GAP_MAX_DP = 12f

    /**
     * Returns the geometry for the given controls container size and display [density], or
     * `null` if the container has not been measured yet (`widthPx <= 0 || heightPx <= 0`).
     */
    fun compute(widthPx: Int, heightPx: Int, density: Float): PlayerLandscapeGeometry? {
        if (widthPx <= 0 || heightPx <= 0) return null
        val safeDensity = if (density > 0f) density else 1f
        return PlayerLandscapeGeometry(
            containerHorizontalMarginPx = (widthPx * CONTAINER_HORIZONTAL_RATIO).toInt(),
            topBarTopMarginPx = (heightPx * TOP_BAR_TOP_RATIO).toInt(),
            bottomPanelBottomMarginPx = (heightPx * BOTTOM_PANEL_BOTTOM_RATIO).toInt(),
            lockMarginStartPx = (widthPx * LOCK_MARGIN_START_RATIO).toInt(),
            iconSidePx = clampDp(
                rawPx = widthPx * ICON_SIDE_RATIO,
                minDp = ICON_SIDE_MIN_DP,
                maxDp = ICON_SIDE_MAX_DP,
                density = safeDensity
            ),
            playSidePx = clampDp(
                rawPx = widthPx * PLAY_SIDE_RATIO,
                minDp = PLAY_SIDE_MIN_DP,
                maxDp = PLAY_SIDE_MAX_DP,
                density = safeDensity
            ),
            transportGapPx = clampDp(
                rawPx = widthPx * TRANSPORT_GAP_RATIO,
                minDp = TRANSPORT_GAP_MIN_DP,
                maxDp = TRANSPORT_GAP_MAX_DP,
                density = safeDensity
            ),
            innerGapPx = clampDp(
                rawPx = widthPx * INNER_GAP_RATIO,
                minDp = INNER_GAP_MIN_DP,
                maxDp = INNER_GAP_MAX_DP,
                density = safeDensity
            )
        )
    }

    private fun clampDp(rawPx: Float, minDp: Float, maxDp: Float, density: Float): Int =
        rawPx.coerceIn(minDp * density, maxDp * density).toInt()
}
