package com.openvideo.app.core.player

import java.io.File

internal object MediaExportFile {
    fun write(file: File, block: () -> Unit): String {
        var completed = false
        try {
            block()
            completed = true
            return file.absolutePath
        } finally {
            if (!completed) file.delete()
        }
    }
}

internal object ClipTimestampPolicy {
    fun relativeTimeUs(sampleTimeUs: Long, actualStartUs: Long): Long {
        require(actualStartUs >= 0L && sampleTimeUs >= actualStartUs)
        return sampleTimeUs - actualStartUs
    }
}
