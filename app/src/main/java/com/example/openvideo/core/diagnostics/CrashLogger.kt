package com.example.openvideo.core.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val DIR_NAME = "crash_logs"
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
            File(dir, "${safeName}_${timestampFormat.format(Date())}.txt").writeText(
                buildString {
                    appendLine("time=${Date()}")
                    appendLine("thread=${Thread.currentThread().name}")
                    appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("sdk=${Build.VERSION.SDK_INT}")
                    appendLine()
                    append(body)
                }
            )
        }
    }

    private fun write(context: Context, fileName: String, threadName: String, throwable: Throwable) {
        runCatching {
            val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            File(dir, fileName).writeText(buildLog(threadName, throwable))
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
}
