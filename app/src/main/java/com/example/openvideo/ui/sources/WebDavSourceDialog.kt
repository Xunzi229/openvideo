package com.example.openvideo.ui.sources

import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.example.openvideo.R
import com.example.openvideo.core.network.WebDavConnectionPolicy
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object WebDavSourceDialog {

    data class Input(
        val name: String,
        val normalizedBaseUrl: String,
        val username: String,
        val password: String
    )

    fun show(context: Context, onSubmit: (Input) -> Unit) {
        val density = context.resources.displayMetrics.density
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(density, 24), dp(density, 8), dp(density, 24), 0)
        }
        val nameInput = content.addField(
            hint = context.getString(R.string.webdav_name_hint),
            inputType = InputType.TYPE_CLASS_TEXT
        )
        val baseUrlInput = content.addField(
            hint = context.getString(R.string.webdav_base_url_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        )
        val usernameInput = content.addField(
            hint = context.getString(R.string.webdav_username_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        )
        val passwordInput = content.addField(
            hint = context.getString(R.string.webdav_password_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.webdav_add_title)
            .setMessage(R.string.webdav_add_message)
            .setView(content)
            .setPositiveButton(R.string.webdav_action_test_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .show()
        nameInput.post {
            nameInput.requestFocus()
        }

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val baseValidation = WebDavConnectionPolicy.validateBaseUrl(baseUrlInput.text.toString())
            if (baseValidation !is WebDavConnectionPolicy.Validation.Valid) {
                baseUrlInput.error = context.getString(R.string.webdav_base_url_invalid)
                return@setOnClickListener
            }
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            when (WebDavConnectionPolicy.validateCredentials(username, password)) {
                WebDavConnectionPolicy.CredentialValidation.Valid -> Unit
                is WebDavConnectionPolicy.CredentialValidation.Invalid -> {
                    Toast.makeText(context, R.string.webdav_credentials_invalid, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            dialog.dismiss()
            onSubmit(
                Input(
                    name = nameInput.text.toString(),
                    normalizedBaseUrl = baseValidation.normalizedBaseUrl,
                    username = username,
                    password = password
                )
            )
        }
    }

    private fun LinearLayout.addField(hint: String, inputType: Int): EditText {
        val field = EditText(context).apply {
            this.hint = hint
            this.inputType = inputType
            setSingleLine(true)
        }
        addView(
            field,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return field
    }

    private fun dp(density: Float, value: Int): Int = (value * density + 0.5f).toInt()
}
