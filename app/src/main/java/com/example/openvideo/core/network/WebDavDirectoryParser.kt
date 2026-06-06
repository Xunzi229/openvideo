package com.example.openvideo.core.network

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

object WebDavDirectoryParser {

    data class Entry(
        val name: String,
        val url: String,
        val isDirectory: Boolean,
        val isPlayableVideo: Boolean,
        val sizeBytes: Long?
    )

    private val playableExtensions = setOf(
        "mp4", "m4v", "mkv", "webm", "mov", "avi", "ts", "m3u8", "mpd"
    )

    fun parse(baseUrl: String, xml: String): List<Entry> {
        val normalizedBase = WebDavConnectionPolicy.validateBaseUrl(baseUrl)
            .let { it as? WebDavConnectionPolicy.Validation.Valid }
            ?.normalizedBaseUrl
            ?: baseUrl
        val baseUri = URI(normalizedBase)
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

        val responses = document.getElementsByTagNameNS("*", "response")
        return (0 until responses.length)
            .mapNotNull { index -> responses.item(index) as? Element }
            .mapNotNull { response ->
                val href = response.firstText("href") ?: return@mapNotNull null
                val url = baseUri.resolve(href).toString()
                if (sameDirectory(url, normalizedBase)) return@mapNotNull null
                val isDirectory = response.getElementsByTagNameNS("*", "collection").length > 0 || url.endsWith("/")
                val name = response.firstText("displayname")?.takeIf { it.isNotBlank() }
                    ?: URI(url).path.trimEnd('/').substringAfterLast('/').ifBlank { url }
                val size = response.firstText("getcontentlength")?.toLongOrNull()
                Entry(
                    name = name,
                    url = if (isDirectory && !url.endsWith("/")) "$url/" else url,
                    isDirectory = isDirectory,
                    isPlayableVideo = !isDirectory && extensionOf(url) in playableExtensions,
                    sizeBytes = size
                )
            }
            .sortedWith(
                compareBy<Entry> {
                    when {
                        it.isDirectory -> 0
                        it.isPlayableVideo -> 1
                        else -> 2
                    }
                }.thenBy { it.name.lowercase() }
            )
    }

    private fun sameDirectory(url: String, baseUrl: String): Boolean =
        url.trimEnd('/') == baseUrl.trimEnd('/')

    private fun extensionOf(url: String): String =
        URI(url).path.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    private fun Element.firstText(localName: String): String? {
        val nodes = getElementsByTagNameNS("*", localName)
        if (nodes.length == 0) return null
        return nodes.item(0)?.textContent?.trim()
    }
}
