package com.example.openvideo.core.media

import android.net.Uri
import java.io.File

/**
 * 本地媒体路径 / URI 规范化：避免对「纯文件路径」使用 [Uri.parse]（路径中的 `#`、`?` 会被当成 fragment/query，
 * 触发 Media3 `FileDataSourceException: uri has query and/or fragment`），并用于判断文件是否仍可播放。
 */
object LocalMediaUriPolicy {

    fun isPlayable(pathOrUri: String): Boolean {
        val t = pathOrUri.trim()
        if (t.isEmpty()) return false
        val l = t.lowercase()
        return when {
            l.startsWith("content://") -> true
            l.startsWith("http://") || l.startsWith("https://") -> true
            l.startsWith("file://") -> {
                val path = runCatching { Uri.parse(t).path }.getOrNull() ?: return false
                File(path).isFile
            }
            else -> File(t).isFile
        }
    }

    /**
     * 供 ExoPlayer 使用的 [Uri]：`content`/`http(s)` 仍用 parse；裸文件路径或含 `#` 的文件名使用 [Uri.fromFile]。
     */
    fun playbackUri(uriOrPath: String): Uri {
        val t = uriOrPath.trim()
        require(t.isNotEmpty()) { "Empty media uri/path" }
        val l = t.lowercase()
        return when {
            l.startsWith("content://") -> Uri.parse(t)
            l.startsWith("file://") -> {
                val path = Uri.parse(t).path
                if (!path.isNullOrEmpty()) Uri.fromFile(File(path)) else Uri.parse(t)
            }
            l.startsWith("http://") || l.startsWith("https://") -> Uri.parse(t)
            else -> Uri.fromFile(File(t))
        }
    }
}
