package com.example.openvideo.core.subtitle

data class SubtitleItem(
    val index: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val style: SubtitleCueStyle? = null
)

data class SubtitleCueStyle(
    val fontName: String? = null,
    val fontSizeSp: Float? = null,
    val primaryColor: Int? = null,
    val outlineColor: Int? = null,
    val outlineWidth: Float? = null,
    val shadowDepth: Float? = null,
    val alignment: Int? = null,
    val marginL: Int? = null,
    val marginR: Int? = null,
    val marginV: Int? = null
)
