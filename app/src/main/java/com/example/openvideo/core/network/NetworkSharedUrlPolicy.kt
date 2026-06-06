package com.example.openvideo.core.network

object NetworkSharedUrlPolicy {

    private const val ACTION_SEND = "android.intent.action.SEND"
    private const val ACTION_VIEW = "android.intent.action.VIEW"

    private val supportedUrlPattern = Regex("""(?i)\b(?:https?|rtsp)://\S+""")

    fun extractPlaybackUrl(
        action: String?,
        mimeType: String?,
        sharedText: String?,
        dataString: String?
    ): String? {
        val candidate = when (action) {
            ACTION_VIEW -> dataString
            ACTION_SEND -> {
                if (!acceptsSharedText(mimeType)) return null
                supportedUrlPattern.find(sharedText.orEmpty())?.value
            }
            else -> null
        } ?: return null

        return when (val validation = NetworkUrlPolicy.validatePlaybackUrl(candidate.trimEnd('.', ',', ';'))) {
            is NetworkUrlPolicy.Validation.Valid -> validation.normalizedUrl
            is NetworkUrlPolicy.Validation.Invalid -> null
        }
    }

    private fun acceptsSharedText(mimeType: String?): Boolean {
        return mimeType == null || mimeType == "text/plain" || mimeType.startsWith("text/")
    }
}
