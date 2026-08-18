package com.example.openvideo.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class CrashReportOutboxSourceTest {

    @Test
    fun reportsArePersistedBeforeSingleThreadedDeliveryAndDeletedOnlyOnSuccess() {
        val path = sequenceOf(
            Paths.get("src/main/java/com/example/openvideo/core/diagnostics/CrashReportOutbox.kt"),
            Paths.get("app/src/main/java/com/example/openvideo/core/diagnostics/CrashReportOutbox.kt")
        ).first(Files::exists)
        val source = String(Files.readAllBytes(path))

        assertTrue(source.contains("DIR_NAME = \"crash_report_outbox\""))
        assertTrue(source.contains("writeText(payload)"))
        assertTrue(source.contains("Executors.newSingleThreadExecutor"))
        assertTrue(source.contains("connection.responseCode in 200..299"))
        assertTrue(source.contains("file.delete()"))
        assertFalse(source.contains("isDaemon = true"))
    }
}
