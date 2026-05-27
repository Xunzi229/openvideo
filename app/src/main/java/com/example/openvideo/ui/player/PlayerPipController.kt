package com.example.openvideo.ui.player

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity

class PlayerPipController(
    private val activity: AppCompatActivity,
    private val viewModel: PlayerViewModel,
    private val onApplyPipChrome: () -> Unit,
    private val onShowControls: () -> Unit
) {
    @Suppress("DEPRECATION")
    fun enterIfSupported() {
        val videoSize = viewModel.player?.videoSize
        val decision = PlayerPipPolicy.enterDecision(
            sdkInt = Build.VERSION.SDK_INT,
            supportsPictureInPicture = activity.packageManager.hasSystemFeature(
                PackageManager.FEATURE_PICTURE_IN_PICTURE
            ),
            isAlreadyInPictureInPicture = isInPipModeCompat(),
            videoWidth = videoSize?.width ?: 0,
            videoHeight = videoSize?.height ?: 0,
            pixelWidthHeightRatio = videoSize?.pixelWidthHeightRatio ?: 1f,
            unappliedRotationDegrees = videoSize?.unappliedRotationDegrees ?: 0
        )
        if (!decision.shouldEnter) return
        runCatching {
            activity.enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(
                        decision.aspectRatio?.toRational() ?: PlayerPipPolicy.fallbackRational()
                    )
                    .build()
            )
        }
    }

    fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        if (isInPictureInPictureMode) {
            onApplyPipChrome()
        } else {
            onShowControls()
        }
    }

    fun isInPipModeCompat(): Boolean =
        PlayerPipCompatPolicy.isInPictureInPictureMode(
            sdkInt = Build.VERSION.SDK_INT,
            isInPictureInPictureMode = activity.isInPictureInPictureMode
        )
}
