package com.example.openvideo.ui.player

import android.content.Context
import android.text.InputType
import android.widget.EditText
import com.example.openvideo.R
import com.example.openvideo.core.network.NetworkRecentUrlPolicy
import com.example.openvideo.core.network.NetworkUrlPolicy
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object NetworkOpenUrlDialog {

    fun show(
        context: Context,
        onRecordRecent: (normalizedUrl: String, title: String) -> Unit
    ) {
        val input = EditText(context).apply {
            hint = context.getString(R.string.home_open_url_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            setPadding(48, 32, 48, 16)
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.home_open_url)
            .setMessage(R.string.home_open_url_message)
            .setView(input)
            .setPositiveButton(R.string.action_open, null)
            .setNegativeButton(R.string.action_cancel, null)
            .show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            when (val result = NetworkUrlPolicy.validatePlaybackUrl(input.text.toString())) {
                is NetworkUrlPolicy.Validation.Valid -> {
                    val title = NetworkRecentUrlPolicy.titleFor(result.normalizedUrl)
                    val intent = PlayerActivityIntents.networkPlayback(context, result.normalizedUrl)
                    onRecordRecent(result.normalizedUrl, title)
                    context.startActivity(intent)
                    dialog.dismiss()
                }
                is NetworkUrlPolicy.Validation.Invalid -> {
                    input.error = context.getString(R.string.home_open_url_invalid)
                }
            }
        }
    }
}
