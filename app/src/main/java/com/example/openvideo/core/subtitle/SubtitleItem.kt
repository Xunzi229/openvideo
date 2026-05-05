package com.example.openvideo.core.subtitle

data class SubtitleItem(
    val index: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)
