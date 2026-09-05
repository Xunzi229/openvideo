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
    fun crashWritePathKeepsLocalLogsBeforeFilteringRemoteReports() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val writeMethod = source.substringAfter("private fun write(")
            .substringBefore("\n    private fun buildDiagnosticLog")

        assertTrue(writeMethod.contains("writeText(log)"))
        assertTrue(writeMethod.contains("BuildConfig.REMOTE_CRASH_REPORTING_ENABLED"))
        assertTrue(writeMethod.contains("BuildConfig.FEISHU_WEBHOOK_URL.isNotBlank()"))
        assertFalse(writeMethod.contains("AppPrefs("))
        assertFalse(writeMethod.contains("remoteCrashReportingEnabled"))
        assertTrue(writeMethod.contains("CrashReportOutbox.enqueue("))
        assertTrue(writeMethod.contains("flushAfterWrite"))
        val localWriteIndex = writeMethod.indexOf("writeText(log)")
        val policyIndex = writeMethod.indexOf("CrashReportingPolicy.shouldReport(source, throwable)")
        val enqueueIndex = writeMethod.indexOf("CrashReportOutbox.enqueue(")
        assertTrue("Expected failures must still be saved locally.", policyIndex > localWriteIndex)
        assertTrue("Remote eligibility must be checked before enqueue.", enqueueIndex > policyIndex)
        assertTrue(writeMethod.contains("reportThrottle.shouldEnqueue(source, throwable, SystemClock.elapsedRealtime())"))

        assertTrue(
            "write() must classify the crash via CrashCategoryPolicy.",
            writeMethod.contains("CrashCategoryPolicy.categorize(throwable, source)")
        )
        assertTrue(
            "write() must prefix file name with source + category.",
            writeMethod.contains("\"\${source}_\${category.token}_")
        )
    }

    @Test
    fun crashLogBodyEmitsCategoryAndRedactsPaths() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val buildLog = source.substringAfter("private fun buildLog(")
            .substringBefore("\n    private fun buildRemotePayload")

        assertTrue("category= line must be present.", buildLog.contains("appendLine(\"category=\${category.token}\")"))
        assertTrue("source= line must be present.", buildLog.contains("appendLine(\"source=\$source\")"))
        assertTrue(
            "Stack trace must go through CrashRedactionPolicy.",
            buildLog.contains("CrashRedactionPolicy.redact(stack)")
        )

        val diagnosticBuilder = source.substringAfter("private fun buildDiagnosticLog(")
            .substringBefore("\n    private fun buildLog")
        assertTrue(
            "Diagnostic body must go through CrashRedactionPolicy.",
            diagnosticBuilder.contains("CrashRedactionPolicy.redact(body)")
        )
    }

    @Test
    fun crashAndDiagnosticLogsIncludeAppVersion() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val diagnosticBuilder = source.substringAfter("private fun buildDiagnosticLog(")
            .substringBefore("\n    private fun buildLog")
        val buildLog = source.substringAfter("private fun buildLog(")
            .substringBefore("\n    private fun buildRemotePayload")

        assertTrue("Crash logs must include the app version name.", buildLog.contains("BuildConfig.VERSION_NAME"))
        assertTrue("Crash logs must include the app version code.", buildLog.contains("BuildConfig.VERSION_CODE"))
        assertTrue(
            "Diagnostic logs must include the app version name.",
            diagnosticBuilder.contains("BuildConfig.VERSION_NAME")
        )
        assertTrue(
            "Diagnostic logs must include the app version code.",
            diagnosticBuilder.contains("BuildConfig.VERSION_CODE")
        )
    }

    @Test
    fun playerErrorLogsAreBuiltOffMainAndKeepImmediateLatestSummary() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val logPlayerError = source.substringAfter("fun logPlayerErrorAsync(")
            .substringBefore("\n    fun flushPendingReports")
        val buildLog = source.substringAfter("private fun buildLog(")
            .substringBefore("\n    private fun buildRemotePayload")

        assertTrue(source.contains("Executors.newSingleThreadExecutor"))
        assertTrue(source.contains("openvideo-player-diagnostics"))
        assertTrue(logPlayerError.contains("diagnosticsProvider: () -> String?"))
        assertTrue(logPlayerError.contains("playerLogExecutor.execute"))
        assertTrue(logPlayerError.contains("runCatching(diagnosticsProvider).getOrNull()"))
        assertTrue(logPlayerError.contains("category: CrashCategory? = null"))
        assertTrue(logPlayerError.contains("categoryOverride = resolvedCategory"))
        assertTrue(logPlayerError.contains("latestPlayerErrorLog = buildLog("))
        assertTrue(source.contains("playerLogSequence.incrementAndGet()"))
        assertTrue(source.contains("playerSequence == playerLogSequence.get()"))
        assertTrue(source.contains("latestPlayerErrorLog ?: runCatching"))
        assertTrue(
            "Crash log diagnostics must be redacted before being written.",
            buildLog.contains("CrashRedactionPolicy.redact(diagnostics)")
        )
    }

    @Test
    fun uncaughtCrashesDelegateToAndroidWhileExactVendorThreadFailureIsIsolated() {
        val source = String(Files.readAllBytes(crashLoggerSource()))
        val installMethod = source.substringAfter("fun install(")
            .substringBefore("\n    fun logPlayerErrorAsync")

        assertTrue(installMethod.contains("UncaughtExceptionPolicy.shouldIsolateFromProcessTermination("))
        assertTrue(installMethod.contains("source = source"))
        assertTrue(installMethod.contains("flushAfterWrite = isolateVendorThread"))
        assertTrue(installMethod.contains("if (!isolateVendorThread)"))
        assertTrue(installMethod.contains("previous?.uncaughtException(thread, throwable)"))
        assertTrue(source.contains("CrashReportOutbox.enqueue("))
        assertTrue(source.contains("fun flushPendingReports("))
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

    @Test
    fun remotePayloadDistinguishesPlaybackFailuresFromUncaughtCrashes() {
        val source = String(Files.readAllBytes(crashLoggerSource()))

        assertTrue(source.contains("event=\${eventName(source)}"))
        assertTrue(source.contains("openvideo playback failure report"))
        assertTrue(source.contains("openvideo oem integration failure report"))
        assertTrue(source.contains("openvideo crash report"))
    }

    @Test
    fun outOfMemoryLogsUseABoundedStackTracePath() {
        val source = String(Files.readAllBytes(crashLoggerSource()))

        assertTrue(source.contains("category != CrashCategory.MEMORY"))
        assertTrue(source.contains("take(MAX_OOM_STACK_FRAMES)"))
        assertTrue(source.contains("MAX_OOM_STACK_FRAMES = 64"))
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
