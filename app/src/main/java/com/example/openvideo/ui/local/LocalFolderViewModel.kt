package com.example.openvideo.ui.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.ui.home.MediaLibraryPolicy
import com.example.openvideo.ui.home.MediaScanSignature
import com.example.openvideo.ui.privacy.PrivacyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalFolderViewModel @Inject constructor(
    private val repository: VideoRepository,
    private val privacyManager: PrivacyManager
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _folders = MutableStateFlow<List<VideoFolder>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val folders: StateFlow<List<VideoFolder>> = _folders
    val videos: StateFlow<List<VideoItem>> = _videos
    val isLoading: StateFlow<Boolean> = _isLoading
    private val continuePlaybackCandidate: StateFlow<LocalContinuePlaybackCandidate?> = combine(
        _videos,
        repository.getHistory()
    ) { visibleVideos, history ->
        val selectedVideoId = LocalContinuePlaybackPolicy.latestPlayableVideoId(
            history = history,
            visibleVideoIds = visibleVideos.map { it.id }.toSet(),
            visibleVideoPaths = visibleVideos.map { it.path }.toSet()
        ) ?: return@combine null
        val selectedHistory = history.firstOrNull { it.videoId == selectedVideoId }
        val selectedVideo = visibleVideos.firstOrNull { video ->
            video.id == selectedVideoId || video.path == selectedHistory?.path
        } ?: return@combine null
        LocalContinuePlaybackCandidate(
            video = selectedVideo,
            positionMs = selectedHistory?.lastPosition ?: 0L
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val continuePlaybackVideo: StateFlow<VideoItem?> = continuePlaybackCandidate
        .map { it?.video }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val continuePlaybackPositionMs: StateFlow<Long> = continuePlaybackCandidate
        .map { it?.positionMs ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private var loadJob: kotlinx.coroutines.Job? = null
    private var lastScanSignature: MediaScanSignature? = null
    private var lastHiddenFolders: List<String> = emptyList()

    fun loadVideos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            repository.scanLocalVideos().collect { videos ->
                val signature = MediaScanSignature.fromVideos(videos)
                val hiddenFolders = privacyManager.getHiddenFolders()
                val hiddenFoldersChanged = hiddenFolders != lastHiddenFolders
                if (!MediaLibraryPolicy.shouldPublishScan(lastScanSignature, signature) && !hiddenFoldersChanged) {
                    _isLoading.value = false
                    return@collect
                }
                lastScanSignature = signature
                lastHiddenFolders = hiddenFolders
                val visibleVideos = MediaLibraryPolicy.visibleVideos(videos, hiddenFolders)
                _videos.value = visibleVideos
                _folders.value = VideoFolderGrouper.groupVideos(visibleVideos)
                _isLoading.value = false
            }
        }
    }
}

private data class LocalContinuePlaybackCandidate(
    val video: VideoItem,
    val positionMs: Long
)
