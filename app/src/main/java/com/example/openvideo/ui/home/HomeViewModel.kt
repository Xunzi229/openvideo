package com.example.openvideo.ui.home

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
class HomeViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.scanLocalVideos().collect { list ->
                _videos.value = list
                _isLoading.value = false
            }
        }
    }
}
