package com.example.openvideo.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CrashLoggerSourceTest {

    @Test
    fun crashLogsAreSavedLocallyAndReportedToFeishuWithKeyword() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val feishuPayloadMethod = source.substringAfter("private fun buildFeishuPayload(")
            .substringBefore("\n    private fun trimForFeishu")

        assertTrue(source.contains("DIR_NAME = \"crash_logs\""))
        assertTrue(source.contains("FEISHU_WEBHOOK_URL"))
        assertTrue(source.contains("https://open.feishu.cn/open-apis/bot/v2/hook/023b593e-25a9-4fbe-809c-0191875826c9"))
        assertTrue(source.contains("REPORT_KEYWORD = \"openvideo\""))
        assertTrue(source.contains("reportToFeishu("))
        assertTrue(source.contains("HttpURLConnection"))
        assertTrue(source.contains("application/json; charset=utf-8"))
        assertTrue(feishuPayloadMethod.contains("append(REPORT_KEYWORD)"))
        assertFalse(source.contains("appendLine(\"keyword=\$REPORT_KEYWORD\")"))
    }

    @Test
    fun feishuReportRunsOffMainThreadAndCannotCrashTheApp() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val reportMethod = source.substringAfter("private fun reportToFeishu(")
            .substringBefore("\n    private fun buildFeishuPayload")

        assertTrue(reportMethod.contains("Thread {") || reportMethod.contains("Thread("))
        assertTrue(reportMethod.contains("runCatching"))
        assertTrue(reportMethod.contains("isDaemon = true"))
        assertTrue(reportMethod.contains(".start()"))
    }

    @Test
    fun diagnosticLogsStayLocalAndDoNotNotifyFeishu() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val diagnosticMethod = source.substringAfter("fun logDiagnostic(")
            .substringBefore("\n    private fun write")

        assertTrue(diagnosticMethod.contains("writeText(log)"))
        assertFalse(
            "Startup diagnostics are not errors and should not notify Feishu.",
            diagnosticMethod.contains("reportToFeishu(")
        )
    }

    private fun crashLoggerSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "diagnostics",
            "CrashLogger.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
