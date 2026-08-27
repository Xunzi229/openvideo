package com.example.openvideo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompatibilityPlayerViewModel @Inject constructor(
    private val repository: VideoRepository,
    private val playerPrefs: PlayerPrefs
) : ViewModel() {
    private var request: CompatibilityPlaybackRequest? = null
    private var positionMs = 0L
    private var durationMs = 0L
    private var playbackEnded = false

    fun initialize(request: CompatibilityPlaybackRequest) {
        if (this.request != null) return
        this.request = request
        positionMs = request.startPositionMs
        durationMs = request.durationMs
    }

    fun updatePlayback(positionMs: Long, durationMs: Long) {
        this.positionMs = positionMs.coerceAtLeast(0L)
        if (durationMs > 0L) this.durationMs = durationMs
        playbackEnded = false
    }

    fun markEnded() {
        playbackEnded = true
    }

    fun saveHistory() {
        val request = request ?: return
        viewModelScope.launch {
            repository.saveHistory(
                video = VideoItem(
                    id = request.videoId,
                    title = request.title,
                    path = request.videoPath.ifBlank { request.uri.toString() },
                    uri = request.uri,
                    duration = durationMs,
                    size = 0L,
                    width = 0,
                    height = 0,
                    dateAdded = 0L,
                    thumbnailUri = null
                ),
                position = if (playbackEnded) 0L else positionMs,
                speed = request.speed,
                aspectRatioKey = playerPrefs.aspectRatio.key,
                contentFrameKey = playerPrefs.contentFrameMode.key,
                externalSubtitleUri = playerPrefs.externalSubtitleUri,
                subtitlesEnabled = playerPrefs.subtitlesEnabled,
                audioTrackGroupIndex = -1,
                audioTrackIndex = -1,
                audioMuted = request.audioMuted
            )
        }
    }
}
