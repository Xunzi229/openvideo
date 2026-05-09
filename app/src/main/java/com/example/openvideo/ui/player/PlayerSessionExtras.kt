package com.example.openvideo.ui.player

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.data.model.VideoItem

/** 播放页会话队列在 Intent 中的 key（同列表 / 同目录）。 */
const val EXTRA_SESSION_QUEUE = "player_session_queue"

fun VideoItem.toSessionBundle(): Bundle =
    Bundle().apply {
        putLong("id", id)
        putString("title", title)
        putString("path", path)
        putString("uri", uri.toString())
        putLong("duration", duration)
        putLong("size", size)
        putInt("width", width)
        putInt("height", height)
        putLong("dateAdded", dateAdded)
        putString("thumb", thumbnailUri?.toString())
    }

fun Bundle.toVideoItemOrNull(): VideoItem? {
    val uriStr = getString("uri") ?: return null
    return try {
        VideoItem(
            id = getLong("id"),
            title = getString("title").orEmpty(),
            path = getString("path").orEmpty(),
            uri = Uri.parse(uriStr),
            duration = getLong("duration"),
            size = getLong("size"),
            width = getInt("width"),
            height = getInt("height"),
            dateAdded = getLong("dateAdded"),
            thumbnailUri = getString("thumb")?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        )
    } catch (_: Exception) {
        null
    }
}

fun Intent.putSessionQueue(videos: List<VideoItem>) {
    putParcelableArrayListExtra(
        EXTRA_SESSION_QUEUE,
        ArrayList(videos.map { it.toSessionBundle() })
    )
}

fun Intent.sessionVideoQueue(): List<VideoItem> {
    val bundles =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(EXTRA_SESSION_QUEUE, Bundle::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra(EXTRA_SESSION_QUEUE)
        } ?: return emptyList()
    return bundles.mapNotNull { it.toVideoItemOrNull() }
}

fun HistoryEntity.toSessionVideoItem(): VideoItem {
    val uri = Uri.parse(path)
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
    val uri = Uri.parse(videoPath)
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
