package com.example.openvideo.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class WebDavCacheSourceTest {

    @Test
    fun directoryClientUsesShortMemoryCacheForListingsOnly() {
        val source = sourceText("WebDavConnectionClient.kt")
        val listDirectory = source.substringAfter("suspend fun listDirectory(")
            .substringBefore("\n}")

        assertTrue(source.contains("private val webDavMemoryCache: WebDavMemoryCache"))
        assertTrue(listDirectory.contains("val cacheKey = webDavMemoryCache.cacheKey("))
        assertTrue(listDirectory.contains("namespace = \"directory\""))
        assertTrue(listDirectory.contains("webDavMemoryCache.getDirectory(cacheKey)?.let"))
        assertTrue(listDirectory.contains("webDavMemoryCache.putDirectory(cacheKey, entries)"))
        assertFalse("Video streams must not be downloaded into the WebDAV metadata cache", source.contains("putVideo"))
    }

    @Test
    fun settingsClearCacheAlsoClearsWebDavMemoryCache() {
        val source = rootText("app", "src", "main", "java", "com", "example", "openvideo", "ui", "settings", "SettingsViewModel.kt")

        assertTrue(source.contains("private val webDavMemoryCache: WebDavMemoryCache"))
        assertTrue(source.contains("webDavMemoryCache.clear()"))
    }

    private fun sourceText(name: String): String =
        rootText("app", "src", "main", "java", "com", "example", "openvideo", "core", "network", name)

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
