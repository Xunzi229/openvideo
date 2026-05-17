package com.example.openvideo.ui.player

enum class PlayerSubtitleLoadToastKind {
    NONE,
    LOADED,
    FAILED
}

data class PlayerSubtitleLoadApplyDecision(
    val shouldApplyToPlayer: Boolean,
    val toastKind: PlayerSubtitleLoadToastKind
)

/**
 * UI 层在 IO 加载完成后的应用与 Toast 决策（不含 Android 类型）。
 */
object PlayerSubtitleLoadApplyPolicy {

    fun afterLoad(loadedCount: Int, requestedToast: Boolean): PlayerSubtitleLoadApplyDecision =
        when {
            loadedCount > 0 -> PlayerSubtitleLoadApplyDecision(
                shouldApplyToPlayer = true,
                toastKind = if (requestedToast) {
                    PlayerSubtitleLoadToastKind.LOADED
                } else {
                    PlayerSubtitleLoadToastKind.NONE
                }
            )
            requestedToast -> PlayerSubtitleLoadApplyDecision(
                shouldApplyToPlayer = false,
                toastKind = PlayerSubtitleLoadToastKind.FAILED
            )
            else -> PlayerSubtitleLoadApplyDecision(
                shouldApplyToPlayer = false,
                toastKind = PlayerSubtitleLoadToastKind.NONE
            )
        }
}
