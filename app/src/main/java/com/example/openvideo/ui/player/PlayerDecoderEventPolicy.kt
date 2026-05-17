package com.example.openvideo.ui.player

/**
 * 把 ExoPlayer `AnalyticsListener` 抛出的 decoder / codec-error 事件，映射成
 * [PlayerStartupTrace] 可消化的事件名集合。
 *
 * - 通过 decoder 名字前缀粗略推断软解 / 硬解（OMX.google.*, c2.android.* 视为软解；
 *   c2.qti.*, c2.exynos.*, OMX.qcom.* 等带厂商前缀视为硬解；ExoPlayer FFmpeg extension
 *   产生的 `libffmpeg*` 视为软解 fallback）。
 * - 同一 decoder 重复 ready 不会重复记录，依赖 [PlayerStartupTrace.recordOnce] 去重。
 *
 * 纯函数，便于在 JVM 单测里覆盖映射规则。
 */
object PlayerDecoderEventPolicy {

    fun videoDecoderEvents(decoderName: String): List<String> = buildList {
        val normalized = decoderName.trim()
        if (normalized.isEmpty()) return@buildList
        add("video_decoder=$normalized")
        if (isSoftwareDecoder(normalized)) add("video_decoder_software")
    }

    fun audioDecoderEvents(decoderName: String): List<String> = buildList {
        val normalized = decoderName.trim()
        if (normalized.isEmpty()) return@buildList
        add("audio_decoder=$normalized")
        if (isSoftwareDecoder(normalized)) add("audio_decoder_software")
    }

    fun videoCodecErrorEvents(errorClassName: String): List<String> =
        codecErrorEvents("video_codec_error", errorClassName)

    fun audioCodecErrorEvents(errorClassName: String): List<String> =
        codecErrorEvents("audio_codec_error", errorClassName)

    fun isSoftwareDecoder(decoderName: String): Boolean {
        val lower = decoderName.lowercase()
        return softwarePrefixes.any { lower.startsWith(it) } || lower.contains("libffmpeg")
    }

    private fun codecErrorEvents(prefix: String, errorClassName: String): List<String> {
        val short = errorClassName.substringAfterLast('.')
            .substringAfterLast('$')
            .trim()
        if (short.isEmpty()) return emptyList()
        return listOf("$prefix=$short")
    }

    private val softwarePrefixes = listOf(
        "omx.google.",
        "c2.android.",
        "ffmpeg",
        "libffmpeg"
    )
}
