package com.example.openvideo.ui.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.ui.home.MediaLibraryPolicy
import com.example.openvideo.ui.home.MediaScanSignature
import com.example.openvideo.ui.privacy.PrivacyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    val isLoading: StateFlow<Boolean> = _isLoading

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
