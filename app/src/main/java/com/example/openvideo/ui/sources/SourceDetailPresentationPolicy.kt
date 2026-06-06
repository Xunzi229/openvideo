package com.example.openvideo.ui.sources

import android.content.Context
import com.example.openvideo.R
import com.example.openvideo.data.local.MediaSourceEntity

data class SourceDetailUiState(
    val typeLabel: String,
    val name: String,
    val address: String,
    val lastUsedLabel: String,
    val canTestConnection: Boolean,
    val canDelete: Boolean
)

data class SourceDetailLabels(
    val urlSource: String,
    val webDavSource: String,
    val otherSource: String,
    val neverUsed: String
) {
    companion object {
        fun from(context: Context): SourceDetailLabels {
            val resources = context.resources
            return SourceDetailLabels(
                urlSource = resources.getString(R.string.sources_recent_source_url),
                webDavSource = resources.getString(R.string.sources_webdav_title),
                otherSource = resources.getString(R.string.source_detail_type_other),
                neverUsed = resources.getString(R.string.source_detail_never_used)
            )
        }

        fun englishDefaults(): SourceDetailLabels = SourceDetailLabels(
            urlSource = "URL",
            webDavSource = "WebDAV",
            otherSource = "Source",
            neverUsed = "Never"
        )
    }
}

object SourceDetailPresentationPolicy {

    fun buildUiState(
        source: MediaSourceEntity,
        labels: SourceDetailLabels,
        formatTimestamp: (Long) -> String
    ): SourceDetailUiState {
        return SourceDetailUiState(
            typeLabel = typeLabel(source.type, labels),
            name = source.name,
            address = source.displayUrl.ifBlank { source.url },
            lastUsedLabel = if (source.lastUsedAt > 0L) formatTimestamp(source.lastUsedAt) else labels.neverUsed,
            canTestConnection = source.isEnabled,
            canDelete = true
        )
    }

    private fun typeLabel(type: String, labels: SourceDetailLabels): String =
        when (type.lowercase()) {
            "url" -> labels.urlSource
            "webdav" -> labels.webDavSource
            else -> labels.otherSource
        }
}
