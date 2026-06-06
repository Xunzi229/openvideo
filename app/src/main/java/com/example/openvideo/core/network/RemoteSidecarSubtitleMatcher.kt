package com.example.openvideo.core.network

object RemoteSidecarSubtitleMatcher {
    private val subtitleExtensions = setOf("srt", "vtt")
    private val languageSuffixPattern = Regex("^[a-z]{2,3}([_-][a-z0-9]{2,8})*$", RegexOption.IGNORE_CASE)

    data class Item(
        val name: String,
        val url: String,
        val isDirectory: Boolean,
        val isPlayableVideo: Boolean
    )

    fun matchForVideo(
        video: Item,
        candidates: List<Item>
    ): Item? {
        if (!video.isPlayableVideo) return null
        val videoBaseName = video.name.substringBeforeLast('.', missingDelimiterValue = video.name)
        val normalizedBase = videoBaseName.lowercase()

        return candidates
            .asSequence()
            .filter { !it.isDirectory }
            .mapNotNull { candidate ->
                val dotIndex = candidate.name.lastIndexOf('.')
                if (dotIndex <= 0 || dotIndex == candidate.name.lastIndex) return@mapNotNull null
                val extension = candidate.name.substring(dotIndex + 1).lowercase()
                if (extension !in subtitleExtensions) return@mapNotNull null

                val nameWithoutExtension = candidate.name.substring(0, dotIndex)
                val normalizedName = nameWithoutExtension.lowercase()
                val score = when {
                    normalizedName == normalizedBase -> 0
                    normalizedName.startsWith("$normalizedBase.") -> {
                        val suffix = nameWithoutExtension.substring(videoBaseName.length + 1)
                        if (languageSuffixPattern.matches(suffix)) 1 else return@mapNotNull null
                    }
                    else -> return@mapNotNull null
                }
                Triple(score, candidate.name.lowercase(), candidate)
            }
            .sortedWith(compareBy<Triple<Int, String, Item>> { it.first }.thenBy { it.second })
            .firstOrNull()
            ?.third
    }
}
