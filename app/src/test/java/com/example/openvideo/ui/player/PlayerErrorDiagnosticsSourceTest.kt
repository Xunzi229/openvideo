package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerErrorDiagnosticsSourceTest {

    @Test
    fun playerErrorsAttachCurrentMediaDiagnosticsToCrashLog() {
        val eventSource = kotlinSource("PlayerEventController.kt")

        assertTrue(
            "Player errors must build media diagnostics before writing the crash log.",
            eventSource.contains("PlayerErrorDiagnostics.build(")
        )
        assertTrue(
            "Player errors must pass media diagnostics into CrashLogger.",
            eventSource.contains("CrashLogger.logPlayerError(activity, error, diagnostics)")
        )
    }

    @Test
    fun mediaDiagnosticsIncludeStoredResolverFileAndPlayerMetadata() {
        val source = kotlinSource("PlayerErrorDiagnostics.kt")

        assertTrue(source.contains("video_id="))
        assertTrue(source.contains("title="))
        assertTrue(source.contains("uri="))
        assertTrue(source.contains("path="))
        assertTrue(source.contains("duration_ms="))
        assertTrue(source.contains("size_bytes="))
        assertTrue(source.contains("width="))
        assertTrue(source.contains("height="))
        assertTrue(source.contains("date_added="))

        assertTrue(source.contains("content_resolver.mime_type="))
        assertTrue(source.contains("OpenableColumns.DISPLAY_NAME"))
        assertTrue(source.contains("OpenableColumns.SIZE"))

        assertTrue(source.contains("file.exists="))
        assertTrue(source.contains("file.can_read="))
        assertTrue(source.contains("file.length_bytes="))
        assertTrue(source.contains("file.last_modified_ms="))

        assertTrue(source.contains("player.current_position_ms="))
        assertTrue(source.contains("player.duration_ms="))
        assertTrue(source.contains("player.buffered_position_ms="))
        assertTrue(source.contains("player.current_media_uri="))
    }

    private fun kotlinSource(name: String): String {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            name
        )
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
