package com.example.openvideo.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.media3.common.PlaybackException
import com.example.openvideo.R
import com.example.openvideo.core.diagnostics.CrashLogger
import com.example.openvideo.core.player.DecodeMode

class PlayerErrorHudController(
    private val activity: PlayerActivity,
    private val viewModel: PlayerViewModel,
    private val playerErrorHudProvider: () -> View,
    private val titleProvider: () -> TextView,
    private val descProvider: () -> TextView,
    private val softDecodeButtonProvider: () -> Button?,
    private val retryButtonProvider: () -> Button?,
    private val copyDiagnosticsButtonProvider: () -> Button?,
    private val backButtonProvider: () -> Button?,
    private val controlsContainerProvider: () -> View,
    private val firstFrameScrimProvider: () -> View,
    private val onShowControls: () -> Unit,
    private val onFinishPlayer: () -> Unit
) {
    fun show(error: PlaybackException) {
        val presentation = PlayerErrorPresentationPolicy.present(error.errorCode)

        titleProvider().text = activity.getString(presentation.titleRes)
        descProvider().text = activity.getString(presentation.descRes)

        val actions = presentation.actions
        softDecodeButtonProvider()?.visibility =
            if (PlayerErrorPresentationPolicy.ErrorAction.SWITCH_SOFTWARE_DECODER in actions) {
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

        softDecodeButtonProvider()?.setOnClickListener {
            viewModel.setDecodeMode(DecodeMode.SOFT)
            hide()
            viewModel.retryPlayback()
        }
        retryButtonProvider()?.setOnClickListener {
            hide()
            viewModel.retryPlayback()
        }
        copyDiagnosticsButtonProvider()?.setOnClickListener {
            val diagText = CrashLogger.readLatestPlayerErrorLog(activity)
            if (diagText.isNullOrBlank()) {
                Toast.makeText(activity, R.string.player_error_diag_unavailable, Toast.LENGTH_SHORT).show()
            } else {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OpenVideo Diagnostics", diagText))
                Toast.makeText(activity, R.string.player_error_diag_copied, Toast.LENGTH_SHORT).show()
            }
        }
        backButtonProvider()?.setOnClickListener {
            onFinishPlayer()
        }

        controlsContainerProvider().visibility = View.GONE
        firstFrameScrimProvider().visibility = View.GONE
        playerErrorHudProvider().visibility = View.VISIBLE
    }

    fun hide() {
        val playerErrorHud = playerErrorHudProvider()
        if (!playerErrorHud.isVisible) return
        playerErrorHud.visibility = View.GONE
        controlsContainerProvider().visibility = View.VISIBLE
        onShowControls()
    }
}
