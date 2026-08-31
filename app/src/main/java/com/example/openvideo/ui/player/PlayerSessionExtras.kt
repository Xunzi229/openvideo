package com.example.openvideo.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.openvideo.core.media.LocalMediaUriPolicy
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.data.model.VideoItem

/** Intent 只携带 token，避免大播放队列触发 Binder TransactionTooLargeException。 */
private const val EXTRA_SESSION_QUEUE_TOKEN = "player_session_queue_token"

fun Intent.putSessionQueue(context: Context, videos: List<VideoItem>) {
    putSessionQueueToken(PlayerSessionQueueStore.register(context, videos))
}

fun Intent.putSessionQueueToken(token: String) {
    putExtra(EXTRA_SESSION_QUEUE_TOKEN, token)
}

fun Intent.sessionQueueToken(): String? = getStringExtra(EXTRA_SESSION_QUEUE_TOKEN)

fun Intent.sessionVideoQueue(context: Context): List<VideoItem> =
    PlayerSessionQueueStore.resolve(context, sessionQueueToken())

fun HistoryEntity.toSessionVideoItem(): VideoItem {
    val uri = LocalMediaUriPolicy.playbackUri(path)
    return VideoItem(
        id = videoId,
        title = title,
        path = path,
        uri = uri,
        duration = duration,
        size = 0,
        width = 0,
        height = 0,
        dateAdded = 0,
        thumbnailUri = null
    )
}

fun PlaylistVideoEntity.toSessionVideoItem(): VideoItem {
    val uri = LocalMediaUriPolicy.playbackUri(videoPath)
    return VideoItem(
        id = videoId,
        title = videoTitle,
        path = videoPath,
        uri = uri,
        duration = videoDuration,
        size = 0,
        width = 0,
        height = 0,
        dateAdded = 0,
        thumbnailUri = null
    )
}
