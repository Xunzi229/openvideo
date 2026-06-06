package com.example.openvideo.ui.home

import com.example.openvideo.R
import com.example.openvideo.core.metadata.MediaSmartListType
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSmartListLabelsTest {

    @Test
    fun labelsCoverAllSmartListTypes() {
        assertEquals(R.string.home_smart_recently_added, HomeSmartListLabels.labelRes(MediaSmartListType.RECENTLY_ADDED))
        assertEquals(R.string.home_smart_in_progress, HomeSmartListLabels.labelRes(MediaSmartListType.IN_PROGRESS))
        assertEquals(R.string.home_smart_completed, HomeSmartListLabels.labelRes(MediaSmartListType.COMPLETED))
        assertEquals(R.string.home_smart_large_files, HomeSmartListLabels.labelRes(MediaSmartListType.LARGE_FILES))
        assertEquals(R.string.home_smart_uhd, HomeSmartListLabels.labelRes(MediaSmartListType.UHD))
        assertEquals(R.string.home_smart_hdr, HomeSmartListLabels.labelRes(MediaSmartListType.HDR))
        assertEquals(R.string.home_smart_with_subtitles, HomeSmartListLabels.labelRes(MediaSmartListType.WITH_SUBTITLES))
    }
}
