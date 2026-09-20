package com.openvideo.app.core.subtitle

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object SubtitleDocumentAccess {
    /** Retain a document grant, or keep a bounded private copy for providers without one. */
    suspend fun retain(context: Context, uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "content") {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    return@withContext uri
                } catch (_: SecurityException) {
                    // Some providers grant only temporary access.
                } catch (_: UnsupportedOperationException) {
                    // Fall back to a private copy while the temporary grant is valid.
                }
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use(SubtitleInput::readBounded)
                ?: return@withContext null
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            val directory = File(context.filesDir, "imported_subtitles").apply { mkdirs() }
            val target = File(directory, "$digest.subtitle")
            if (!target.exists()) {
                val temporary = File.createTempFile("subtitle-", ".tmp", directory)
                try {
                    temporary.writeBytes(bytes)
                    if (!temporary.renameTo(target) && !target.exists()) return@withContext null
                } finally {
                    temporary.delete()
                }
            }
            Uri.fromFile(target)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
}
