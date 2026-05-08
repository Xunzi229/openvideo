package com.example.openvideo.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.player.RenderMode
import com.example.openvideo.core.prefs.AspectRatio
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
    val aspectRatio: AspectRatio = AspectRatio.FIT,
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
    private var videoPath: String = ""
    private var playerListener: androidx.media3.common.Player.Listener? = null
    private var pendingRestorePosition: Long? = null

    fun initialize(uri: Uri, title: String, id: Long, path: String = "") {
        videoId = id
        videoUri = uri
        videoPath = path
        _uiState.value = _uiState.value.copy(title = title)

        playerManager.initialize(uri)
        playerListener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.value = _uiState.value.copy(
                    duration = playerManager.duration
                )
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    markPlaybackStarted()
                    applyPendingRestore()
                }
            }
        }
        playerManager.addListener(playerListener!!)
        playerManager.setMediaUri(uri)

        viewModelScope.launch {
            val isFav = repository.isFavorite(id)
            _uiState.value = _uiState.value.copy(isFavorite = isFav)
        }
    }

    fun restorePosition(videoId: Long) {
        viewModelScope.launch {
            val history = repository.getHistory(videoId)
            if (history != null && history.lastPosition > 0) {
                pendingRestorePosition = history.lastPosition
                applyPendingRestore()
            }
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    fun seekForward(ms: Long = playerPrefs.seekInterval * 1000L) {
        playerManager.seekForward(ms)
    }

    fun seekBackward(ms: Long = playerPrefs.seekInterval * 1000L) {
        playerManager.seekBackward(ms)
    }
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)

    fun updatePosition() {
        _uiState.value = _uiState.value.copy(
            currentPosition = playerManager.currentPosition,
            duration = playerManager.duration
        )
    }

    fun setSpeed(speed: Float, pitch: Float = 1.0f) {
        playerManager.setSpeed(speed, pitch)
        _uiState.value = _uiState.value.copy(speed = speed)
    }

    fun setRepeatMode(repeatMode: Int) {
        playerManager.setRepeatMode(repeatMode)
    }

    fun setVolumeBoost(enabled: Boolean) {
        playerManager.setVolumeBoost(enabled)
    }

    fun setSubtitles(subtitles: List<SubtitleItem>) {
        _uiState.value = _uiState.value.copy(subtitles = subtitles)
    }

    fun getCurrentSubtitle(): String {
        val state = _uiState.value
        if (state.subtitles.isEmpty()) return ""
        val pos = state.currentPosition + playerPrefs.subtitleDelayMs
        return state.subtitles.find { pos in it.startTimeMs..it.endTimeMs }?.text ?: ""
    }

    fun playStream(streamUrl: String) {
        val uri = Uri.parse(streamUrl)
        videoId = streamUrl.hashCode().toLong()
        videoUri = uri
        videoPath = streamUrl
        pendingRestorePosition = null
        _uiState.value = _uiState.value.copy(
            title = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: streamUrl,
            currentPosition = 0,
            duration = 0,
            subtitles = emptyList()
        )
        playerManager.setMediaUri(uri)
    }

    fun currentVideoSource(): String =
        videoPath.ifBlank { videoUri?.toString().orEmpty() }

    fun currentVideoShareText(): String {
        val source = currentVideoSource()
        val title = _uiState.value.title.ifBlank { source }
        return listOf(title, source).filter { it.isNotBlank() }.joinToString("\n")
    }

    fun addCurrentVideoToDefaultPlaylist() {
        viewModelScope.launch {
            val uri = videoUri ?: return@launch
            repository.addToQuickPlaylist(
                VideoItem(
                    id = videoId,
                    title = _uiState.value.title.ifBlank { uri.lastPathSegment.orEmpty() },
                    path = videoPath.ifBlank { uri.toString() },
                    uri = uri,
                    duration = playerManager.duration,
                    size = 0,
                    width = 0,
                    height = 0,
                    dateAdded = 0,
                    thumbnailUri = null
                )
            )
        }
    }

    fun exportClip(startMs: Long, endMs: Long, callback: (Boolean, String?) -> Unit) {
        val uri = videoUri ?: run {
            callback(false, null)
            return
        }
        playerManager.exportClip(uri, startMs, endMs, callback)
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
                        path = videoPath.ifBlank { uri.toString() },
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

    private fun markPlaybackStarted() {
        viewModelScope.launch {
            val uri = videoUri ?: return@launch
            val history = repository.getHistory(videoId)
            repository.saveHistory(
                VideoItem(
                    id = videoId,
                    title = _uiState.value.title,
                    path = videoPath.ifBlank { uri.toString() },
                    uri = uri,
                    duration = playerManager.duration,
                    size = 0,
                    width = 0,
                    height = 0,
                    dateAdded = 0,
                    thumbnailUri = null
                ),
                history?.lastPosition ?: 0L
            )
        }
    }

    private fun applyPendingRestore() {
        val savedPosition = pendingRestorePosition ?: return
        val target = PlaybackResumePolicy.restoreTarget(
            savedPositionMs = savedPosition,
            durationMs = playerManager.duration
        ) ?: run {
            pendingRestorePosition = null
            return
        }
        playerManager.seekTo(target)
        pendingRestorePosition = null
    }

    fun release() {
        saveHistory()
        playerListener?.let { playerManager.removeListener(it) }
        playerListener = null
        playerManager.release()
    }

    val player get() = playerManager.player
}
