package com.example.openvideo.core.ui

enum class AppleActionStyle {
    DEFAULT,
    DESTRUCTIVE,
    CANCEL
}

data class AppleAction(
    val title: CharSequence,
    val style: AppleActionStyle = AppleActionStyle.DEFAULT,
    val bold: Boolean = false,
    val selected: Boolean? = null,
    val dismissOnClick: Boolean = true,
    val onClick: () -> Unit = {}
)
