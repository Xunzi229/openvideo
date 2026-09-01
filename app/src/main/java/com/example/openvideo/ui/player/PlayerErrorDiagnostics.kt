package com.example.openvideo.ui.player

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.C
import androidx.media3.common.Player
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.core.diagnostics.CrashRedactionPolicy
import java.io.File

object PlayerErrorDiagnostics {

    data class Snapshot(
        val videoId: Long,
        val title: String,
        val uri: Uri?,
        val path: String,
        val durationMs: Long,
        val sizeBytes: Long,
        val width: Int,
        val height: Int,
        val dateAdded: Long,
        val playerCurrentPositionMs: Long,
        val playerDurationMs: Long,
        val playerBufferedPositionMs: Long,
        val playerCurrentMediaUri: Uri?
    )

    /** Capture Player state on its application thread before any diagnostic IO is dispatched. */
    fun capture(
        video: VideoItem?,
        player: Player?
    ): Snapshot {
        val uri = video?.uri ?: currentMediaUri(player)
        return Snapshot(
            videoId = video?.id ?: 0L,
            title = video?.title.orEmpty(),
            uri = uri,
            path = video?.path.orEmpty(),
            durationMs = video?.duration ?: 0L,
            sizeBytes = video?.size ?: 0L,
            width = video?.width ?: 0,
            height = video?.height ?: 0,
            dateAdded = video?.dateAdded ?: 0L,
            playerCurrentPositionMs = player?.currentPosition ?: 0L,
            playerDurationMs = player?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L,
            playerBufferedPositionMs = player?.bufferedPosition ?: 0L,
            playerCurrentMediaUri = currentMediaUri(player)
        )
    }

    /** Resolver and filesystem metadata must be built off the main thread. */
    fun build(context: Context, snapshot: Snapshot): String {
        return buildString {
            appendLine("source_media.video_id=${snapshot.videoId}")
            appendLine("source_media.title=${snapshot.title}")
            appendLine("source_media.uri=${redactedUri(snapshot.uri)}")
            appendLine("source_media.path=${snapshot.path}")
            appendLine("source_media.duration_ms=${snapshot.durationMs}")
            appendLine("source_media.size_bytes=${snapshot.sizeBytes}")
            appendLine("source_media.width=${snapshot.width}")
            appendLine("source_media.height=${snapshot.height}")
            appendLine("source_media.date_added=${snapshot.dateAdded}")
            appendContentResolverMetadata(context, snapshot.uri)
            appendFileMetadata(snapshot.path, snapshot.uri)
            appendPlayerMetadata(snapshot)
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

    private fun StringBuilder.appendPlayerMetadata(snapshot: Snapshot) {
        appendLine("player.current_position_ms=${snapshot.playerCurrentPositionMs}")
        appendLine("player.duration_ms=${snapshot.playerDurationMs}")
        appendLine("player.buffered_position_ms=${snapshot.playerBufferedPositionMs}")
        appendLine("player.current_media_uri=${redactedUri(snapshot.playerCurrentMediaUri)}")
    }

    private fun fileFor(path: String, uri: Uri?): File? =
        when {
            path.isNotBlank() && !path.contains("://") -> File(path)
            uri?.scheme == ContentResolver.SCHEME_FILE -> uri.path?.let(::File)
            else -> null
        }

    private fun currentMediaUri(player: Player?): Uri? =
        player?.currentMediaItem?.localConfiguration?.uri

    private fun redactedUri(uri: Uri?): String =
        uri?.toString()?.let(CrashRedactionPolicy::redactUri).orEmpty()
}
