package com.openvideo.app.core.network

import org.w3c.dom.Element
import java.io.StringReader
import org.xml.sax.InputSource
import org.xml.sax.SAXException
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

    fun parse(
        baseUrl: String,
        xml: String,
        factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
    ): List<Entry> {
        // Reject DTDs before parsing. Android's XML factory does not implement
        // Xerces feature switches; an entity resolver alone cannot stop internal entities.
        require(!xml.contains("<!DOCTYPE", ignoreCase = true)) { "DTD is not allowed" }
        val normalizedBase = WebDavConnectionPolicy.validateBaseUrl(baseUrl)
            .let { it as? WebDavConnectionPolicy.Validation.Valid }
            ?.normalizedBaseUrl
            ?: baseUrl
        val baseUri = URI(normalizedBase)
        val builder = factory.apply {
            isNamespaceAware = true
            setExpandEntityReferences(false)
        }.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> throw SAXException("External entities are not allowed") }
        val document = builder.parse(InputSource(StringReader(xml)))

        val responses = document.getElementsByTagNameNS("*", "response")
        return (0 until responses.length)
            .mapNotNull { index -> responses.item(index) as? Element }
            .mapNotNull { response ->
                val href = response.firstText("href") ?: return@mapNotNull null
                val resolved = baseUri.resolve(href).normalize()
                if (!isTrustedDescendant(baseUri, resolved)) return@mapNotNull null
                val url = resolved.toString()
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

    private fun isTrustedDescendant(base: URI, candidate: URI): Boolean {
        val basePort = if (base.port >= 0) base.port else defaultPort(base.scheme)
        val candidatePort = if (candidate.port >= 0) candidate.port else defaultPort(candidate.scheme)
        return base.scheme.equals(candidate.scheme, ignoreCase = true) &&
            base.host.equals(candidate.host, ignoreCase = true) &&
            basePort == candidatePort &&
            candidate.userInfo == null &&
            candidate.rawPath.orEmpty().startsWith(base.rawPath.orEmpty())
    }

    private fun defaultPort(scheme: String?): Int = if (scheme.equals("https", ignoreCase = true)) 443 else 80

    private fun extensionOf(url: String): String =
        URI(url).path.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    private fun Element.firstText(localName: String): String? {
        val nodes = getElementsByTagNameNS("*", localName)
        if (nodes.length == 0) return null
        return nodes.item(0)?.textContent?.trim()
    }
}
