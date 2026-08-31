package com.example.openvideo.ui.player

import android.content.Context
import android.net.Uri
import com.example.openvideo.core.media.LocalMediaUriPolicy
import com.example.openvideo.data.model.VideoItem
import java.io.File
import java.util.LinkedHashMap
import java.util.UUID

internal object PlayerSessionQueueStore {
    private const val MAX_RETAINED_QUEUES = 8
    private const val CACHE_DIRECTORY = "player_session_queues"
    private const val CACHE_SUFFIX = ".queue"

    private val queues =
        object : LinkedHashMap<String, List<VideoItem>>(MAX_RETAINED_QUEUES, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, List<VideoItem>>?
            ): Boolean = size > MAX_RETAINED_QUEUES
        }

    fun register(context: Context, videos: List<VideoItem>): String {
        val token = UUID.randomUUID().toString()
        val snapshot = videos.toList()
        remember(token, snapshot)
        persist(context, token, snapshot)
        return token
    }

    fun resolve(context: Context, token: String?): List<VideoItem> {
        val validToken = token?.takeIf { isValidToken(it) } ?: return emptyList()
        recall(validToken)?.let { return it }
        val restored = restore(context, validToken) ?: return emptyList()
        remember(validToken, restored)
        return restored
    }

    @Synchronized
    private fun remember(token: String, videos: List<VideoItem>) {
        queues[token] = videos
    }

    @Synchronized
    private fun recall(token: String): List<VideoItem>? = queues[token]

    private fun persist(context: Context, token: String, videos: List<VideoItem>) {
        try {
            val directory = cacheDirectory(context)
            if (!directory.exists() && !directory.mkdirs()) return
            val target = cacheFile(directory, token)
            val temporary = File(directory, "$token.tmp")
            try {
                PlayerSessionQueueCodec.write(
                    videos.asSequence().map { it.toSessionQueueRecord() },
                    videos.size,
                    temporary.outputStream()
                )
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                }
                target.setLastModified(System.currentTimeMillis())
                trimDiskCache(directory)
            } finally {
                temporary.delete()
            }
        } catch (_: Exception) {
            // Memory transfer still works; PlayerActivity has a single-item fallback if restoration later fails.
        }
    }

    private fun restore(context: Context, token: String): List<VideoItem>? {
        return try {
            val file = cacheFile(cacheDirectory(context), token)
            if (!file.isFile) {
                null
            } else {
                val restored = PlayerSessionQueueCodec.read(file.inputStream()) { it.toVideoItem() }
                file.setLastModified(System.currentTimeMillis())
                restored
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun trimDiskCache(directory: File) {
        directory.listFiles { file -> file.isFile && file.name.endsWith(CACHE_SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_RETAINED_QUEUES)
            ?.forEach { it.delete() }
    }

    private fun cacheDirectory(context: Context): File =
        File(context.applicationContext.noBackupFilesDir, CACHE_DIRECTORY)

    private fun cacheFile(directory: File, token: String): File =
        File(directory, "$token$CACHE_SUFFIX")

    private fun isValidToken(token: String): Boolean =
        runCatching { UUID.fromString(token).toString() == token }.getOrDefault(false)

    private fun VideoItem.toSessionQueueRecord(): PlayerSessionQueueRecord =
        PlayerSessionQueueRecord(
            id = id,
            title = title,
            path = path,
            uri = uri.toString(),
            duration = duration,
            size = size,
            width = width,
            height = height,
            dateAdded = dateAdded,
            thumbnailUri = thumbnailUri?.toString(),
            libraryPath = libraryPath,
            dateModified = dateModified,
            orientationDegrees = orientationDegrees
        )

    private fun PlayerSessionQueueRecord.toVideoItem(): VideoItem =
        VideoItem(
            id = id,
            title = title,
            path = path,
            uri = LocalMediaUriPolicy.playbackUri(uri),
            duration = duration,
            size = size,
            width = width,
            height = height,
            dateAdded = dateAdded,
            thumbnailUri = thumbnailUri?.takeIf { it.isNotBlank() }?.let(Uri::parse),
            libraryPath = libraryPath,
            dateModified = dateModified,
            orientationDegrees = orientationDegrees
        )
}
