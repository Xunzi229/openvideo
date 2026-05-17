package com.example.openvideo.core.diagnostics

/**
 * 写入崩溃日志前对文本做轻量脱敏：把可能涉及用户文件名的私有路径替换为占位符，
 * 但保留扩展名 / id 等对定位问题有用的信息。
 *
 * 纯函数，便于在 JVM 单测里覆盖。
 */
object CrashRedactionPolicy {

    private const val MEDIA_URI_PLACEHOLDER_PREFIX = "content_media_id_"

    private val storagePathRegex = Regex("""/storage/emulated/\d+/[^\s)>\]"']*""")
    private val sdcardPathRegex = Regex("""/sdcard/[^\s)>\]"']*""")
    private val rawFileUriRegex = Regex("""file:///?[^\s)>\]"']*""")
    private val mediaContentUriRegex = Regex(
        """content://media/(?:internal|external(?:_primary)?)/[A-Za-z]+/(\d+)"""
    )
    private val genericContentUriRegex = Regex("""content://[A-Za-z0-9.\-_/]+[^\s)>\]"']*""")

    fun redact(text: String): String {
        if (text.isEmpty()) return text
        var result = text

        result = mediaContentUriRegex.replace(result) { match ->
            val id = match.groupValues[1]
            "$MEDIA_URI_PLACEHOLDER_PREFIX$id"
        }
        result = rawFileUriRegex.replace(result) { match -> redactedPath(match.value) }
        result = storagePathRegex.replace(result) { match -> redactedPath(match.value) }
        result = sdcardPathRegex.replace(result) { match -> redactedPath(match.value) }
        result = genericContentUriRegex.replace(result) { _ -> "<content_uri>" }
        return result
    }

    private fun redactedPath(path: String): String {
        val extension = path.substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.isNotEmpty() && it.length <= 6 && it.all { ch -> ch.isLetterOrDigit() } }
        return if (extension != null) "<file>.$extension" else "<file>"
    }
}
