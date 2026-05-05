package com.example.openvideo.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.core.player.AspectRatio
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.player.RenderMode
import com.example.openvideo.core.subtitle.SubtitleItem
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val decodeMode: DecodeMode = DecodeMode.HARD,
    val renderMode: RenderMode = RenderMode.SURFACE,
    val aspectRatio: AspectRatio = AspectRatio.DEFAULT,
    val speed: Float = 1.0f,
    val isFavorite: Boolean = false,
    val currentSubtitle: String = "",
    val subtitles: List<SubtitleItem> = emptyList()
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val repository: VideoRepository,
    private val playerPrefs: com.example.openvideo.core.prefs.PlayerPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private var videoId: Long = 0
    private var videoUri: Uri? = null

    fun initialize(uri: Uri, title: String, id: Long) {
        videoId = id
        videoUri = uri
        _uiState.value = _uiState.value.copy(title = title)

        val player = playerManager.initialize()
        playerManager.setMediaUri(uri)

        playerManager.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.value = _uiState.value.copy(
                    duration = playerManager.duration
                )
            }
        })

        viewModelScope.launch {
            val isFav = repository.isFavorite(id)
            _uiState.value = _uiState.value.copy(isFavorite = isFav)
        }
    }

    fun restorePosition(videoId: Long) {
        viewModelScope.launch {
            val history = repository.getHistory(videoId)
            if (history != null && history.lastPosition > 0) {
                val duration = playerManager.duration
                if (duration > 0 && history.lastPosition < duration - 10_000) {
                    playerManager.seekTo(history.lastPosition)
                }
            }
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    fun seekForward() {
        val ms = playerPrefs.seekInterval * 1000L
        playerManager.seekForward(ms)
    }

    fun seekBackward() {
        val ms = playerPrefs.seekInterval * 1000L
        playerManager.seekBackward(ms)
    }
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)

    fun updatePosition() {
        _uiState.value = _uiState.value.copy(
            currentPosition = playerManager.currentPosition,
            duration = playerManager.duration
        )
    }

    fun setSpeed(speed: Float) {
        playerManager.setSpeed(speed)
        _uiState.value = _uiState.value.copy(speed = speed)
    }

    fun setSubtitles(subtitles: List<SubtitleItem>) {
        _uiState.value = _uiState.value.copy(subtitles = subtitles)
    }

    fun getCurrentSubtitle(): String {
        val state = _uiState.value
        if (state.subtitles.isEmpty()) return ""
        val pos = state.currentPosition
        return state.subtitles.find { pos in it.startTimeMs..it.endTimeMs }?.text ?: ""
    }

    fun setDecodeMode(mode: DecodeMode) {
        playerManager.applyDecodeMode(mode)
        _uiState.value = _uiState.value.copy(decodeMode = mode)
    }

    fun setRenderMode(mode: RenderMode) {
        playerManager.renderMode = mode
        _uiState.value = _uiState.value.copy(renderMode = mode)
    }

    fun setAspectRatio(ratio: AspectRatio) {
        playerManager.aspectRatio = ratio
        _uiState.value = _uiState.value.copy(aspectRatio = ratio)
    }

    fun saveHistory() {
        viewModelScope.launch {
            videoUri?.let { uri ->
                repository.saveHistory(
                    VideoItem(
                        id = videoId,
                        title = _uiState.value.title,
                        path = uri.toString(),
                        uri = uri,
                        duration = playerManager.duration,
                        size = 0,
                        width = 0,
                        height = 0,
                        dateAdded = 0,
                        thumbnailUri = null
                    ),
                    playerManager.currentPosition
                )
            }
        }
    }

    fun release() {
        saveHistory()
        playerManager.release()
    }

    val player get() = playerManager.player
}
