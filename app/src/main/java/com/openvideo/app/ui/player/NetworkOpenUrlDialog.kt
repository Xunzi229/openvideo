package com.openvideo.app.ui.player

import android.content.Context
import android.text.InputType
import com.openvideo.app.R
import com.openvideo.app.core.network.NetworkRecentUrlPolicy
import com.openvideo.app.core.network.NetworkUrlPolicy
import com.openvideo.app.core.ui.AppleAction
import com.openvideo.app.core.ui.AppleActionStyle
import com.openvideo.app.core.ui.AppleAlertDialog
import com.openvideo.app.core.ui.AppleOverlayChrome
import com.openvideo.app.core.ui.AppleOverlayColors

object NetworkOpenUrlDialog {

    fun show(
        context: Context,
        onRecordRecent: (normalizedUrl: String, title: String) -> Unit
    ) {
        val colors = AppleOverlayColors.from(context)
        val input = AppleOverlayChrome.inputField(
            context = context,
            colors = colors,
            hint = context.getString(R.string.home_open_url_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        )
        var dialog: android.app.Dialog? = null
        dialog = AppleAlertDialog.show(
            context = context,
            title = context.getString(R.string.home_open_url),
            message = context.getString(R.string.home_open_url_message),
            extraContent = input,
            includeIme = true,
            focusView = input,
            actions = listOf(
                AppleAction(context.getString(R.string.action_cancel), AppleActionStyle.CANCEL),
                AppleAction(
                    title = context.getString(R.string.action_open),
                    dismissOnClick = false
                ) {
                    when (val result = NetworkUrlPolicy.validatePlaybackUrl(input.text.toString())) {
                        is NetworkUrlPolicy.Validation.Valid -> {
                            val title = NetworkRecentUrlPolicy.titleFor(result.normalizedUrl)
                            val intent = PlayerActivityIntents.networkPlayback(context, result.normalizedUrl)
                            onRecordRecent(result.normalizedUrl, title)
                            context.startActivity(intent)
                            dialog?.dismiss()
                        }
                        is NetworkUrlPolicy.Validation.Invalid -> {
                            input.error = context.getString(R.string.home_open_url_invalid)
                        }
                    }
                }
            )
        )
        input.post {
            input.requestFocus()
        }
    }
}
