package com.example.openvideo.ui.player

object PlayerSubtitleAutoload {
    private val subtitleExtensions = setOf("srt", "ass", "ssa", "vtt")
    private val languageSuffixPattern = Regex("^[a-z]{2,3}([_-][a-z0-9]{2,8})*$", RegexOption.IGNORE_CASE)

    fun canLoadAsSubtitleUri(uriString: String): Boolean {
        val lower = uriString.substringBefore('?').lowercase()
        return subtitleExtensions.any { lower.endsWith(".$it") }
    }

    fun rankSidecarCandidates(
        videoBaseName: String,
        candidateFileNames: List<String>
    ): List<String> {
        val normalizedBase = videoBaseName.lowercase()
        return candidateFileNames
            .mapNotNull { fileName ->
                val dotIndex = fileName.lastIndexOf('.')
                if (dotIndex <= 0 || dotIndex == fileName.lastIndex) return@mapNotNull null

                val extension = fileName.substring(dotIndex + 1).lowercase()
                if (extension !in subtitleExtensions) return@mapNotNull null

                val nameWithoutExtension = fileName.substring(0, dotIndex)
                val normalizedName = nameWithoutExtension.lowercase()
                val score = when {
                    normalizedName == normalizedBase -> 0
                    normalizedName.startsWith("$normalizedBase.") -> {
                        val suffix = nameWithoutExtension.substring(videoBaseName.length + 1)
                        if (languageSuffixPattern.matches(suffix)) 1 else return@mapNotNull null
                    }
                    else -> return@mapNotNull null
                }
                score to fileName
            }
            .sortedWith(compareBy<Pair<Int, String>> { it.first }.thenBy { it.second.lowercase() })
            .map { it.second }
    }
}
