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
import com.example.openvideo.ui.local.VideoFolderGrouper
import com.example.openvideo.ui.local.VideoFolderSummary
import com.example.openvideo.ui.history.HistoryContinueWatchingPolicy
import com.example.openvideo.ui.playlist.PlaylistEditor
import android.net.Uri
import com.example.openvideo.ui.privacy.PrivacyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val playlistEditor: PlaylistEditor,
    private val privacyManager: PrivacyManager
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _scannedVideoCount = MutableStateFlow(0)
    private val _hiddenFilteredCount = MutableStateFlow(0)
    private val _hiddenFolders = MutableStateFlow<List<String>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _sortField = MutableStateFlow(sortFieldFromPrefs(appPrefs.sortField))
    private val _sortAsc = MutableStateFlow(appPrefs.sortAsc)
    private val _categoryViewModes = MutableStateFlow(loadCategoryViewModes())
    private val _category = MutableStateFlow(HomeCategory.ALL)
    private val _selectedFolderKey = MutableStateFlow<String?>(null)

    val sortField: StateFlow<SortField> = _sortField
    val sortAsc: StateFlow<Boolean> = _sortAsc
    val categoryViewModes: StateFlow<Map<HomeCategory, ViewMode>> = _categoryViewModes
    val viewMode: StateFlow<ViewMode> = combine(_category, _categoryViewModes) { category, modes ->
        modes[category] ?: ViewMode.LIST
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)
    val category: StateFlow<HomeCategory> = _category
    val selectedFolderKey: StateFlow<String?> = _selectedFolderKey
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAll()

    val folders: StateFlow<List<VideoFolderSummary>> = _videos
        .map { videos -> VideoFolderGrouper.groupPaths(videos.map { it.path }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allCategoryVideos: Flow<List<VideoItem>> = combine(
        _videos,
        _hiddenFolders
    ) { scanned, _ ->
        scanned
    }

    private val recentCategoryVideos: Flow<List<VideoItem>> = combine(
        _videos,
        repository.getHistory(),
        _hiddenFolders
    ) { scanned, history, hiddenFolders ->
        videosFromHistory(scanned, history, hiddenFolders)
    }

    private val favoriteCategoryVideos: Flow<List<VideoItem>> = combine(
        _videos,
        repository.getFavorites(),
        _hiddenFolders
    ) { scanned, favorites, hiddenFolders ->
        videosFromFavorites(scanned, favorites, hiddenFolders)
    }

    val allVideos: StateFlow<List<VideoItem>> = filteredSortedVideos(allCategoryVideos)
    val recentVideos: StateFlow<List<VideoItem>> = filteredRecentVideos(recentCategoryVideos)
    val favoriteVideos: StateFlow<List<VideoItem>> = filteredSortedVideos(favoriteCategoryVideos)
    val recentContinueWatchingBadges: StateFlow<Map<Long, ContinueWatchingBadge>> = repository.getHistory()
        .map { history ->
            HistoryContinueWatchingPolicy.buildItems(
                history = history,
                nowMs = System.currentTimeMillis(),
                localFileExists = { candidatePath -> File(candidatePath).exists() }
            ).associate { item ->
                item.entity.videoId to ContinueWatchingBadge(
                    watchedTimeLabel = item.watchedTimeLabel,
                    progressLabel = item.progressLabel,
                    isAvailable = item.isAvailable
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val videos: StateFlow<List<VideoItem>> = combine(
        allVideos, recentVideos, favoriteVideos, _category
    ) { all, recent, favorites, category ->
        when (category) {
            HomeCategory.ALL -> all
            HomeCategory.RECENT -> recent
            HomeCategory.FAVORITES -> favorites
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filteredSortedVideos(source: Flow<List<VideoItem>>): StateFlow<List<VideoItem>> = combine(
        source, _selectedFolderKey, _searchQuery, _sortField, _sortAsc
    ) { list, selectedFolderKey, query, field, asc ->
        val folderFiltered = MediaLibraryPolicy.visibleVideos(
            videos = list,
            hiddenFolders = emptyList(),
            folderKey = selectedFolderKey
        )
        val filtered = if (query.isBlank()) folderFiltered
        else folderFiltered.filter { it.title.contains(query, ignoreCase = true) }
        val sorted = when (field) {
            SortField.NAME -> if (asc) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.title.lowercase() }
            SortField.SIZE -> if (asc) filtered.sortedBy { it.size } else filtered.sortedByDescending { it.size }
            SortField.DURATION -> if (asc) filtered.sortedBy { it.duration } else filtered.sortedByDescending { it.duration }
            SortField.DATE -> if (asc) filtered.sortedBy { it.dateAdded } else filtered.sortedByDescending { it.dateAdded }
        }
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filteredRecentVideos(source: Flow<List<VideoItem>>): StateFlow<List<VideoItem>> = combine(
        source, _selectedFolderKey, _searchQuery
    ) { list, selectedFolderKey, query ->
        val folderFiltered = MediaLibraryPolicy.visibleVideos(
            videos = list,
            hiddenFolders = emptyList(),
            folderKey = selectedFolderKey
        )
        if (query.isBlank()) folderFiltered
        else folderFiltered.filter { it.title.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val emptyState: StateFlow<MediaLibraryEmptyState> = combine(
        _isLoading,
        _scannedVideoCount,
        videos,
        _hiddenFilteredCount
    ) { isLoading, scannedCount, visibleVideos, hiddenFilteredCount ->
        MediaLibraryPolicy.emptyState(
            isLoading = isLoading,
            scannedCount = scannedCount,
            visibleCount = visibleVideos.size,
            hiddenFilteredCount = hiddenFilteredCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaLibraryEmptyState.LOADING)

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
        val currentCategory = _category.value
        _categoryViewModes.value = _categoryViewModes.value + (currentCategory to mode)
        saveViewModeForCategory(currentCategory, mode)
    }

    fun setCategory(category: HomeCategory) {
        _category.value = category
    }

    fun setFolderFilter(folderKey: String?) {
        _selectedFolderKey.value = folderKey
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

    private fun loadCategoryViewModes(): Map<HomeCategory, ViewMode> {
        return mapOf(
            HomeCategory.ALL to viewModeFromPrefs(appPrefs.homeAllViewMode),
            HomeCategory.RECENT to viewModeFromPrefs(appPrefs.homeRecentViewMode),
            HomeCategory.FAVORITES to viewModeFromPrefs(appPrefs.homeFavoriteViewMode)
        )
    }

    private fun saveViewModeForCategory(category: HomeCategory, mode: ViewMode) {
        val key = mode.name.lowercase()
        when (category) {
            HomeCategory.ALL -> appPrefs.homeAllViewMode = key
            HomeCategory.RECENT -> appPrefs.homeRecentViewMode = key
            HomeCategory.FAVORITES -> appPrefs.homeFavoriteViewMode = key
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null
    private var lastScanSignature: MediaScanSignature? = null
    private var lastHiddenFolders: List<String> = emptyList()

    fun loadVideos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            repository.scanLocalVideos().collect { list ->
                val signature = MediaScanSignature.fromVideos(list)
                val hiddenFolders = privacyManager.getHiddenFolders()
                val hiddenFoldersChanged = hiddenFolders != lastHiddenFolders
                if (!MediaLibraryPolicy.shouldPublishScan(lastScanSignature, signature) && !hiddenFoldersChanged) {
                    _isLoading.value = false
                    return@collect
                }
                lastScanSignature = signature
                lastHiddenFolders = hiddenFolders
                _hiddenFolders.value = hiddenFolders
                _scannedVideoCount.value = list.size
                _hiddenFilteredCount.value = MediaLibraryPolicy.hiddenFilteredCount(
                    list.map { it.path },
                    hiddenFolders
                )
                val visibleVideos = MediaLibraryPolicy.visibleVideos(
                    videos = list,
                    hiddenFolders = hiddenFolders
                )
                val validFolderKey = MediaLibraryPolicy.validFolderKey(
                    selectedFolderKey = _selectedFolderKey.value,
                    folderKeys = VideoFolderGrouper.groupPaths(visibleVideos.map { it.path }).map { it.key }
                )
                if (validFolderKey != _selectedFolderKey.value) {
                    _selectedFolderKey.value = validFolderKey
                }
                _videos.value = visibleVideos
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
        history: List<HistoryEntity>,
        hiddenFolders: List<String>
    ): List<VideoItem> {
        val scannedById = scanned.associateBy { it.id }
        return history
            .sortedByDescending { it.timestamp }
            .mapNotNull { item ->
                scannedById[item.videoId]?.copy(dateAdded = item.timestamp / 1000)
                    ?: item.toVideoItem().takeIf { video ->
                        MediaLibraryPolicy.shouldExposeStoredFallback(
                            path = video.path,
                            hiddenFolders = hiddenFolders,
                            localFileExists = { candidatePath -> File(candidatePath).exists() }
                        )
                    }
            }
    }

    private fun videosFromFavorites(
        scanned: List<VideoItem>,
        favorites: List<FavoriteEntity>,
        hiddenFolders: List<String>
    ): List<VideoItem> {
        val scannedById = scanned.associateBy { it.id }
        return favorites
            .sortedByDescending { it.timestamp }
            .mapNotNull { item ->
                scannedById[item.videoId]?.copy(dateAdded = item.timestamp / 1000)
                    ?: item.toVideoItem().takeIf { video ->
                        MediaLibraryPolicy.shouldExposeStoredFallback(
                            path = video.path,
                            hiddenFolders = hiddenFolders,
                            localFileExists = { candidatePath -> File(candidatePath).exists() }
                        )
                    }
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
