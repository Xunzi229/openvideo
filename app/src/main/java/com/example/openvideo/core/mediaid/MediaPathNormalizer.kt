package com.example.openvideo.core.mediaid

import java.util.Locale

data class NormalizedMediaPath(
    val original: String,
    val displayPath: String,
    val comparisonKey: String,
    val fileName: String,
    val parentKey: String
)

object MediaPathNormalizer {

    fun normalize(pathOrUri: String): NormalizedMediaPath? {
        val original = pathOrUri.trim()
        if (original.isBlank()) return null

        val displayPath = trimTrailingSeparator(collapseSeparators(original.replace('\\', '/')))
        val comparisonKey = displayPath.lowercase(Locale.ROOT)
        return NormalizedMediaPath(
            original = original,
            displayPath = displayPath,
            comparisonKey = comparisonKey,
            fileName = displayPath.substringAfterLast('/'),
            parentKey = comparisonKey.substringBeforeLast('/', missingDelimiterValue = "")
        )
    }

    private fun collapseSeparators(path: String): String {
        val schemeMarker = "://"
        val schemeIndex = path.indexOf(schemeMarker)
        if (schemeIndex > 1) {
            val prefix = path.substring(0, schemeIndex + schemeMarker.length)
            val rest = path.substring(schemeIndex + schemeMarker.length)
                .replace(Regex("""/+"""), "/")
            return prefix + rest
        }
        return path.replace(Regex("""/+"""), "/")
    }

    private fun trimTrailingSeparator(path: String): String {
        var end = path.length
        while (end > 1 && path[end - 1] == '/') {
            end--
        }
        return path.substring(0, end)
    }
}
