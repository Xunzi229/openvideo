package com.example.openvideo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortField(val labelRes: Int) {
    DATE(R.string.sort_by_date),
    NAME(R.string.sort_by_name),
    SIZE(R.string.sort_by_size),
    DURATION(R.string.sort_by_duration)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _sortField = MutableStateFlow(SortField.DATE)
    private val _sortAsc = MutableStateFlow(false)

    val sortField: StateFlow<SortField> = _sortField
    val sortAsc: StateFlow<Boolean> = _sortAsc

    val videos: StateFlow<List<VideoItem>> = combine(
        _videos, _searchQuery, _sortField, _sortAsc
    ) { list, query, field, asc ->
        val filtered = if (query.isBlank()) list
        else list.filter { it.title.contains(query, ignoreCase = true) }
        val sorted = when (field) {
            SortField.NAME -> if (asc) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.title.lowercase() }
            SortField.SIZE -> if (asc) filtered.sortedBy { it.size } else filtered.sortedByDescending { it.size }
            SortField.DURATION -> if (asc) filtered.sortedBy { it.duration } else filtered.sortedByDescending { it.duration }
            SortField.DATE -> if (asc) filtered.sortedBy { it.dateAdded } else filtered.sortedByDescending { it.dateAdded }
        }
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadVideos()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun cycleSortField() {
        val fields = SortField.entries
        val next = (fields.indexOf(_sortField.value) + 1) % fields.size
        _sortField.value = fields[next]
    }

    fun toggleSortOrder() {
        _sortAsc.value = !_sortAsc.value
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

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
        }
    }

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            repository.deleteVideo(video)
            loadVideos()
        }
    }
}
