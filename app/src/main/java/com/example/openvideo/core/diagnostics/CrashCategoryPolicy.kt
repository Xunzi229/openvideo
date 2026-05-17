package com.example.openvideo.core.diagnostics

/**
 * 纯函数式的崩溃 / 播放失败归类。
 *
 * 将 `Throwable` 与可选的来源标签（如 `player`、`uncaught_<ts>`）映射到固定的 [CrashCategory]，
 * 让日志文件名、日志头部 `category=` 字段、远程通知都能用同一份口径。
 *
 * 不做 IO，纯文本判断，便于在 JVM 单测里覆盖映射规则。
 */
object CrashCategoryPolicy {

    /** 来自 [CrashLogger.logPlayerError] 的标签。 */
    const val SOURCE_PLAYER = "player"

    /** 来自 [CrashLogger.install] 默认未捕获异常处理。 */
    const val SOURCE_UNCAUGHT = "uncaught"

    fun categorize(throwable: Throwable, source: String? = null): CrashCategory {
        val chain = throwableChain(throwable)
        val classNames = chain.map { it.javaClass.name }
        val messages = chain.mapNotNull { it.message }.map { it.lowercase() }
        val stack = chain.flatMap { it.stackTrace.asSequence().map(StackTraceElement::toString) }

        if (source.equals(SOURCE_PLAYER, ignoreCase = true)) return CrashCategory.PLAYBACK
        if (classNames.any { it.contains("PlaybackException") || it.contains("ExoPlaybackException") }) {
            return CrashCategory.PLAYBACK
        }
        if (classNames.any { it.contains("DecoderException") || it.contains("MediaCodec") } ||
            stack.any { it.contains("media3") || it.contains("exoplayer") || it.contains("MediaCodec") }
        ) {
            return CrashCategory.PLAYBACK
        }

        if (classNames.any { it.contains("SecurityException") } ||
            messages.any { it.contains("permission") || it.contains("denied") }
        ) {
            return CrashCategory.PERMISSION
        }

        if (messages.any { it.contains("mediastore") || it.contains("media store") } ||
            stack.any { it.contains("MediaStore") || it.contains("ContentResolver") }
        ) {
            return CrashCategory.MEDIA_STORE
        }

        if (classNames.any {
                it.contains("InflateException") ||
                    it.contains("NotFoundException") ||
                    it.contains("ResourcesNotFoundException")
            } ||
            stack.any { it.contains("LayoutInflater") || it.contains("Resources.getValue") }
        ) {
            return CrashCategory.RESOURCE_INFLATE
        }

        if (source != null && source.contains("release", ignoreCase = true)) {
            return CrashCategory.RELEASE_SCRIPT
        }

        return CrashCategory.UNKNOWN
    }

    private fun throwableChain(throwable: Throwable): List<Throwable> {
        val list = mutableListOf<Throwable>()
        var current: Throwable? = throwable
        val seen = HashSet<Throwable>()
        while (current != null && seen.add(current)) {
            list += current
            current = current.cause
        }
        return list
    }
}

enum class CrashCategory(val token: String) {
    PLAYBACK("playback"),
    PERMISSION("permission"),
    MEDIA_STORE("mediastore"),
    RESOURCE_INFLATE("resource_inflate"),
    RELEASE_SCRIPT("release"),
    UNKNOWN("unknown")
}
