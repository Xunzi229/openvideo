package com.example.openvideo.core.subtitle

interface OnlineSubtitleClient {
    suspend fun search(request: OnlineSubtitleSearchRequest): List<OnlineSubtitleSearchResult>

    suspend fun download(request: OnlineSubtitleDownloadRequest): ByteArray
}
