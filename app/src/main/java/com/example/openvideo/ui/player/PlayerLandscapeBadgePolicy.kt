package com.example.openvideo.ui.player

/**
 * 横屏顶部右侧角标显示决策的纯策略。
 *
 * 目前只判定「视频宽度是否达到 4K 分辨率」用于展示 `4K` 角标。后续如果需要 8K / HDR 等
 * 角标，统一在此扩展，保持 Activity 侧仅做 view 可见性切换。
 */
object PlayerLandscapeBadgePolicy {

    /** 4K（UHD）的宽度阈值，按主流定义 3840×2160 起。 */
    const val UHD_4K_MIN_WIDTH = 3840

    fun is4kVideo(videoWidth: Int): Boolean = videoWidth >= UHD_4K_MIN_WIDTH

    fun shouldShowBadge(videoWidth: Int): Boolean = is4kVideo(videoWidth)
}
