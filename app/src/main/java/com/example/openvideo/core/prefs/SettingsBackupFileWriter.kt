package com.example.openvideo.core.prefs

import android.content.ContentResolver
import android.net.Uri
import java.io.OutputStream

/**
 * 将通过 [SettingsBackupExporter] 生成的 JSON 写入用户选择的 SAF 目标 URI。
 */
object SettingsBackupFileWriter {

    enum class FailureReason {
        OPEN_STREAM_FAILED,
        WRITE_FAILED
    }

    sealed class Result {
        data object Success : Result()
        data class Failure(val reason: FailureReason) : Result()
    }

    fun writeJson(contentResolver: ContentResolver, uri: Uri, json: String): Result =
        try {
            val stream = contentResolver.openOutputStream(uri)
                ?: return Result.Failure(FailureReason.OPEN_STREAM_FAILED)
            stream.use { out ->
                writeJsonToOutputStream(out, json)
            }
            Result.Success
        } catch (_: Exception) {
            Result.Failure(FailureReason.WRITE_FAILED)
        }

    internal fun writeJsonToOutputStream(stream: OutputStream, json: String) {
        stream.write(json.toByteArray(Charsets.UTF_8))
    }
}
