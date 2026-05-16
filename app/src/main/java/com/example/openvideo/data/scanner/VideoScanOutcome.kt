package com.example.openvideo.data.scanner

import com.example.openvideo.data.model.VideoItem

sealed class VideoScanOutcome {
    data class Progress(val scannedCount: Int) : VideoScanOutcome()
    data class Success(val videos: List<VideoItem>) : VideoScanOutcome()
    data object PermissionDenied : VideoScanOutcome()
    data class Error(val message: String) : VideoScanOutcome()
}
