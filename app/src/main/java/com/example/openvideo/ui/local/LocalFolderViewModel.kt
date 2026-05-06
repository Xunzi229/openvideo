package com.example.openvideo.ui.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalFolderViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _folders = MutableStateFlow<List<VideoFolder>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val folders: StateFlow<List<VideoFolder>> = _folders
    val isLoading: StateFlow<Boolean> = _isLoading

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadVideos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            repository.scanLocalVideos().collect { videos ->
                _videos.value = videos
                _folders.value = VideoFolderGrouper.groupVideos(videos)
                _isLoading.value = false
            }
        }
    }
}
