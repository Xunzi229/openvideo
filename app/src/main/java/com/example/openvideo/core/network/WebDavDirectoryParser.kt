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
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            setExpandEntityReferences(false)
        }.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

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
