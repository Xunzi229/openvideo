package com.example.openvideo.core.diagnostics

import android.content.Context
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal object CrashReportOutbox {
    private const val DIR_NAME = "crash_report_outbox"
    private const val MAX_PENDING_REPORTS = 20
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 5_000
    private val flushing = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "openvideo-crash-outbox")
    }

    fun enqueue(context: Context, fileName: String, payload: String) {
        runCatching {
            val dir = outboxDir(context).apply { mkdirs() }
            File(dir, safeFileName(fileName)).writeText(payload)
            trimQueue(dir)
        }
    }

    fun flushAsync(context: Context, webhookUrl: String) {
        if (webhookUrl.isBlank() || !flushing.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        executor.execute {
            try {
                for (file in pendingFiles(appContext)) {
                    if (send(webhookUrl, file.readText())) {
                        file.delete()
                    } else {
                        break
                    }
                }
            } finally {
                flushing.set(false)
            }
        }
    }

    fun clear(context: Context) {
        runCatching { pendingFiles(context).forEach { it.delete() } }
    }

    private fun send(webhookUrl: String, payload: String): Boolean = runCatching {
        val connection = (URL(webhookUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            connection.outputStream.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer -> writer.write(payload) }
            }
            connection.responseCode in 200..299
        } finally {
            runCatching { connection.inputStream.close() }
            runCatching { connection.errorStream?.close() }
            connection.disconnect()
        }
    }.getOrDefault(false)

    private fun pendingFiles(context: Context): List<File> =
        outboxDir(context).listFiles()?.sortedBy(File::lastModified).orEmpty()

    private fun outboxDir(context: Context) = File(context.applicationContext.filesDir, DIR_NAME)

    private fun trimQueue(dir: File) {
        dir.listFiles()?.sortedByDescending(File::lastModified)
            ?.drop(MAX_PENDING_REPORTS)
            ?.forEach { it.delete() }
    }

    private fun safeFileName(fileName: String): String =
        fileName.replace(Regex("[^A-Za-z0-9_.-]"), "_")
}
