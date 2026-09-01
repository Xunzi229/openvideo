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
            "Player errors must build media diagnostics for the background logger.",
            eventSource.contains("PlayerErrorDiagnostics.build(")
        )
        assertTrue(
            "Player errors must use the non-blocking logger.",
            eventSource.contains("CrashLogger.logPlayerErrorAsync(")
        )
        val captureIndex = eventSource.indexOf("PlayerErrorDiagnostics.capture(")
        val asyncLogIndex = eventSource.indexOf("CrashLogger.logPlayerErrorAsync(", captureIndex)
        val resolverBuildIndex = eventSource.indexOf("PlayerErrorDiagnostics.build(", asyncLogIndex)
        assertTrue("Player state must be captured before dispatch.", captureIndex >= 0)
        assertTrue("The async logger must receive the captured state.", asyncLogIndex > captureIndex)
        assertTrue("Resolver metadata must be built inside the async provider.", resolverBuildIndex > asyncLogIndex)
        assertTrue(eventSource.contains("val diagnosticsContext = activity.applicationContext"))
        assertTrue(eventSource.contains("PlayerFailureClassificationPolicy.category("))
        assertTrue(eventSource.contains("player_error.code="))
        assertTrue(eventSource.contains("player_error.category="))
        assertTrue(eventSource.contains("category = category"))
        assertTrue("Player input failures must remain observable.", !eventSource.contains("reportRemotely"))
    }

    @Test
    fun mediaDiagnosticsIncludeStoredResolverFileAndPlayerMetadata() {
        val source = kotlinSource("PlayerErrorDiagnostics.kt")

        assertTrue(source.contains("data class Snapshot("))
        assertTrue(source.contains("fun capture("))
        assertTrue(source.contains("fun build(context: Context, snapshot: Snapshot)"))
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
        assertTrue(source.contains("CrashRedactionPolicy::redactUri"))
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
