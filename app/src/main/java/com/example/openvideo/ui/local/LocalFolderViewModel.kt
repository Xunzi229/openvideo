package com.example.openvideo.ui.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.data.scanner.VideoScanOutcome
import com.example.openvideo.ui.home.MediaLibraryEmptyState
import com.example.openvideo.ui.home.MediaLibraryPolicy
import com.example.openvideo.ui.home.MediaLibraryScanProgress
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
    private val privacyManager: PrivacyManager,
    private val appPrefs: AppPrefs
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _pinnedFolderKeys = MutableStateFlow(appPrefs.pinnedFolderKeys)
    private val _folders = MutableStateFlow<List<VideoFolder>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _scanProgress = MutableStateFlow<MediaLibraryScanProgress?>(null)
    val scanProgress: StateFlow<MediaLibraryScanProgress?> = _scanProgress
    private val _permissionDenied = MutableStateFlow(false)
    private val _scanError = MutableStateFlow(false)
    private val _scannedVideoCount = MutableStateFlow(0)

    val folders: StateFlow<List<VideoFolder>> = combine(_folders, _pinnedFolderKeys) { folders, pinnedKeys ->
        VideoFolderFilterPolicy.displayFolderList(folders, pinnedKeys)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos
    val isLoading: StateFlow<Boolean> = _isLoading
    val emptyState: StateFlow<MediaLibraryEmptyState> = combine(
        _isLoading,
        _scannedVideoCount,
        _folders,
        _permissionDenied,
        _scanError
    ) { isLoading, scannedCount, folders, permissionDenied, scanError ->
        MediaLibraryPolicy.emptyState(
            isLoading = isLoading,
            scannedCount = scannedCount,
            visibleCount = folders.sumOf { it.videoCount },
            permissionDenied = permissionDenied,
            scanError = scanError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaLibraryEmptyState.LOADING)
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

    fun togglePinnedFolder(folderKey: String) {
        val updated = VideoFolderFilterPolicy.togglePinned(_pinnedFolderKeys.value, folderKey)
        _pinnedFolderKeys.value = updated
        appPrefs.pinnedFolderKeys = updated
    }

    fun loadVideos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _scanProgress.value = null
            repository.scanLocalVideos().collect { outcome ->
                when (outcome) {
                    is VideoScanOutcome.Progress -> {
                        _scanProgress.value = MediaLibraryScanProgress(outcome.scannedCount)
                    }
                    VideoScanOutcome.PermissionDenied -> {
                        _scanProgress.value = null
                        _permissionDenied.value = true
                        _scanError.value = false
                        _videos.value = emptyList()
                        _folders.value = emptyList()
                        _scannedVideoCount.value = 0
                        _isLoading.value = false
                    }
                    is VideoScanOutcome.Error -> {
                        _scanProgress.value = null
                        _permissionDenied.value = false
                        _scanError.value = true
                        _isLoading.value = false
                    }
                    is VideoScanOutcome.Success -> {
                        _scanProgress.value = null
                        _permissionDenied.value = false
                        _scanError.value = false
                        val videos = outcome.videos
                        val signature = MediaScanSignature.fromVideos(videos)
                        val hiddenFolders = privacyManager.getHiddenFolders()
                        val hiddenFoldersChanged = hiddenFolders != lastHiddenFolders
                        if (!MediaLibraryPolicy.shouldPublishScan(lastScanSignature, signature) && !hiddenFoldersChanged) {
                            _scanProgress.value = null
                            _isLoading.value = false
                            return@collect
                        }
                        lastScanSignature = signature
                        lastHiddenFolders = hiddenFolders
                        val visibleVideos = MediaLibraryPolicy.visibleVideos(videos, hiddenFolders)
                        _videos.value = visibleVideos
                        val groupedFolders = VideoFolderGrouper.groupVideos(visibleVideos)
                        val availableFolderKeys = groupedFolders.map { it.key }.toSet()
                        val prunedPinned = VideoFolderFilterPolicy.prunePinnedKeys(
                            pinnedKeys = _pinnedFolderKeys.value,
                            availableKeys = availableFolderKeys
                        )
                        if (prunedPinned != _pinnedFolderKeys.value) {
                            _pinnedFolderKeys.value = prunedPinned
                            appPrefs.pinnedFolderKeys = prunedPinned
                        }
                        _folders.value = groupedFolders
                        _scannedVideoCount.value = videos.size
                        repository.pruneStaleHistory(videos)
                        _isLoading.value = false
                    }
                }
            }
        }
    }
}

private data class LocalContinuePlaybackCandidate(
    val video: VideoItem,
    val positionMs: Long
)
