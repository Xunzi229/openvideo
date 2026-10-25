package com.openvideo.app.ui.player

import android.app.Activity
import com.openvideo.app.core.prefs.AppPrefs
import com.openvideo.app.core.prefs.PlayerPrefs
import com.openvideo.app.data.model.VideoItem

class PlayerVideoOrientationController(
    private val activity: Activity,
    private val playerPrefs: PlayerPrefs,
    private val appPrefs: AppPrefs,
    private val userOverrodeOrientationProvider: () -> Boolean,
    private val onUserOverrodeOrientationChanged: (Boolean) -> Unit
) {

    fun apply(
        width: Int,
        height: Int,
        pixelWidthHeightRatio: Float = 1f,
        unappliedRotationDegrees: Int = 0
    ) {
        // Keep the preferred entry orientation after late video-size callbacks, but
        // let the fullscreen button override it for the current video.
        val preferredOrientation = PlayerOrientationPolicy.preferredOrientation(appPrefs.playbackOrientation)
        if (preferredOrientation != null) {
            if (!userOverrodeOrientationProvider() && activity.requestedOrientation != preferredOrientation) {
                activity.requestedOrientation = preferredOrientation
            }
            return
        }
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
        val preferredOrientation = PlayerOrientationPolicy.preferredOrientation(appPrefs.playbackOrientation)
        if (preferredOrientation != null) {
            activity.requestedOrientation = preferredOrientation
            return
        }
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
            intent.sessionVideoQueue(activity).firstOrNull { it.id == videoId }?.let { item ->
                if (item.width > 0 && item.height > 0) {
                    return item.width to item.height
                }
            }
        }
        return width to height
    }
}
