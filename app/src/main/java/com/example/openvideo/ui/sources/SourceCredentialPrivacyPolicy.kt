package com.example.openvideo.ui.sources

import android.content.Context
import com.example.openvideo.R

data class SourceCredentialPrivacyNotice(
    val title: String,
    val items: List<String>
)

data class SourceCredentialPrivacyLabels(
    val title: String,
    val storage: String,
    val export: String,
    val diagnostics: String
) {
    companion object {
        fun from(context: Context): SourceCredentialPrivacyLabels {
            val resources = context.resources
            return SourceCredentialPrivacyLabels(
                title = resources.getString(R.string.source_privacy_title),
                storage = resources.getString(R.string.source_privacy_storage),
                export = resources.getString(R.string.source_privacy_export),
                diagnostics = resources.getString(R.string.source_privacy_diagnostics)
            )
        }

        fun englishDefaults(): SourceCredentialPrivacyLabels = SourceCredentialPrivacyLabels(
            title = "Credential privacy",
            storage = "Passwords and tokens are stored with Android Keystore backed encrypted storage.",
            export = "Exports include source names and addresses only; passwords, tokens, cookies, and headers are excluded.",
            diagnostics = "Diagnostics and crash reports must redact full URLs, usernames, passwords, tokens, cookies, and headers."
        )
    }
}

object SourceCredentialPrivacyPolicy {

    fun buildNotice(labels: SourceCredentialPrivacyLabels): SourceCredentialPrivacyNotice =
        SourceCredentialPrivacyNotice(
            title = labels.title,
            items = listOf(labels.storage, labels.export, labels.diagnostics)
        )

    fun formatNotice(notice: SourceCredentialPrivacyNotice): String =
        buildString {
            append(notice.title)
            notice.items.forEach { item ->
                append('\n')
                append("- ")
                append(item)
            }
        }
}
