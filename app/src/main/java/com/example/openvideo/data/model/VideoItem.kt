package com.example.openvideo.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val title: String,
    val path: String,
    val uri: Uri,
    val duration: Long,       // milliseconds
    val size: Long,           // bytes
    val width: Int,
    val height: Int,
    val dateAdded: Long,      // epoch seconds
    val thumbnailUri: Uri?
)
