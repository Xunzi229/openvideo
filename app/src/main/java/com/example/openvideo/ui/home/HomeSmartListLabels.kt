package com.example.openvideo.ui.home

import androidx.annotation.StringRes
import com.example.openvideo.R
import com.example.openvideo.core.metadata.MediaSmartListType

object HomeSmartListLabels {

    @StringRes
    fun labelRes(type: MediaSmartListType): Int = when (type) {
        MediaSmartListType.RECENTLY_ADDED -> R.string.home_smart_recently_added
        MediaSmartListType.IN_PROGRESS -> R.string.home_smart_in_progress
        MediaSmartListType.COMPLETED -> R.string.home_smart_completed
        MediaSmartListType.LARGE_FILES -> R.string.home_smart_large_files
        MediaSmartListType.UHD -> R.string.home_smart_uhd
        MediaSmartListType.HDR -> R.string.home_smart_hdr
        MediaSmartListType.WITH_SUBTITLES -> R.string.home_smart_with_subtitles
    }
}
