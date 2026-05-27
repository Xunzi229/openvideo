package com.example.openvideo.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Handler
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.ui.PlayerView
import com.example.openvideo.R

class PlayerExitController(
    private val activity: AppCompatActivity,
    private val handler: Handler,
    private val viewModel: PlayerViewModel,
    private val playerViewProvider: () -> PlayerView?,
    private val playerRootProvider: () -> View?,
    private val firstFrameScrimProvider: () -> View?,
    private val controlsContainerProvider: () -> View?,
    private val onDismissPlaybackNotification: () -> Unit
) {
    private var exitState = PlayerExitState()

    val isFinishing: Boolean
        get() = exitState.isFinishing

    fun finishPlayer() {
        val decision = PlayerExitPolicy.requestFinish(exitState)
        val presentation = PlayerExitPolicy.finishPresentation()
        exitState = decision.nextState
        if (!decision.shouldFinish) return
        onDismissPlaybackNotification()
        preparePlayerExitFrame()
        settleOrientationBeforeExit(presentation)
        handler.postDelayed({
            releasePlayerAfterExit()
        }, presentation.releaseDelayMs)
    }

    fun preparePlayerExitFrame() {
        val playerView = playerViewProvider()
        val frame = PlayerExitPolicy.exitFrameDecision(playerView != null)
        if (!frame.shouldPrepare) return
        if (frame.cancelPlayerViewAnimation) playerView?.animate()?.cancel()
        if (frame.hidePlayerView) playerView?.visibility = View.INVISIBLE
        val backdropColorRes = when (frame.backdrop) {
            PlayerExitBackdrop.APP_BASE -> R.color.ov_bg_base
        }
        playerRootProvider()?.setBackgroundColor(ContextCompat.getColor(activity, backdropColorRes))
        firstFrameScrimProvider()?.let { firstFrameScrim ->
            firstFrameScrim.animate().cancel()
            firstFrameScrim.setBackgroundColor(ContextCompat.getColor(activity, backdropColorRes))
            firstFrameScrim.alpha = 1f
            firstFrameScrim.visibility = View.VISIBLE
            firstFrameScrim.bringToFront()
        }
        controlsContainerProvider()?.let { controlsContainer ->
            controlsContainer.animate().cancel()
            controlsContainer.visibility = View.INVISIBLE
        }
        activity.window.setBackgroundDrawableResource(backdropColorRes)
    }

    fun releasePlayerAfterExit() {
        val decision = PlayerExitPolicy.requestRelease(exitState)
        exitState = decision.nextState
        if (!decision.shouldRelease) return
        onDismissPlaybackNotification()
        viewModel.release()
    }

    private fun settleOrientationBeforeExit(presentation: PlayerExitPresentation) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        handler.postDelayed({
            preparePlayerExitFrame()
            activity.finish()
            suppressExitTransition()
        }, presentation.finishDelayMs)
    }

    private fun suppressExitTransition() {
        when {
            PlayerExitPolicy.transitionStrategyFor(Build.VERSION.SDK_INT) ==
                PlayerExitTransitionStrategy.OVERRIDE_ACTIVITY_TRANSITION &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                overrideCloseTransitionCompat()
            }
            else -> {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun overrideCloseTransitionCompat() {
        activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }
}
