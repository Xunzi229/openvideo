package com.example.openvideo.data.scanner

object MediaStorePathPolicy {

    fun playbackSource(
        sdkInt: Int,
        dataPath: String,
        contentUri: String
    ): String = if (sdkInt >= 29) contentUri else dataPath.ifBlank { contentUri }

    fun libraryPath(
        dataPath: String,
        relativePath: String,
        displayName: String,
        externalStorageRoot: String,
        contentUri: String
    ): String {
        if (dataPath.isNotBlank()) return normalize(dataPath)
        if (relativePath.isBlank() || displayName.isBlank()) return contentUri
        return listOf(
            externalStorageRoot.trimEnd('/', '\\'),
            relativePath.trim('/', '\\'),
            displayName.trimStart('/', '\\')
        ).filter(String::isNotBlank).joinToString("/").let(::normalize)
    }

    private fun normalize(path: String): String =
        path.replace('\\', '/').replace(Regex("/+"), "/")
}
