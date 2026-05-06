package com.example.openvideo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.PendingIntent
import com.example.openvideo.R
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.data.local.FavoriteEntity
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.local.PlaylistDao
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.data.scanner.VideoDeleteResult
import com.example.openvideo.ui.playlist.PlaylistEditor
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class SortField(val labelRes: Int) {
    DATE(R.string.sort_by_date),
    NAME(R.string.sort_by_name),
    SIZE(R.string.sort_by_size),
    DURATION(R.string.sort_by_duration)
}

enum class ViewMode { LIST, GRID }

enum class HomeCategory { ALL, RECENT, FAVORITES }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VideoRepository,
    private val appPrefs: AppPrefs,
    private val playlistDao: PlaylistDao,
    private val playlistEditor: PlaylistEditor
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _sortField = MutableStateFlow(sortFieldFromPrefs(appPrefs.sortField))
    private val _sortAsc = MutableStateFlow(appPrefs.sortAsc)
    private val _viewMode = MutableStateFlow(viewModeFromPrefs(appPrefs.viewMode))
    private val _category = MutableStateFlow(HomeCategory.ALL)

    val sortField: StateFlow<SortField> = _sortField
    val sortAsc: StateFlow<Boolean> = _sortAsc
    val viewMode: StateFlow<ViewMode> = _viewMode
    val category: StateFlow<HomeCategory> = _category
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAll()

    private val categoryVideos: Flow<List<VideoItem>> = combine(
        _videos,
        repository.getHistory(),
        repository.getFavorites(),
        _category
    ) { scanned, history, favorites, category ->
        when (category) {
            HomeCategory.ALL -> scanned
            HomeCategory.RECENT -> videosFromHistory(scanned, history)
            HomeCategory.FAVORITES -> videosFromFavorites(scanned, favorites)
        }
    }

    val videos: StateFlow<List<VideoItem>> = combine(
        categoryVideos, _searchQuery, _sortField, _sortAsc
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
        appPrefs.sortField = fields[next].name.lowercase()
    }

    fun toggleSortOrder() {
        _sortAsc.value = !_sortAsc.value
        appPrefs.sortAsc = _sortAsc.value
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        appPrefs.viewMode = mode.name.lowercase()
    }

    fun setCategory(category: HomeCategory) {
        _category.value = category
    }

    private fun sortFieldFromPrefs(key: String): SortField {
        return when (key) {
            "name" -> SortField.NAME
            "size" -> SortField.SIZE
            "duration" -> SortField.DURATION
            else -> SortField.DATE
        }
    }

    private fun viewModeFromPrefs(key: String): ViewMode {
        return when (key) {
            "grid" -> ViewMode.GRID
            else -> ViewMode.LIST
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadVideos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
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

    suspend fun isFavorite(videoId: Long): Boolean = repository.isFavorite(videoId)

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            if (repository.deleteVideo(video)) {
                _videos.value = _videos.value.filter { it.id != video.id }
            }
        }
    }

    fun deleteVideos(videos: List<VideoItem>) {
        viewModelScope.launch {
            val ids = videos
                .filter { repository.deleteVideo(it) }
                .map { it.id }
                .toSet()
            if (ids.isNotEmpty()) {
                _videos.value = _videos.value.filter { it.id !in ids }
            }
        }
    }

    fun deleteVideosWithResult(videos: List<VideoItem>, onResult: (VideoDeleteResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteVideos(videos)
            if (result is VideoDeleteResult.Deleted && result.uris.isNotEmpty()) {
                val deletedUris = result.uris
                _videos.value = _videos.value.filter { it.uri !in deletedUris }
            }
            onResult(result)
        }
    }

    fun createDeleteRequest(videos: List<VideoItem>): PendingIntent? {
        return repository.createDeleteRequest(videos)
    }

    fun addToPlaylist(playlistId: Long, video: VideoItem) {
        viewModelScope.launch {
            playlistEditor.addToPlaylist(playlistId, video)
        }
    }

    fun createPlaylistWithVideo(name: String, video: VideoItem) {
        viewModelScope.launch {
            playlistEditor.createPlaylistWithVideo(name, video)
        }
    }

    private fun videosFromHistory(
        scanned: List<VideoItem>,
        history: List<HistoryEntity>
    ): List<VideoItem> {
        val scannedById = scanned.associateBy { it.id }
        return history
            .sortedByDescending { it.timestamp }
            .map { item ->
                scannedById[item.videoId]?.copy(dateAdded = item.timestamp / 1000)
                    ?: item.toVideoItem()
            }
    }

    private fun videosFromFavorites(
        scanned: List<VideoItem>,
        favorites: List<FavoriteEntity>
    ): List<VideoItem> {
        val scannedById = scanned.associateBy { it.id }
        return favorites
            .sortedByDescending { it.timestamp }
            .map { item ->
                scannedById[item.videoId]?.copy(dateAdded = item.timestamp / 1000)
                    ?: item.toVideoItem()
            }
    }

    private fun HistoryEntity.toVideoItem(): VideoItem = VideoItem(
        id = videoId,
        title = title,
        path = path,
        uri = storedUri(path),
        duration = duration,
        size = 0,
        width = 0,
        height = 0,
        dateAdded = timestamp / 1000,
        thumbnailUri = null
    )

    private fun FavoriteEntity.toVideoItem(): VideoItem = VideoItem(
        id = videoId,
        title = title,
        path = path,
        uri = storedUri(path),
        duration = duration,
        size = 0,
        width = 0,
        height = 0,
        dateAdded = timestamp / 1000,
        thumbnailUri = null
    )

    private fun storedUri(path: String): Uri {
        return when {
            path.startsWith("content://") || path.startsWith("file://") -> Uri.parse(path)
            else -> Uri.fromFile(File(path))
        }
    }
}
