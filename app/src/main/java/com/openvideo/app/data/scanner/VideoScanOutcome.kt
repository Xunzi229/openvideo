package com.openvideo.app.data.scanner

import com.openvideo.app.data.model.VideoItem

sealed class VideoScanOutcome {
    data class Progress(val scannedCount: Int) : VideoScanOutcome()
    data class Success(val videos: List<VideoItem>) : VideoScanOutcome()
    data object PermissionDenied : VideoScanOutcome()
    data class Error(val message: String) : VideoScanOutcome()
}
