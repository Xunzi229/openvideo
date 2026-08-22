package com.example.openvideo.ui.sources

import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.example.openvideo.R
import com.example.openvideo.core.network.WebDavConnectionPolicy
import com.example.openvideo.core.ui.AppleFormSheet
import com.example.openvideo.core.ui.AppleHud
import com.example.openvideo.core.ui.AppleOverlayChrome
import com.example.openvideo.core.ui.AppleOverlayColors

object WebDavSourceDialog {

    data class Input(
        val name: String,
        val normalizedBaseUrl: String,
        val username: String,
        val password: String
    )

    fun show(context: Context, onSubmit: (Input) -> Unit) {
        val colors = AppleOverlayColors.from(context)
        val fields = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = AppleOverlayChrome.dp(context, 16)
            setPadding(pad, AppleOverlayChrome.dp(context, 8), pad, pad)
        }
        fields.addView(TextView(context).apply {
            text = context.getString(R.string.webdav_add_message)
            setTextColor(colors.message)
            textSize = AppleOverlayChrome.MESSAGE_SP
            gravity = android.view.Gravity.CENTER
            includeFontPadding = false
        })
        val nameInput = fields.addField(
            colors = colors,
            hint = context.getString(R.string.webdav_name_hint),
            inputType = InputType.TYPE_CLASS_TEXT
        )
        val baseUrlInput = fields.addField(
            colors = colors,
            hint = context.getString(R.string.webdav_base_url_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        )
        val usernameInput = fields.addField(
            colors = colors,
            hint = context.getString(R.string.webdav_username_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        )
        val passwordInput = fields.addField(
            colors = colors,
            hint = context.getString(R.string.webdav_password_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        val scroll = NestedScrollView(context)
        scroll.addView(fields, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        AppleFormSheet.show(
            context = context,
            content = scroll,
            includeIme = true,
            title = context.getString(R.string.webdav_add_title),
            cancelTitle = context.getString(R.string.action_cancel),
            confirmTitle = context.getString(R.string.webdav_action_test_save),
            defaultFocus = nameInput,
            onConfirm = {
                val baseValidation = WebDavConnectionPolicy.validateBaseUrl(baseUrlInput.text.toString())
                if (baseValidation !is WebDavConnectionPolicy.Validation.Valid) {
                    baseUrlInput.error = context.getString(R.string.webdav_base_url_invalid)
                    false
                } else {
                    val username = usernameInput.text.toString()
                    val password = passwordInput.text.toString()
                    when (WebDavConnectionPolicy.validateCredentials(username, password)) {
                        WebDavConnectionPolicy.CredentialValidation.Valid -> {
                            onSubmit(
                                Input(
                                    name = nameInput.text.toString(),
                                    normalizedBaseUrl = baseValidation.normalizedBaseUrl,
                                    username = username,
                                    password = password
                                )
                            )
                            true
                        }
                        is WebDavConnectionPolicy.CredentialValidation.Invalid -> {
                            AppleHud.show(context, R.string.webdav_credentials_invalid)
                            false
                        }
                    }
                }
            }
        )
        nameInput.post {
            nameInput.requestFocus()
        }
    }

    private fun LinearLayout.addField(
        colors: AppleOverlayColors,
        hint: String,
        inputType: Int
    ): EditText {
        val field = AppleOverlayChrome.inputField(context, colors, hint, inputType)
        addView(
            field,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = AppleOverlayChrome.dp(context, 10)
            }
        )
        return field
    }
}
