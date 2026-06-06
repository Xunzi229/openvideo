package com.example.openvideo.core.subtitle

enum class OnlineSubtitleSearchTrigger {
    MANUAL,
    AUTOMATIC_VIDEO_OPEN
}

data class OnlineSubtitleSearchRequest(
    val title: String,
    val season: Int? = null,
    val episode: Int? = null,
    val language: SubtitleLanguage = SubtitleLanguage.UNKNOWN,
    val trigger: OnlineSubtitleSearchTrigger,
    val includeFileHash: Boolean = false
) {
    companion object {
        fun manual(
            title: String,
            season: Int? = null,
            episode: Int? = null,
            language: SubtitleLanguage = SubtitleLanguage.UNKNOWN
        ): OnlineSubtitleSearchRequest {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotBlank()) { "Online subtitle search title must not be blank." }
            return OnlineSubtitleSearchRequest(
                title = normalizedTitle,
                season = season,
                episode = episode,
                language = language,
                trigger = OnlineSubtitleSearchTrigger.MANUAL
            )
        }
    }
}

data class OnlineSubtitleDownloadRequest(
    val resultId: String,
    val fileName: String
)

data class OnlineSubtitleSearchResult(
    val id: String,
    val language: SubtitleLanguage,
    val fileName: String,
    val releaseName: String,
    val source: String,
    val downloadCount: Int? = null,
    val rating: Float? = null,
    val download: OnlineSubtitleDownloadRequest
)

sealed interface OnlineSubtitlePrivacyDecision {
    data object Allowed : OnlineSubtitlePrivacyDecision
    data class Blocked(val reason: String) : OnlineSubtitlePrivacyDecision
}

object OnlineSubtitlePrivacyPolicy {
    fun evaluate(request: OnlineSubtitleSearchRequest): OnlineSubtitlePrivacyDecision {
        if (request.trigger != OnlineSubtitleSearchTrigger.MANUAL) {
            return OnlineSubtitlePrivacyDecision.Blocked("Online subtitle search must be started manually.")
        }
        if (request.includeFileHash) {
            return OnlineSubtitlePrivacyDecision.Blocked("File hash upload is disabled by default.")
        }
        return OnlineSubtitlePrivacyDecision.Allowed
    }
}
