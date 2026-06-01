package com.example.openvideo.ui.player

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.C
import androidx.media3.common.Player
import com.example.openvideo.data.model.VideoItem
import java.io.File

object PlayerErrorDiagnostics {

    fun build(
        context: Context,
        video: VideoItem?,
        player: Player?
    ): String {
        val uri = video?.uri ?: currentMediaUri(player)
        val path = video?.path.orEmpty()
        return buildString {
            appendLine("source_media.video_id=${video?.id ?: 0L}")
            appendLine("source_media.title=${video?.title.orEmpty()}")
            appendLine("source_media.uri=${uri?.toString().orEmpty()}")
            appendLine("source_media.path=$path")
            appendLine("source_media.duration_ms=${video?.duration ?: 0L}")
            appendLine("source_media.size_bytes=${video?.size ?: 0L}")
            appendLine("source_media.width=${video?.width ?: 0}")
            appendLine("source_media.height=${video?.height ?: 0}")
            appendLine("source_media.date_added=${video?.dateAdded ?: 0L}")
            appendContentResolverMetadata(context, uri)
            appendFileMetadata(path, uri)
            appendPlayerMetadata(player)
        }
    }

    private fun StringBuilder.appendContentResolverMetadata(context: Context, uri: Uri?) {
        if (uri == null) {
            appendLine("content_resolver.mime_type=")
            appendLine("content_resolver.display_name=")
            appendLine("content_resolver.openable_size_bytes=")
            return
        }

        val resolver = context.contentResolver
        appendLine("content_resolver.mime_type=${runCatching { resolver.getType(uri) }.getOrNull().orEmpty()}")

        var displayName = ""
        var openableSize = ""
        runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex).orEmpty()

                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        openableSize = cursor.getLong(sizeIndex).toString()
                    }
                }
            }
        }
        appendLine("content_resolver.display_name=$displayName")
        appendLine("content_resolver.openable_size_bytes=$openableSize")
    }

    private fun StringBuilder.appendFileMetadata(path: String, uri: Uri?) {
        val file = fileFor(path, uri)
        if (file == null) {
            appendLine("file.exists=")
            appendLine("file.can_read=")
            appendLine("file.length_bytes=")
            appendLine("file.last_modified_ms=")
            return
        }

        appendLine("file.exists=${file.exists()}")
        appendLine("file.can_read=${file.canRead()}")
        appendLine("file.length_bytes=${runCatching { file.length() }.getOrDefault(0L)}")
        appendLine("file.last_modified_ms=${runCatching { file.lastModified() }.getOrDefault(0L)}")
    }

    private fun StringBuilder.appendPlayerMetadata(player: Player?) {
        appendLine("player.current_position_ms=${player?.currentPosition ?: 0L}")
        appendLine("player.duration_ms=${player?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L}")
        appendLine("player.buffered_position_ms=${player?.bufferedPosition ?: 0L}")
        appendLine("player.current_media_uri=${currentMediaUri(player)?.toString().orEmpty()}")
    }

    private fun fileFor(path: String, uri: Uri?): File? =
        when {
            path.isNotBlank() && !path.contains("://") -> File(path)
            uri?.scheme == ContentResolver.SCHEME_FILE -> uri.path?.let(::File)
            else -> null
        }

    private fun currentMediaUri(player: Player?): Uri? =
        player?.currentMediaItem?.localConfiguration?.uri
}
