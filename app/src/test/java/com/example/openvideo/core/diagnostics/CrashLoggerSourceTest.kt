package com.example.openvideo.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CrashLoggerSourceTest {

    @Test
    fun crashLogsAreSavedLocallyWithoutHardcodedRemoteWebhook() {
        val source = String(Files.readAllBytes(crashLoggerSource()))

        assertTrue(source.contains("DIR_NAME = \"crash_logs\""))
        assertTrue(source.contains("writeText(log)"))
        assertTrue(source.contains("BuildConfig.FEISHU_WEBHOOK_URL"))
        assertTrue(source.contains("BuildConfig.REMOTE_CRASH_REPORTING_ENABLED"))
        assertFalse("Crash logging must not hardcode a Feishu webhook.", source.contains("FEISHU_WEBHOOK_URL ="))
        assertFalse("Crash logging must not hardcode a remote hook URL.", source.contains("open-apis/bot"))
    }

    @Test
    fun crashWritePathOnlyTriggersRemoteReportingWhenBuildConfigEnablesIt() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val writeMethod = source.substringAfter("private fun write(")
            .substringBefore("\n    private fun buildDiagnosticLog")

        assertTrue(writeMethod.contains("writeText(log)"))
        assertTrue(writeMethod.contains("BuildConfig.REMOTE_CRASH_REPORTING_ENABLED"))
        assertTrue(writeMethod.contains("BuildConfig.FEISHU_WEBHOOK_URL.isNotBlank()"))
        assertTrue(writeMethod.contains("reportRemotely(BuildConfig.FEISHU_WEBHOOK_URL"))
        assertFalse(writeMethod.contains("reportToFeishu("))
    }

    @Test
    fun remoteReportRunsOffMainThreadAndCannotCrashTheApp() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val reportMethod = source.substringAfter("private fun reportRemotely(")
            .substringBefore("\n    private fun buildRemotePayload")

        assertTrue(reportMethod.contains("Thread {") || reportMethod.contains("Thread("))
        assertTrue(reportMethod.contains("runCatching"))
        assertTrue(reportMethod.contains("HttpURLConnection"))
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
