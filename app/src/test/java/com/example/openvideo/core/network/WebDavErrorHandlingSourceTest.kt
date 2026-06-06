package com.example.openvideo.core.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class WebDavErrorHandlingSourceTest {

    @Test
    fun connectionClientUsesPolicyForTransportFailureClassification() {
        val source = sourceText("core", "network", "WebDavConnectionClient.kt")

        assertTrue(source.contains("WebDavConnectionPolicy.classifyFailure(error)"))
        assertTrue(source.contains("WebDavConnectionPolicy.ConnectionResult.Failure(WebDavConnectionPolicy.classifyFailure(error))"))
        assertTrue(source.contains("DirectoryResult.Failure(WebDavConnectionPolicy.classifyFailure(error))"))
    }

    @Test
    fun sourcesAndBrowserMapSpecificWebDavErrorsToMessages() {
        val sources = sourceText("ui", "sources", "SourcesFragment.kt")
        val browser = sourceText("ui", "sources", "WebDavBrowserFragment.kt")
        val strings = resourceText("values", "strings.xml")

        listOf(sources, browser).forEach { source ->
            assertTrue(source.contains("WebDavConnectionPolicy.Error.NOT_FOUND -> R.string.webdav_test_not_found"))
            assertTrue(source.contains("WebDavConnectionPolicy.Error.TIMEOUT -> R.string.webdav_test_timeout"))
            assertTrue(source.contains("WebDavConnectionPolicy.Error.CERTIFICATE_ERROR -> R.string.webdav_test_certificate_error"))
        }
        assertTrue(strings.contains("webdav_test_not_found"))
        assertTrue(strings.contains("webdav_test_timeout"))
        assertTrue(strings.contains("webdav_test_certificate_error"))
    }

    private fun sourceText(vararg parts: String): String =
        rootText("app", "src", "main", "java", "com", "example", "openvideo", *parts)

    private fun resourceText(dir: String, name: String): String =
        rootText("app", "src", "main", "res", dir, name)

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
