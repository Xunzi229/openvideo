package com.example.openvideo.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.media3.common.PlaybackException
import com.example.openvideo.R
import com.example.openvideo.core.diagnostics.CrashLogger
import com.example.openvideo.core.ui.AppleHud

class PlayerErrorHudController(
    private val activity: PlayerActivity,
    private val viewModel: PlayerViewModel,
    private val playerErrorHudProvider: () -> View,
    private val titleProvider: () -> TextView,
    private val descProvider: () -> TextView,
    private val compatibilityButtonProvider: () -> Button?,
    private val retryButtonProvider: () -> Button?,
    private val copyDiagnosticsButtonProvider: () -> Button?,
    private val backButtonProvider: () -> Button?,
    private val controlsContainerProvider: () -> View,
    private val firstFrameScrimProvider: () -> View,
    private val onShowControls: () -> Unit,
    private val onOpenCompatibilityMode: () -> Unit,
    private val onReattachPlayerAfterRetry: () -> Unit,
    private val onFinishPlayer: () -> Unit
) {
    fun show(error: PlaybackException) {
        val presentation = PlayerErrorPresentationPolicy.present(error.errorCode, error.cause)

        titleProvider().text = activity.getString(presentation.titleRes)
        descProvider().text = activity.getString(presentation.descRes)

        val actions = presentation.actions
        compatibilityButtonProvider()?.visibility =
            if (PlayerErrorPresentationPolicy.ErrorAction.OPEN_COMPATIBILITY_MODE in actions) {
                View.VISIBLE
            } else {
                View.GONE
            }
        retryButtonProvider()?.visibility =
            if (PlayerErrorPresentationPolicy.ErrorAction.RETRY in actions) View.VISIBLE else View.GONE
        copyDiagnosticsButtonProvider()?.visibility =
            if (PlayerErrorPresentationPolicy.ErrorAction.COPY_DIAGNOSTICS in actions) View.VISIBLE else View.GONE
        backButtonProvider()?.visibility =
            if (PlayerErrorPresentationPolicy.ErrorAction.GO_BACK in actions) View.VISIBLE else View.GONE

        compatibilityButtonProvider()?.setOnClickListener { onOpenCompatibilityMode() }
        retryButtonProvider()?.setOnClickListener {
            hide()
            viewModel.retryPlayback()
            onReattachPlayerAfterRetry()
        }
        copyDiagnosticsButtonProvider()?.setOnClickListener {
            val diagText = CrashLogger.readLatestPlayerErrorLog(activity)
            if (diagText.isNullOrBlank()) {
                AppleHud.show(activity, R.string.player_error_diag_unavailable)
            } else {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OpenVideo Diagnostics", diagText))
                AppleHud.show(activity, R.string.player_error_diag_copied)
            }
        }
        backButtonProvider()?.setOnClickListener {
            onFinishPlayer()
        }

        controlsContainerProvider().visibility = View.GONE
        firstFrameScrimProvider().visibility = View.GONE
        val playerErrorHud = playerErrorHudProvider()
        playerErrorHud.visibility = View.VISIBLE
        playerErrorHud.post { focusDefaultAction() }
    }

    private fun focusDefaultAction() {
        listOfNotNull(
            retryButtonProvider()?.takeIf { it.isVisible },
            compatibilityButtonProvider()?.takeIf { it.isVisible },
            copyDiagnosticsButtonProvider()?.takeIf { it.isVisible },
            backButtonProvider()?.takeIf { it.isVisible }
        ).firstOrNull()?.requestFocus()
    }

    fun hide() {
        val playerErrorHud = playerErrorHudProvider()
        if (!playerErrorHud.isVisible) return
        playerErrorHud.visibility = View.GONE
        controlsContainerProvider().visibility = View.VISIBLE
        onShowControls()
    }
}
