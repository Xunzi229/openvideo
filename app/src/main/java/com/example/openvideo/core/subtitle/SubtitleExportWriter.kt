package com.example.openvideo.core.subtitle

import java.io.IOException
import java.io.OutputStream

object SubtitleExportWriter {

    enum class FailureReason {
        WRITE_FAILED
    }

    sealed class Result {
        data class Success(val bytesWritten: Int) : Result()
        data class Failure(val reason: FailureReason) : Result()
    }

    fun writePlanToOutputStream(
        stream: OutputStream,
        plan: SubtitleUtf8ExportPlan
    ): Result =
        try {
            stream.write(plan.bytes)
            Result.Success(bytesWritten = plan.bytes.size)
        } catch (_: IOException) {
            Result.Failure(FailureReason.WRITE_FAILED)
        }
}
