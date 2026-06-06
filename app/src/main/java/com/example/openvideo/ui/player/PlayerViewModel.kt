package com.example.openvideo.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.example.openvideo.core.network.NetworkPlaybackRetryPolicy
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.player.PlayerAudioDiagnostics
import com.example.openvideo.core.player.PlayerAudioTrackInfo
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.player.RenderMode
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode
import com.example.openvideo.core.subtitle.DualSubtitleText
import com.example.openvideo.core.subtitle.DualSubtitleState
import com.example.openvideo.core.subtitle.PrimarySubtitle
import com.example.openvideo.core.subtitle.SubtitleItem
import com.example.openvideo.core.subtitle.SubtitleLoader
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PlayerUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val decodeMode: DecodeMode = DecodeMode.HARD,
    val renderMode: RenderMode = RenderMode.SURFACE,
    val aspectRatio: AspectRatio = AspectRatio.FIT,
    val contentFrameMode: ContentFrameMode = ContentFrameMode.OFF,
    val speed: Float = 1.0f,
    val isFavorite: Boolean = false,
    val currentSubtitle: String = "",
    val subtitles: List<SubtitleItem> = emptyList(),
    val dualSubtitles: DualSubtitleState = DualSubtitleState()
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val repository: VideoRepository,
    private val playerPrefs: com.example.openvideo.core.prefs.PlayerPrefs,
    private val subtitleLoader: SubtitleLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private var videoId: Long = 0
    private var videoUri: Uri? = null
    val currentVideoUri: Uri? get() = videoUri
    private var videoPath: String = ""
    private var requestHeaders: Map<String, String> = emptyMap()
    private var playerListener: androidx.media3.common.Player.Listener? = null
    private var pendingRestorePosition: Long? = null
    private var pendingAudioSelection: PendingAudioSelection? = null
    private var networkAutoRetryAttempts = 0
    private var networkAutoRetryJob: Job? = null
    private val defaultPlaybackMemory = DefaultPlaybackMemory(
        speed = playerPrefs.speed,
        aspectRatio = playerPrefs.aspectRatio,
        contentFrameMode = playerPrefs.contentFrameMode,
        subtitlesEnabled = playerPrefs.subtitlesEnabled,
        audioMuted = playerPrefs.audioMuted
    )

    private val _sessionQueue = MutableStateFlow<List<VideoItem>>(emptyList())
    val sessionQueue: StateFlow<List<VideoItem>> = _sessionQueue

    /** 当前正在播放的条目 id（与会话列表高亮一致）。 */
    val playingVideoId: Long get() = videoId

    fun isActiveSessionFor(videoId: Long): Boolean =
        videoId != 0L && this.videoId == videoId && player != null

    fun setSessionQueue(videos: List<VideoItem>) {
        _sessionQueue.value = videos
    }

    fun initialize(uri: Uri, title: String, id: Long, path: String = "", requestHeaders: Map<String, String> = emptyMap()) {
        resetNetworkAutoRetry()
        playerListener?.let { playerManager.removeListener(it) }
        playerListener = null

        videoId = id
        videoUri = uri
        videoPath = path
        this.requestHeaders = requestHeaders
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
                    resetNetworkAutoRetry()
                    applyPendingAudioSelection()
                    markPlaybackStarted()
                    applyPendingRestore()
                }
            }
        }
        playerManager.addListener(playerListener!!)
        playerManager.setMediaUri(uri, requestHeaders)

        viewModelScope.launch {
            val isFav = repository.isFavorite(id)
            _uiState.value = _uiState.value.copy(isFavorite = isFav)
        }
    }

    fun restorePosition(videoId: Long, fallbackPositionMs: Long = 0L) {
        viewModelScope.launch {
            val history = repository.getHistory(videoId)
            val restorePositionMs = history?.lastPosition?.takeIf { it > 0 } ?: fallbackPositionMs
            if (restorePositionMs > 0) {
                pendingRestorePosition = restorePositionMs
                applyPendingRestore()
            }
        }
    }

    fun restorePlaybackPreferences(videoId: Long) {
        restorePlaybackPreferences(videoId) {}
    }

    fun restorePlaybackPreferences(videoId: Long, onRestored: () -> Unit) {
        viewModelScope.launch {
            val history = repository.getHistory(videoId)
            val speed: Float
            if (history != null) {
                speed = history.speed
                playerPrefs.speed = history.speed
                playerPrefs.aspectRatio = AspectRatio.fromKey(history.aspectRatioKey)
                playerPrefs.contentFrameMode = ContentFrameMode.fromKey(history.contentFrameKey)
                playerPrefs.externalSubtitleUri = history.externalSubtitleUri
                playerPrefs.subtitlesEnabled = history.subtitlesEnabled
                playerPrefs.audioMuted = history.audioMuted
                pendingAudioSelection = PendingAudioSelection(
                    groupIndex = history.audioTrackGroupIndex,
                    trackIndex = history.audioTrackIndex,
                    muted = history.audioMuted
                )
            } else {
                speed = defaultPlaybackMemory.speed
                playerPrefs.speed = defaultPlaybackMemory.speed
                playerPrefs.aspectRatio = defaultPlaybackMemory.aspectRatio
                playerPrefs.contentFrameMode = defaultPlaybackMemory.contentFrameMode
                playerPrefs.externalSubtitleUri = ""
                playerPrefs.subtitlesEnabled = defaultPlaybackMemory.subtitlesEnabled
                playerPrefs.audioMuted = defaultPlaybackMemory.audioMuted
                pendingAudioSelection = PendingAudioSelection(
                    groupIndex = -1,
                    trackIndex = -1,
                    muted = defaultPlaybackMemory.audioMuted
                )
            }
            setSpeed(
                speed,
                PlayerPlaybackSettings.pitchFor(speed, playerPrefs.speedPreservePitch)
            )
            setAspectRatio(playerPrefs.aspectRatio)
            _uiState.value = _uiState.value.copy(
                speed = speed,
                aspectRatio = playerPrefs.aspectRatio,
                contentFrameMode = playerPrefs.contentFrameMode
            )
            onRestored()
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    /**
     * 播放出错后重新 prepare 当前媒体。
     * 会重置 ExoPlayer 的 media item 并从当前记录的 videoUri 重新加载。
     */
    fun retryPlayback(resetAutoRetry: Boolean = true) {
        if (resetAutoRetry) {
            resetNetworkAutoRetry()
        }
        val uri = videoUri ?: return
        playerManager.setMediaUri(uri, requestHeaders)
    }

    fun handleNetworkAutoRetry(error: PlaybackException): Boolean {
        val decision = NetworkPlaybackRetryPolicy.nextDecision(
            errorCode = error.errorCode,
            cause = error.cause,
            completedAttempts = networkAutoRetryAttempts
        )
        if (decision !is NetworkPlaybackRetryPolicy.Decision.Retry) return false
        networkAutoRetryAttempts = decision.nextAttempt
        networkAutoRetryJob?.cancel()
        networkAutoRetryJob = viewModelScope.launch {
            delay(decision.delayMs)
            retryPlayback(resetAutoRetry = false)
        }
        return true
    }

    private fun resetNetworkAutoRetry() {
        networkAutoRetryAttempts = 0
        networkAutoRetryJob?.cancel()
        networkAutoRetryJob = null
    }

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

    fun audioTracks(): List<PlayerAudioTrackInfo> =
        playerManager.currentAudioTracks()

    fun selectedAudioTrack(): PlayerAudioTrackInfo? =
        audioTracks().firstOrNull { it.selected }

    fun audioDiagnostics(): PlayerAudioDiagnostics =
        playerManager.currentAudioDiagnostics()

    fun selectAudioTrack(track: PlayerAudioTrackInfo) {
        playerPrefs.audioMuted = false
        playerManager.selectAudioTrack(track.groupIndex, track.trackIndex)
    }

    fun disableAudioTrack() {
        playerPrefs.audioMuted = true
        playerManager.disableAudioTrack()
    }

    fun setSubtitles(subtitles: List<SubtitleItem>) {
        _uiState.value = _uiState.value.copy(
            subtitles = subtitles,
            dualSubtitles = DualSubtitleState(primary = PrimarySubtitle(items = subtitles))
        )
    }

    fun loadSubtitles(
        uriString: String,
        videoPath: String,
        showToast: Boolean = false,
        onFinished: (PlayerSubtitleLoadApplyDecision) -> Unit = {}
    ) {
        viewModelScope.launch {
            val subtitles = withContext(Dispatchers.IO) {
                PlayerSubtitleLoadCoordinator.load(
                    uriString,
                    videoPath,
                    subtitleLoader,
                    requestHeaders = requestHeaders,
                    rememberedSubtitlePath = playerPrefs.externalSubtitleUri,
                    languagePreference = playerPrefs.subtitleLanguagePreference()
                )
            }
            val decision = PlayerSubtitleLoadApplyPolicy.afterLoad(subtitles.size, showToast)
            if (decision.shouldApplyToPlayer) {
                setSubtitles(subtitles)
            }
            onFinished(decision)
        }
    }

    fun getCurrentSubtitle(): String {
        return getCurrentDualSubtitle()
            ?.primary
            .orEmpty()
    }

    fun getCurrentDualSubtitle(): DualSubtitleText? {
        val state = _uiState.value
        val positionMs = state.currentPosition + playerPrefs.subtitleDelayMs
        return state.dualSubtitles.textAt(positionMs = positionMs)
    }

    fun playStream(streamUrl: String) {
        val uri = Uri.parse(streamUrl)
        videoId = streamUrl.hashCode().toLong()
        videoUri = uri
        videoPath = streamUrl
        requestHeaders = emptyMap()
        pendingRestorePosition = null
        _uiState.value = _uiState.value.copy(
            title = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: streamUrl,
            currentPosition = 0,
            duration = 0,
            subtitles = emptyList()
        )
        playerManager.setMediaUri(uri, emptyMap())
    }

    fun currentVideoSource(): String =
        videoPath.ifBlank { videoUri?.toString().orEmpty() }

    fun currentVideoItemForDiagnostics(): VideoItem? {
        val uri = videoUri ?: return null
        return _sessionQueue.value.firstOrNull { item ->
            item.id == videoId || item.uri == uri
        } ?: VideoItem(
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
    }

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
        viewModelScope.launch { persistCurrentPlaybackProgress() }
    }

    private suspend fun persistCurrentPlaybackProgress() {
        val uri = videoUri ?: return
        val selectedAudioTrack = selectedAudioTrack()
        repository.saveHistory(
            currentHistoryVideoItem(uri),
            currentPersistablePosition(),
            speed = playerPrefs.speed,
            aspectRatioKey = playerPrefs.aspectRatio.key,
            contentFrameKey = playerPrefs.contentFrameMode.key,
            externalSubtitleUri = playerPrefs.externalSubtitleUri,
            subtitlesEnabled = playerPrefs.subtitlesEnabled,
            audioTrackGroupIndex = selectedAudioTrack?.groupIndex ?: -1,
            audioTrackIndex = selectedAudioTrack?.trackIndex ?: -1,
            audioMuted = playerPrefs.audioMuted
        )
    }

    private fun currentPersistablePosition(): Long =
        if (playerManager.playbackState == Player.STATE_ENDED) 0L else playerManager.currentPosition

    private fun currentHistoryVideoItem(uri: Uri): VideoItem =
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
        )

    /**
     * 在同一会话队列中切换到其它视频（保存当前进度后加载新媒体）。
     */
    fun switchToVideo(item: VideoItem, onSwitched: () -> Unit = {}) {
        viewModelScope.launch {
            persistCurrentPlaybackProgress()
            withContext(Dispatchers.Main.immediate) {
                videoId = item.id
                videoUri = item.uri
                videoPath = item.path
                requestHeaders = emptyMap()
                pendingRestorePosition = null
                _uiState.value = _uiState.value.copy(
                    title = item.title,
                    subtitles = emptyList(),
                    currentPosition = 0,
                    duration = 0
                )
                playerManager.setMediaUri(item.uri, emptyMap())
            }
            val isFav = repository.isFavorite(item.id)
            _uiState.value = _uiState.value.copy(isFavorite = isFav)
            restorePlaybackPreferences(item.id) {
                if (playerPrefs.rememberProgress) {
                    restorePosition(item.id)
                }
                onSwitched()
            }
        }
    }

    private fun markPlaybackStarted() {
        viewModelScope.launch {
            val uri = videoUri ?: return@launch
            val history = repository.getHistory(videoId)
            val selectedAudioTrack = selectedAudioTrack()
            repository.saveHistory(
                currentHistoryVideoItem(uri),
                history?.lastPosition ?: 0L,
                speed = playerPrefs.speed,
                aspectRatioKey = playerPrefs.aspectRatio.key,
                contentFrameKey = playerPrefs.contentFrameMode.key,
                externalSubtitleUri = playerPrefs.externalSubtitleUri,
                subtitlesEnabled = playerPrefs.subtitlesEnabled,
                audioTrackGroupIndex = selectedAudioTrack?.groupIndex ?: history?.audioTrackGroupIndex ?: -1,
                audioTrackIndex = selectedAudioTrack?.trackIndex ?: history?.audioTrackIndex ?: -1,
                audioMuted = playerPrefs.audioMuted
            )
        }
    }

    private fun applyPendingAudioSelection() {
        val selection = pendingAudioSelection ?: return
        pendingAudioSelection = null
        when {
            selection.muted -> disableAudioTrack()
            selection.groupIndex >= 0 && selection.trackIndex >= 0 ->
                playerManager.selectAudioTrack(selection.groupIndex, selection.trackIndex)
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

    private data class PendingAudioSelection(
        val groupIndex: Int,
        val trackIndex: Int,
        val muted: Boolean
    )

    private data class DefaultPlaybackMemory(
        val speed: Float,
        val aspectRatio: AspectRatio,
        val contentFrameMode: ContentFrameMode,
        val subtitlesEnabled: Boolean,
        val audioMuted: Boolean
    )
}
