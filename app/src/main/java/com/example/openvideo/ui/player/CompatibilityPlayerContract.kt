package com.example.openvideo.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

data class CompatibilityPlaybackRequest(
    val uri: Uri,
    val title: String,
    val videoId: Long,
    val videoPath: String,
    val startPositionMs: Long,
    val durationMs: Long,
    val requestHeaders: Map<String, String>,
    val speed: Float,
    val audioMuted: Boolean
)

object CompatibilityPlayerContract {
    private const val EXTRA_URI = "compatibility_uri"
    private const val EXTRA_TITLE = "compatibility_title"
    private const val EXTRA_VIDEO_ID = "compatibility_video_id"
    private const val EXTRA_VIDEO_PATH = "compatibility_video_path"
    private const val EXTRA_START_POSITION = "compatibility_start_position"
    private const val EXTRA_DURATION = "compatibility_duration"
    private const val EXTRA_REQUEST_HEADERS = "compatibility_request_headers"
    private const val EXTRA_SPEED = "compatibility_speed"
    private const val EXTRA_AUDIO_MUTED = "compatibility_audio_muted"

    fun createIntent(context: Context, request: CompatibilityPlaybackRequest): Intent =
        Intent(context, CompatibilityPlayerActivity::class.java).apply {
            putExtra(EXTRA_URI, request.uri.toString())
            putExtra(EXTRA_TITLE, request.title)
            putExtra(EXTRA_VIDEO_ID, request.videoId)
            putExtra(EXTRA_VIDEO_PATH, request.videoPath)
            putExtra(EXTRA_START_POSITION, request.startPositionMs.coerceAtLeast(0L))
            putExtra(EXTRA_DURATION, request.durationMs.coerceAtLeast(0L))
            putExtra(EXTRA_SPEED, request.speed)
            putExtra(EXTRA_AUDIO_MUTED, request.audioMuted)
            putExtra(
                EXTRA_REQUEST_HEADERS,
                Bundle().apply {
                    request.requestHeaders.forEach { (name, value) ->
                        if (name.isNotBlank() && value.isNotBlank()) putString(name.trim(), value.trim())
                    }
                }
            )
        }

    fun read(intent: Intent): CompatibilityPlaybackRequest? {
        val uri = intent.getStringExtra(EXTRA_URI)?.takeIf(String::isNotBlank)?.let(Uri::parse)
            ?: return null
        val headers = intent.getBundleExtra(EXTRA_REQUEST_HEADERS)
            ?.keySet()
            ?.associateWith { name -> intent.getBundleExtra(EXTRA_REQUEST_HEADERS)?.getString(name).orEmpty() }
            ?.filterValues(String::isNotBlank)
            .orEmpty()
        return CompatibilityPlaybackRequest(
            uri = uri,
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            videoId = intent.getLongExtra(EXTRA_VIDEO_ID, 0L),
            videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH).orEmpty(),
            startPositionMs = intent.getLongExtra(EXTRA_START_POSITION, 0L).coerceAtLeast(0L),
            durationMs = intent.getLongExtra(EXTRA_DURATION, 0L).coerceAtLeast(0L),
            requestHeaders = headers,
            speed = intent.getFloatExtra(EXTRA_SPEED, 1f).coerceIn(0.25f, 4f),
            audioMuted = intent.getBooleanExtra(EXTRA_AUDIO_MUTED, false)
        )
    }
}
