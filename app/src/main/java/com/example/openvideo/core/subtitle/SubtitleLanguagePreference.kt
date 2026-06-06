package com.example.openvideo.core.subtitle

data class SubtitleLanguagePreference(
    val primary: SubtitleLanguage = SubtitleLanguage.UNKNOWN,
    val secondary: SubtitleLanguage = SubtitleLanguage.UNKNOWN,
    val preferBilingual: Boolean = false
) {
    companion object {
        fun fromKeys(
            primary: String,
            secondary: String,
            preferBilingual: Boolean
        ): SubtitleLanguagePreference =
            SubtitleLanguagePreference(
                primary = languageForKey(primary),
                secondary = languageForKey(secondary),
                preferBilingual = preferBilingual
            )

        private fun languageForKey(key: String): SubtitleLanguage =
            when (key.trim().lowercase()) {
                "chinese" -> SubtitleLanguage.CHINESE
                "english" -> SubtitleLanguage.ENGLISH
                "japanese" -> SubtitleLanguage.JAPANESE
                "korean" -> SubtitleLanguage.KOREAN
                "bilingual" -> SubtitleLanguage.BILINGUAL
                else -> SubtitleLanguage.UNKNOWN
            }
    }
}
