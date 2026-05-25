package com.example.openvideo.ui.player

import android.app.Activity
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.data.model.VideoItem

class PlayerVideoOrientationController(
    private val activity: Activity,
    private val playerPrefs: PlayerPrefs,
    private val userOverrodeOrientationProvider: () -> Boolean,
    private val onUserOverrodeOrientationChanged: (Boolean) -> Unit
) {

    fun apply(
        width: Int,
        height: Int,
        pixelWidthHeightRatio: Float = 1f,
        unappliedRotationDegrees: Int = 0
    ) {
        if (!PlayerVideoOrientationApplyPolicy.shouldApply(
                autoOrientationByVideo = playerPrefs.autoOrientationByVideo,
                userOverrodeOrientation = userOverrodeOrientationProvider(),
                width = width,
                height = height
            )
        ) {
            return
        }
        val targetOrientation = PlayerVideoLayoutPolicy.orientationForVideo(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
        if (activity.requestedOrientation == targetOrientation) return
        activity.requestedOrientation = targetOrientation
    }

    fun applyInitial() {
        val (width, height) = resolveInitialVideoDimensions()
        activity.requestedOrientation = PlayerOrientationPolicy.initialOrientationForVideo(
            width = width,
            height = height,
            autoOrientationByVideo = playerPrefs.autoOrientationByVideo
        )
    }

    fun preApplyForItem(item: VideoItem) {
        onUserOverrodeOrientationChanged(false)
        apply(width = item.width, height = item.height)
    }

    private fun resolveInitialVideoDimensions(): Pair<Int, Int> {
        val intent = activity.intent
        var width = intent.getIntExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, 0)
        var height = intent.getIntExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, 0)
        if (width > 0 && height > 0) return width to height

        val videoId = intent.getLongExtra("video_id", 0L)
        if (videoId != 0L) {
            intent.sessionVideoQueue().firstOrNull { it.id == videoId }?.let { item ->
                if (item.width > 0 && item.height > 0) {
                    return item.width to item.height
                }
            }
        }
        return width to height
    }
}
