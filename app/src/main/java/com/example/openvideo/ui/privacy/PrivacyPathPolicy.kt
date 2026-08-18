package com.example.openvideo.ui.privacy

import java.util.Locale

object PrivacyPathPolicy {

    fun canonical(path: String): String {
        val trimmed = path.trim().replace('\\', '/')
        if (trimmed.isBlank()) return ""
        val schemeIndex = trimmed.indexOf("://")
        val normalized = if (schemeIndex > 0) {
            val prefix = trimmed.substring(0, schemeIndex + 3)
            prefix + trimmed.substring(schemeIndex + 3).replace(Regex("/+"), "/")
        } else {
            trimmed.replace(Regex("/+"), "/")
        }
        return normalized.trimEnd('/').ifBlank { "/" }
    }

    fun isWithin(path: String, folder: String): Boolean {
        val candidate = canonical(path).lowercase(Locale.ROOT)
        val parent = canonical(folder).lowercase(Locale.ROOT)
        if (candidate.isBlank() || parent.isBlank()) return false
        if (candidate.startsWith("content://") || parent.startsWith("content://")) return false
        return candidate == parent || candidate.startsWith("$parent/")
    }
}
