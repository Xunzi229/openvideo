package com.example.openvideo.core.ui

enum class ScreenBreakpoint {
    COMPACT,   // 0~599dp - 手机竖屏
    MEDIUM,    // 600~839dp - 手机横屏/小平板
    EXPANDED;  // 840dp+ - 大平板/折叠屏展开

    val isCompact: Boolean get() = this == COMPACT
    val isMedium: Boolean get() = this == MEDIUM
    val isExpanded: Boolean get() = this == EXPANDED
    val isAtLeastMedium: Boolean get() = this != COMPACT
}
