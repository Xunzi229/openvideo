package com.example.openvideo.core.diagnostics

import android.content.Context
import android.os.Build
import com.example.openvideo.BuildConfig
import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val DIR_NAME = "crash_logs"
    private const val REMOTE_CONNECT_TIMEOUT_MS = 5_000
    private const val REMOTE_READ_TIMEOUT_MS = 5_000
    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            write(appContext, "uncaught_${timestampFormat.format(Date())}.txt", thread.name, throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun logPlayerError(context: Context, throwable: Throwable) {
        write(
            context.applicationContext,
            "player_${timestampFormat.format(Date())}.txt",
            Thread.currentThread().name,
            throwable
        )
    }

    fun logDiagnostic(context: Context, name: String, body: String) {
        runCatching {
            val safeName = name.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val dir = File(context.applicationContext.filesDir, DIR_NAME).apply { mkdirs() }
            val log = buildDiagnosticLog(name, body)
            File(dir, "${safeName}_${timestampFormat.format(Date())}.txt").writeText(log)
        }
    }

    private fun write(context: Context, fileName: String, threadName: String, throwable: Throwable) {
        runCatching {
            val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            val log = buildLog(threadName, throwable)
            File(dir, fileName).writeText(log)
            if (BuildConfig.REMOTE_CRASH_REPORTING_ENABLED && BuildConfig.FEISHU_WEBHOOK_URL.isNotBlank()) {
                reportRemotely(BuildConfig.FEISHU_WEBHOOK_URL, fileName, log)
            }
        }
    }

    private fun buildDiagnosticLog(name: String, body: String): String {
        return buildString {
            appendLine("type=diagnostic")
            appendLine("name=$name")
            appendLine("time=${Date()}")
            appendLine("thread=${Thread.currentThread().name}")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("sdk=${Build.VERSION.SDK_INT}")
            appendLine()
            append(body)
        }
    }

    private fun buildLog(threadName: String, throwable: Throwable): String {
        val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        return buildString {
            appendLine("time=${Date()}")
            appendLine("thread=$threadName")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("sdk=${Build.VERSION.SDK_INT}")
            appendLine()
            append(stack)
        }
    }

    private fun reportRemotely(webhookUrl: String, title: String, log: String) {
        Thread {
            runCatching {
                val connection = (URL(webhookUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = REMOTE_CONNECT_TIMEOUT_MS
                    readTimeout = REMOTE_READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                connection.outputStream.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                        writer.write(buildRemotePayload(title, log))
                    }
                }
                runCatching { connection.inputStream.close() }
                runCatching { connection.errorStream?.close() }
                connection.disconnect()
            }
        }.apply {
            name = "openvideo-remote-crash-report"
            isDaemon = true
        }.start()
    }

    private fun buildRemotePayload(title: String, log: String): String {
        val text = buildString {
            appendLine("openvideo crash report")
            appendLine("title=$title")
            append(trimForRemote(log))
        }
        return """{"msg_type":"text","content":{"text":"${escapeJson(text)}"}}"""
    }

    private fun trimForRemote(value: String): String =
        if (value.length <= 3_500) value else value.take(3_500) + "\n...truncated"

    private fun escapeJson(value: String): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }

}
