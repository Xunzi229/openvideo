package com.example.openvideo.core.diagnostics

import android.content.Context
import android.os.Build
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
    private const val FEISHU_WEBHOOK_URL =
        "https://open.feishu.cn/open-apis/bot/v2/hook/023b593e-25a9-4fbe-809c-0191875826c9"
    private const val REPORT_KEYWORD = "openvideo"
    private const val FEISHU_CONNECT_TIMEOUT_MS = 5_000
    private const val FEISHU_READ_TIMEOUT_MS = 5_000
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
            reportToFeishu(fileName, log)
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

    private fun reportToFeishu(title: String, log: String) {
        Thread {
            runCatching {
                val connection = (URL(FEISHU_WEBHOOK_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = FEISHU_CONNECT_TIMEOUT_MS
                    readTimeout = FEISHU_READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                connection.outputStream.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                        writer.write(buildFeishuPayload(title, log))
                    }
                }
                runCatching { connection.inputStream.close() }
                runCatching { connection.errorStream?.close() }
                connection.disconnect()
            }
        }.apply {
            name = "openvideo-feishu-crash-report"
            isDaemon = true
        }.start()
    }

    private fun buildFeishuPayload(title: String, log: String): String {
        val text = buildString {
            append(REPORT_KEYWORD)
            appendLine(" crash report")
            appendLine("title=$title")
            append(trimForFeishu(log))
        }
        return """{"msg_type":"text","content":{"text":"${escapeJson(text)}"}}"""
    }

    private fun trimForFeishu(value: String): String =
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
