package com.openvideo.app.ui.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.openvideo.app.core.network.NetworkPlaybackRetryPolicy
import com.openvideo.app.core.player.DecodeMode
import com.openvideo.app.core.player.PlayerAudioDiagnostics
import com.openvideo.app.core.player.PlayerAudioTrackInfo
import com.openvideo.app.core.player.PlayerManager
import com.openvideo.app.core.player.RenderMode
import com.openvideo.app.core.prefs.AspectRatio
import com.openvideo.app.core.prefs.ContentFrameMode
import com.openvideo.app.core.subtitle.DualSubtitleText
import com.openvideo.app.core.subtitle.DualSubtitleState
import com.openvideo.app.core.subtitle.PrimarySubtitle
import com.openvideo.app.core.subtitle.SecondarySubtitle
import com.openvideo.app.core.subtitle.SubtitleCandidate
import com.openvideo.app.core.subtitle.SubtitleDelayCorrectionPolicy
import com.openvideo.app.core.subtitle.SubtitleExportWriter
import com.openvideo.app.core.subtitle.SubtitleInfo
import com.openvideo.app.core.subtitle.SubtitleInfoPolicy
import com.openvideo.app.core.subtitle.SubtitleItem
import com.openvideo.app.core.subtitle.SubtitleLoader
import com.openvideo.app.core.subtitle.SubtitleUtf8ExportPolicy
import com.openvideo.app.data.model.VideoItem
import com.openvideo.app.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val playerPrefs: com.openvideo.app.core.prefs.PlayerPrefs,
    private val subtitleLoader: SubtitleLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private var videoId: Long = 0
    private var videoUri: Uri? = null
    val currentVideoUri: Uri? get() = videoUri
    private var videoPath: String = ""
    private var requestHeaders: Map<String, String> = emptyMap()
    val currentRequestHeaders: Map<String, String> get() = requestHeaders.toMap()
    private val subtitleLoadMutex = Mutex()
    private val primarySubtitleRequest = LatestPlayerRequest()
    private val secondarySubtitleRequest = LatestPlayerRequest()
    private var mediaGeneration = 0L
    internal val currentMediaGeneration: Long get() = mediaGeneration
    private var primarySubtitleJob: Job? = null
    private var secondarySubtitleJob: Job? = null
    private var switchJob: Job? = null
    private var reloadPrimarySubtitle: (() -> Unit)? = null
    private var reloadSecondarySubtitle: (() -> Unit)? = null
    private var playbackPreferencesRestored = false
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
    var sessionQueueToken: String? = null
        private set

    /** 当前正在播放的条目 id（与会话列表高亮一致）。 */
    val playingVideoId: Long get() = videoId

    private fun invalidateMediaRequests() {
        mediaGeneration++
        primarySubtitleRequest.next()
        secondarySubtitleRequest.next()
        primarySubtitleJob?.cancel()
        secondarySubtitleJob?.cancel()
        resetNetworkAutoRetry()
        pendingRestorePosition = null
        pendingAudioSelection = null
        playbackPreferencesRestored = false
        reloadPrimarySubtitle = null
        reloadSecondarySubtitle = null
        _uiState.value = _uiState.value.withoutSubtitles()
    }

    fun isActiveSessionFor(videoId: Long): Boolean =
        videoId != 0L && this.videoId == videoId && player != null

    fun adoptActiveSession(
        uri: Uri,
        title: String,
        id: Long,
        path: String = "",
        requestHeaders: Map<String, String> = emptyMap()
    ): Boolean {
        val activePlayer = playerManager.player ?: return false
        val activeUri = activePlayer.currentMediaItem?.localConfiguration?.uri ?: return false
        if (activeUri != uri) return false

        switchJob?.cancel()
        invalidateMediaRequests()
        playbackPreferencesRestored = true
        playerListener?.let { playerManager.removeListener(it) }
        videoId = id
        videoUri = uri
        videoPath = path
        this.requestHeaders = requestHeaders.ifEmpty { playerManager.currentMediaRequestHeaders() }
        _uiState.value = _uiState.value.copy(
            title = title,
            isPlaying = activePlayer.isPlaying,
            currentPosition = activePlayer.currentPosition,
            duration = activePlayer.duration.takeIf { it > 0L } ?: 0L
        )
        playerListener = object : androidx.media3.common.Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) resetNetworkAutoRetry()
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                applyPendingAudioSelection()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.value = _uiState.value.copy(duration = playerManager.duration)
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    resetNetworkAutoRetry()
                    applyPendingAudioSelection()
                    if (playbackPreferencesRestored) markPlaybackStarted()
                    applyPendingRestore()
                }
            }
        }
        playerManager.addListener(playerListener!!)
        viewModelScope.launch {
            val isFav = repository.isFavorite(id)
            if (id == videoId) _uiState.value = _uiState.value.copy(isFavorite = isFav)
        }
        return true
    }

    fun setSessionQueue(videos: List<VideoItem>, token: String?) {
        _sessionQueue.value = videos
        sessionQueueToken = token
    }

    fun setSessionQueueToken(token: String) {
        sessionQueueToken = token
    }

    fun initialize(uri: Uri, title: String, id: Long, path: String = "", requestHeaders: Map<String, String> = emptyMap()) {
        switchJob?.cancel()
        invalidateMediaRequests()
        playerListener?.let { playerManager.removeListener(it) }
        playerListener = null

        videoId = id
        videoUri = uri
        videoPath = path
        this.requestHeaders = requestHeaders
        _uiState.value = _uiState.value.copy(title = title)

        playerManager.initialize(uri)
        playerListener = object : androidx.media3.common.Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) resetNetworkAutoRetry()
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                applyPendingAudioSelection()
            }

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
                    if (playbackPreferencesRestored) markPlaybackStarted()
                    applyPendingRestore()
                }
            }
        }
        playerManager.addListener(playerListener!!)
        playerManager.setMediaUri(uri, requestHeaders)

        viewModelScope.launch {
            val isFav = repository.isFavorite(id)
            if (id != videoId) return@launch
            _uiState.value = _uiState.value.copy(isFavorite = isFav)
        }
    }

    fun restorePosition(videoId: Long, fallbackPositionMs: Long = 0L) {
        val generation = mediaGeneration
        viewModelScope.launch {
            val history = repository.getHistory(videoId)
            if (videoId != this@PlayerViewModel.videoId || generation != mediaGeneration) return@launch
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
        val generation = mediaGeneration
        viewModelScope.launch {
            val history = repository.getHistory(videoId)
            if (videoId != this@PlayerViewModel.videoId || generation != mediaGeneration) return@launch
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
            playbackPreferencesRestored = true
            applyPendingAudioSelection()
            onRestored()
            if (playerManager.playbackState == Player.STATE_READY) markPlaybackStarted()
        }
    }

    fun pausePlayback() {
        resetNetworkAutoRetry()
        player?.pause()
    }

    fun togglePlayPause() {
        when {
            player?.playWhenReady == true -> pausePlayback()
            player?.playerError != null -> retryPlayback()
            else -> playerManager.togglePlayPause()
        }
    }

    /**
     * 播放出错后重新 prepare 当前媒体。
     * 会重置 ExoPlayer 的 media item 并从当前记录的 videoUri 重新加载。
     */
    fun retryPlayback(resetAutoRetry: Boolean = true) {
        if (resetAutoRetry) {
            resetNetworkAutoRetry()
        }
        val uri = videoUri ?: return
        val retryPosition = if (player?.isCurrentMediaItemLive == true) null else playerManager.currentPosition
        if (resetAutoRetry && _uiState.value.decodeMode == DecodeMode.SOFT) {
            playerManager.initialize(uri)
            playerListener?.let { playerManager.addListener(it) }
        }
        playerManager.setMediaUri(uri, requestHeaders, retryPosition)
    }

    fun handleNetworkAutoRetry(error: PlaybackException): Boolean {
        if (player?.playWhenReady != true) return false
        val decision = NetworkPlaybackRetryPolicy.nextDecision(
            errorCode = error.errorCode,
            cause = error.cause,
            completedAttempts = networkAutoRetryAttempts
        )
        if (decision !is NetworkPlaybackRetryPolicy.Decision.Retry) return false
        if (videoUri?.scheme !in setOf("http", "https", "rtsp")) return false
        networkAutoRetryAttempts = decision.nextAttempt
        networkAutoRetryJob?.cancel()
        val expectedUri = videoUri
        networkAutoRetryJob = viewModelScope.launch {
            delay(decision.delayMs)
            if (videoUri != expectedUri || player?.playWhenReady != true) return@launch
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
            dualSubtitles = _uiState.value.dualSubtitles.copy(primary = PrimarySubtitle(items = subtitles))
        )
    }

    fun setSecondarySubtitles(subtitles: List<SubtitleItem>, enabled: Boolean = true) {
        val current = _uiState.value.dualSubtitles
        _uiState.value = _uiState.value.copy(
            dualSubtitles = current.copy(
                secondary = SecondarySubtitle(items = subtitles, enabled = enabled)
            )
        )
    }

    fun setSecondarySubtitlesEnabled(enabled: Boolean) {
        val current = _uiState.value.dualSubtitles
        _uiState.value = _uiState.value.copy(
            dualSubtitles = current.copy(
                secondary = current.secondary.copy(enabled = enabled)
            )
        )
    }

    fun loadSecondarySubtitles(
        uriString: String,
        videoPath: String,
        onFinished: (PlayerSubtitleLoadApplyDecision) -> Unit = {}
    ) {
        reloadSecondarySubtitle = {
            val enabled = _uiState.value.dualSubtitles.secondary.enabled
            loadSecondarySubtitles(uriString, videoPath) {
                setSecondarySubtitlesEnabled(enabled)
            }
        }
        secondarySubtitleJob?.cancel()
        val token = secondarySubtitleRequest.next()
        val headers = requestHeaders.toMap()
        secondarySubtitleJob = viewModelScope.launch {
            val subtitles = withContext(Dispatchers.IO) {
                subtitleLoadMutex.withLock {
                    ensureActive()
                    PlayerSubtitleLoadCoordinator.load(
                        uriString,
                        videoPath,
                        subtitleLoader,
                        requestHeaders = headers,
                        explicitSubtitle = true
                    )
                }
            }
            if (!secondarySubtitleRequest.accepts(token)) return@launch
            val decision = PlayerSubtitleLoadApplyPolicy.afterLoad(subtitles.size, requestedToast = true)
            if (decision.shouldApplyToPlayer) {
                setSecondarySubtitles(subtitles)
            }
            onFinished(decision)
        }
    }

    fun loadSubtitles(
        uriString: String,
        videoPath: String,
        showToast: Boolean = false,
        onFinished: (PlayerSubtitleLoadApplyDecision) -> Unit = {},
        onCandidateChoiceRequired: (List<SubtitleCandidate>) -> Unit = {}
    ) {
        reloadPrimarySubtitle = {
            loadSubtitles(uriString, videoPath, showToast, onFinished, onCandidateChoiceRequired)
        }
        primarySubtitleJob?.cancel()
        val token = primarySubtitleRequest.next()
        val headers = requestHeaders.toMap()
        val rememberedPath = playerPrefs.externalSubtitleUri
        val languagePreference = playerPrefs.subtitleLanguagePreference()
        primarySubtitleJob = viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                subtitleLoadMutex.withLock {
                    ensureActive()
                    PlayerSubtitleLoadCoordinator.loadWithOutcome(
                        uriString,
                        videoPath,
                        subtitleLoader,
                        requestHeaders = headers,
                        rememberedSubtitlePath = rememberedPath,
                        languagePreference = languagePreference,
                        explicitSubtitle = uriString.isNotBlank() && uriString == rememberedPath
                    )
                }
            }
            if (!primarySubtitleRequest.accepts(token) || videoPath != this@PlayerViewModel.videoPath) return@launch
            when (outcome) {
                is PlayerSubtitleLoadOutcome.Loaded -> {
                    val decision = PlayerSubtitleLoadApplyPolicy.afterLoad(outcome.subtitles.size, showToast)
                    if (decision.shouldApplyToPlayer) {
                        setSubtitles(outcome.subtitles)
                    }
                    onFinished(decision)
                }
                is PlayerSubtitleLoadOutcome.RequiresUserChoice -> {
                    onCandidateChoiceRequired(outcome.candidates)
                    onFinished(PlayerSubtitleLoadApplyDecision(false, PlayerSubtitleLoadToastKind.NONE))
                }
                PlayerSubtitleLoadOutcome.None -> {
                    val decision = PlayerSubtitleLoadApplyPolicy.afterLoad(0, showToast)
                    onFinished(decision)
                }
            }
        }
    }

    fun reloadSubtitlesForEncoding() {
        reloadPrimarySubtitle?.invoke()
        reloadSecondarySubtitle?.invoke()
    }

    sealed class SubtitleExportResult {
        data object Success : SubtitleExportResult()
        data object NoSubtitles : SubtitleExportResult()
        data object NoDelay : SubtitleExportResult()
        data object OriginalOverwriteBlocked : SubtitleExportResult()
        data object OpenStreamFailed : SubtitleExportResult()
        data object WriteFailed : SubtitleExportResult()
    }

    fun hasCurrentSubtitles(): Boolean =
        _uiState.value.subtitles.isNotEmpty()

    suspend fun writeCurrentSubtitleUtf8ExportTo(
        context: Context,
        uri: Uri
    ): SubtitleExportResult = withContext(Dispatchers.IO) {
        val subtitles = _uiState.value.subtitles
        if (subtitles.isEmpty()) return@withContext SubtitleExportResult.NoSubtitles
        if (SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(
                targetUri = uri.toString(),
                originalSubtitleUri = playerPrefs.externalSubtitleUri
            )
        ) {
            return@withContext SubtitleExportResult.OriginalOverwriteBlocked
        }
        val plan = SubtitleUtf8ExportPolicy.planSrtCopy(
            items = subtitles,
            sourceName = currentVideoSource()
        )
        val stream = try {
            context.contentResolver.openOutputStream(uri)
                ?: return@withContext SubtitleExportResult.OpenStreamFailed
        } catch (_: Exception) {
            return@withContext SubtitleExportResult.OpenStreamFailed
        }
        try {
            stream.use { out ->
                when (SubtitleExportWriter.writePlanToOutputStream(out, plan)) {
                    is SubtitleExportWriter.Result.Success -> SubtitleExportResult.Success
                    is SubtitleExportWriter.Result.Failure -> SubtitleExportResult.WriteFailed
                }
            }
        } catch (_: Exception) {
            SubtitleExportResult.WriteFailed
        }
    }

    fun suggestedSubtitleExportFileName(): String =
        SubtitleUtf8ExportPolicy.planSrtCopy(
            items = _uiState.value.subtitles,
            sourceName = currentVideoSource()
        ).suggestedCopyName

    suspend fun writeCurrentSubtitleDelayCorrectionExportTo(
        context: Context,
        uri: Uri
    ): SubtitleExportResult = withContext(Dispatchers.IO) {
        val subtitles = _uiState.value.subtitles
        if (subtitles.isEmpty()) return@withContext SubtitleExportResult.NoSubtitles
        if (playerPrefs.subtitleDelayMs == 0) return@withContext SubtitleExportResult.NoDelay
        if (SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(
                targetUri = uri.toString(),
                originalSubtitleUri = playerPrefs.externalSubtitleUri
            )
        ) {
            return@withContext SubtitleExportResult.OriginalOverwriteBlocked
        }
        val delayPlan = SubtitleDelayCorrectionPolicy.planShiftedCopy(
            items = subtitles,
            deltaMs = playerPrefs.subtitleDelayMs,
            sourceName = currentVideoSource()
        )
        val exportPlan = SubtitleUtf8ExportPolicy.planSrtCopy(
            items = delayPlan.items,
            sourceName = delayPlan.suggestedCopyName
        )
        val stream = try {
            context.contentResolver.openOutputStream(uri)
                ?: return@withContext SubtitleExportResult.OpenStreamFailed
        } catch (_: Exception) {
            return@withContext SubtitleExportResult.OpenStreamFailed
        }
        try {
            stream.use { out ->
                when (SubtitleExportWriter.writePlanToOutputStream(out, exportPlan)) {
                    is SubtitleExportWriter.Result.Success -> SubtitleExportResult.Success
                    is SubtitleExportWriter.Result.Failure -> SubtitleExportResult.WriteFailed
                }
            }
        } catch (_: Exception) {
            SubtitleExportResult.WriteFailed
        }
    }

    fun suggestedSubtitleDelayCorrectionExportFileName(): String =
        SubtitleDelayCorrectionPolicy.planShiftedCopy(
            items = _uiState.value.subtitles,
            deltaMs = playerPrefs.subtitleDelayMs,
            sourceName = currentVideoSource()
        ).suggestedCopyName

    fun currentSubtitleInfo(): SubtitleInfo =
        SubtitleInfoPolicy.summarize(
            items = _uiState.value.subtitles,
            sourceLabel = currentVideoSource(),
            encoding = playerPrefs.subtitleEncoding
        )

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
        switchJob?.cancel()
        invalidateMediaRequests()
        playbackPreferencesRestored = true
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
        viewModelScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { persistCurrentPlaybackProgress() }
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
        _sessionQueue.value.firstOrNull { it.id == videoId && it.uri == uri }?.let { item ->
            item.copy(duration = playerManager.duration.takeIf { it > 0L } ?: item.duration)
        } ?: VideoItem(
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
    fun switchToVideo(
        item: VideoItem,
        onPlayerRecreated: () -> Unit = {},
        onSwitched: () -> Unit = {}
    ) {
        switchJob?.cancel()
        resetNetworkAutoRetry()
        switchJob = viewModelScope.launch {
            persistCurrentPlaybackProgress()
            invalidateMediaRequests()
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
                playerManager.initialize(item.uri)
                playerListener?.let { playerManager.addListener(it) }
                onPlayerRecreated()
                playerManager.setMediaUri(item.uri, emptyMap())
            }
            val isFav = repository.isFavorite(item.id)
            if (item.id == videoId) {
                _uiState.value = _uiState.value.copy(isFavorite = isFav)
            }
            restorePlaybackPreferences(item.id) {
                if (playerPrefs.rememberProgress) {
                    restorePosition(item.id)
                }
                onSwitched()
            }
        }
    }

    private fun markPlaybackStarted() {
        val uri = videoUri ?: return
        val item = currentHistoryVideoItem(uri)
        val expectedVideoId = videoId
        viewModelScope.launch {
            val history = repository.getHistory(expectedVideoId)
            if (videoId != expectedVideoId || videoUri != uri) return@launch
            val selectedAudioTrack = selectedAudioTrack()
            repository.saveHistory(
                item,
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
        if (playerManager.playbackState != Player.STATE_READY) return
        if (!selection.muted && selection.groupIndex >= 0 && player?.currentTracks?.groups.isNullOrEmpty()) return
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
        switchJob?.cancel()
        invalidateMediaRequests()
        detachFromActiveSession()
        playerManager.release()
    }

    fun detachFromActiveSession() {
        playerListener?.let { playerManager.removeListener(it) }
        playerListener = null
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
