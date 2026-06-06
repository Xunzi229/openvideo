package com.example.openvideo.ui.player

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.content.res.Configuration
import androidx.annotation.RequiresApi
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPicture(decision)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPictureInPicture(decision: PlayerPipDecision) {
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

    fun isInPipModeCompat(): Boolean {
        val isInPictureInPictureMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.isInPictureInPictureMode
        } else {
            false
        }
        return PlayerPipCompatPolicy.isInPictureInPictureMode(
            sdkInt = Build.VERSION.SDK_INT,
            isInPictureInPictureMode = isInPictureInPictureMode
        )
    }
}
