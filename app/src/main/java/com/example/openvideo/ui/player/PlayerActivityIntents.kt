package com.example.openvideo.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.openvideo.core.network.NetworkRecentUrlPolicy

object PlayerActivityIntents {
    const val EXTRA_REQUEST_HEADERS = "request_headers"
    const val EXTRA_EXTERNAL_SUBTITLE_URI = "external_subtitle_uri"

    fun networkPlayback(
        context: Context,
        normalizedUrl: String,
        requestHeaders: Map<String, String> = emptyMap(),
        externalSubtitleUri: String? = null
    ): Intent {
        val title = NetworkRecentUrlPolicy.titleFor(normalizedUrl)
        return Intent(context, PlayerActivity::class.java).apply {
            putExtra("video_uri", normalizedUrl)
            putExtra("video_title", title)
            putExtra("video_id", normalizedUrl.hashCode().toLong())
            putExtra("video_path", normalizedUrl)
            if (!externalSubtitleUri.isNullOrBlank()) {
                putExtra(EXTRA_EXTERNAL_SUBTITLE_URI, externalSubtitleUri.trim())
            }
            sanitizedHeaders(requestHeaders).takeIf { it.isNotEmpty() }?.let { headers ->
                putExtra(
                    EXTRA_REQUEST_HEADERS,
                    Bundle().apply {
                        headers.forEach { (name, value) ->
                            putString(name.trim(), value.trim())
                        }
                    }
                )
            }
        }
    }

    fun requestHeaders(intent: Intent): Map<String, String> {
        val bundle = intent.getBundleExtra(EXTRA_REQUEST_HEADERS) ?: return emptyMap()
        return bundle.keySet()
            .associateWith { key -> bundle.getString(key)?.trim().orEmpty() }
            .filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
            .mapKeys { (key, _) -> key.trim() }
    }

    fun externalSubtitleUri(intent: Intent): String =
        intent.getStringExtra(EXTRA_EXTERNAL_SUBTITLE_URI)?.trim().orEmpty()

    private fun sanitizedHeaders(headers: Map<String, String>): Map<String, String> =
        headers
            .mapKeys { (key, _) -> key.trim() }
            .mapValues { (_, value) -> value.trim() }
            .filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
}
