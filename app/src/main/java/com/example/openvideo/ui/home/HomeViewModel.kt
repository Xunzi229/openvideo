package com.example.openvideo.ui.home

import android.content.Context
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
import com.example.openvideo.data.scanner.VideoScanOutcome
import com.example.openvideo.ui.local.VideoFolderFilterPolicy
import com.example.openvideo.ui.local.VideoFolderGrouper
import com.example.openvideo.ui.local.VideoFolderSummary
import com.example.openvideo.ui.history.HistoryContinueWatchingLabels
import com.example.openvideo.ui.history.HistoryContinueWatchingPolicy
import com.example.openvideo.ui.playlist.PlaylistEditor
import android.net.Uri
import com.example.openvideo.ui.privacy.PrivacyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val repository: VideoRepository,
    private val appPrefs: AppPrefs,
    private val playlistDao: PlaylistDao,
    private val playlistEditor: PlaylistEditor,
    private val privacyManager: PrivacyManager
) : ViewModel() {

    private val continueWatchingLabels = HistoryContinueWatchingLabels.from(context.applicationContext)

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _scannedVideoCount = MutableStateFlow(0)
    private val _hiddenFilteredCount = MutableStateFlow(0)
    private val _hiddenFolders = MutableStateFlow<List<String>>(emptyList())
    private val _permissionDenied = MutableStateFlow(false)
    private val _scanError = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _advancedFilters = MutableStateFlow(MediaLibraryAdvancedFilters())
    val advancedFilters: StateFlow<MediaLibraryAdvancedFilters> = _advancedFilters
    val hasActiveAdvancedFilters: StateFlow<Boolean> = _advancedFilters
        .map { it.isActive() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    private val _sortField = MutableStateFlow(sortFieldFromPrefs(appPrefs.sortField))
    private val _sortAsc = MutableStateFlow(appPrefs.sortAsc)
    private val _categoryViewModes = MutableStateFlow(loadCategoryViewModes())
    private val _category = MutableStateFlow(HomeCategory.ALL)
    private val _selectedFolderKey = MutableStateFlow<String?>(null)
    private val _pinnedFolderKeys = MutableStateFlow(appPrefs.pinnedFolderKeys)

    val sortField: StateFlow<SortField> = _sortField
    val sortAsc: StateFlow<Boolean> = _sortAsc
    val categoryViewModes: StateFlow<Map<HomeCategory, ViewMode>> = _categoryViewModes
    val viewMode: StateFlow<ViewMode> = combine(_category, _categoryViewModes) { category, modes ->
        modes[category] ?: ViewMode.LIST
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)
    val category: StateFlow<HomeCategory> = _category
    val selectedFolderKey: StateFlow<String?> = _selectedFolderKey
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAll()

    private val allCategoryVideos: Flow<List<VideoItem>> = combine(
        _videos,
        _hiddenFolders
    ) { scanned, _ ->
        scanned
    }

    private val recentCategoryVideos: Flow<List<VideoItem>> = combine(
        _videos,
        repository.getHistory(),
        _hiddenFolders,
        _permissionDenied
    ) { scanned, history, hiddenFolders, permissionDenied ->
        videosFromHistory(scanned, history, hiddenFolders, permissionDenied)
    }

    private val favoriteCategoryVideos: Flow<List<VideoItem>> = combine(
        _videos,
        repository.getFavorites(),
        _hiddenFolders,
        _permissionDenied
    ) { scanned, favorites, hiddenFolders, permissionDenied ->
        videosFromFavorites(scanned, favorites, hiddenFolders, permissionDenied)
    }

    val allVideos: StateFlow<List<VideoItem>> = filteredSortedVideos(allCategoryVideos)
    val recentVideos: StateFlow<List<VideoItem>> = filteredRecentVideos(recentCategoryVideos)
    val favoriteVideos: StateFlow<List<VideoItem>> = filteredSortedVideos(favoriteCategoryVideos)

    private val allVideosForFolderChips = filteredSortedVideosWithoutFolder(allCategoryVideos)
    private val recentVideosForFolderChips = filteredRecentVideosWithoutFolder(recentCategoryVideos)
    private val favoriteVideosForFolderChips = filteredSortedVideosWithoutFolder(favoriteCategoryVideos)

    private val videosForFolderChips: StateFlow<List<VideoItem>> = combine(
        allVideosForFolderChips,
        recentVideosForFolderChips,
        favoriteVideosForFolderChips,
        _category
    ) { all, recent, favorites, category ->
        when (category) {
            HomeCategory.ALL -> all
            HomeCategory.RECENT -> recent
            HomeCategory.FAVORITES -> favorites
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<VideoFolderSummary>> = combine(
        videosForFolderChips,
        _pinnedFolderKeys
    ) { videos, pinnedKeys ->
        VideoFolderFilterPolicy.displayFolders(
            folders = VideoFolderGrouper.groupPaths(videos.map { it.path }),
            pinnedKeys = pinnedKeys
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentContinueWatchingBadges: StateFlow<Map<Long, ContinueWatchingBadge>> = repository.getHistory()
        .map { history ->
            HistoryContinueWatchingPolicy.buildItems(
                history = history,
                labels = continueWatchingLabels,
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
        source,
        _selectedFolderKey,
        _searchQuery,
        _advancedFilters,
        _sortField,
        _sortAsc
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val list = values[0] as List<VideoItem>
        val selectedFolderKey = values[1] as String?
        val query = values[2] as String
        val filters = values[3] as MediaLibraryAdvancedFilters
        val field = values[4] as SortField
        val asc = values[5] as Boolean
        val folderFiltered = MediaLibraryPolicy.visibleVideos(
            videos = list,
            hiddenFolders = emptyList(),
            folderKey = selectedFolderKey
        )
        val filtered = applyLibraryFilters(folderFiltered, query, filters)
        val sorted = when (field) {
            SortField.NAME -> if (asc) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.title.lowercase() }
            SortField.SIZE -> if (asc) filtered.sortedBy { it.size } else filtered.sortedByDescending { it.size }
            SortField.DURATION -> if (asc) filtered.sortedBy { it.duration } else filtered.sortedByDescending { it.duration }
            SortField.DATE -> if (asc) filtered.sortedBy { it.dateAdded } else filtered.sortedByDescending { it.dateAdded }
        }
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filteredRecentVideos(source: Flow<List<VideoItem>>): StateFlow<List<VideoItem>> = combine(
        source,
        _selectedFolderKey,
        _searchQuery,
        _advancedFilters
    ) { list, selectedFolderKey, query, filters ->
        val folderFiltered = MediaLibraryPolicy.visibleVideos(
            videos = list,
            hiddenFolders = emptyList(),
            folderKey = selectedFolderKey
        )
        applyLibraryFilters(folderFiltered, query, filters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filteredSortedVideosWithoutFolder(
        source: Flow<List<VideoItem>>
    ): StateFlow<List<VideoItem>> = combine(
        source,
        _searchQuery,
        _advancedFilters,
        _sortField,
        _sortAsc
    ) { list, query, filters, field, asc ->
        val visible = MediaLibraryPolicy.visibleVideos(
            videos = list,
            hiddenFolders = emptyList(),
            folderKey = null
        )
        val filtered = applyLibraryFilters(visible, query, filters)
        when (field) {
            SortField.NAME -> if (asc) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.title.lowercase() }
            SortField.SIZE -> if (asc) filtered.sortedBy { it.size } else filtered.sortedByDescending { it.size }
            SortField.DURATION -> if (asc) filtered.sortedBy { it.duration } else filtered.sortedByDescending { it.duration }
            SortField.DATE -> if (asc) filtered.sortedBy { it.dateAdded } else filtered.sortedByDescending { it.dateAdded }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filteredRecentVideosWithoutFolder(
        source: Flow<List<VideoItem>>
    ): StateFlow<List<VideoItem>> = combine(
        source,
        _searchQuery,
        _advancedFilters
    ) { list, query, filters ->
        val visible = MediaLibraryPolicy.visibleVideos(
            videos = list,
            hiddenFolders = emptyList(),
            folderKey = null
        )
        applyLibraryFilters(visible, query, filters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applyLibraryFilters(
        videos: List<VideoItem>,
        query: String,
        filters: MediaLibraryAdvancedFilters
    ): List<VideoItem> {
        val nowEpochSec = System.currentTimeMillis() / 1000L
        return videos.filter { video ->
            MediaLibrarySearchPolicy.matchesLibrary(video, query, filters, nowEpochSec)
        }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _scanProgress = MutableStateFlow<MediaLibraryScanProgress?>(null)
    val scanProgress: StateFlow<MediaLibraryScanProgress?> = _scanProgress
    val emptyState: StateFlow<MediaLibraryEmptyState> = combine(
        _isLoading,
        _scannedVideoCount,
        videos,
        _hiddenFilteredCount,
        _permissionDenied,
        _scanError,
        _category,
        _selectedFolderKey,
        _searchQuery,
        _advancedFilters
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val visibleVideos = values[2] as List<VideoItem>
        val selectedFolderKey = values[7] as String?
        val query = values[8] as String
        val filters = values[9] as MediaLibraryAdvancedFilters
        MediaLibraryPolicy.emptyState(
            isLoading = values[0] as Boolean,
            scannedCount = values[1] as Int,
            visibleCount = visibleVideos.size,
            hiddenFilteredCount = values[3] as Int,
            permissionDenied = values[4] as Boolean,
            scanError = values[5] as Boolean,
            category = values[6] as HomeCategory,
            hasActiveUserFilter = selectedFolderKey != null || query.isNotBlank() || filters.isActive()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaLibraryEmptyState.LOADING)

    init {
        loadVideos()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAdvancedFilters(filters: MediaLibraryAdvancedFilters) {
        _advancedFilters.value = filters
    }

    fun clearAdvancedFilters() {
        _advancedFilters.value = MediaLibraryAdvancedFilters()
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

    fun togglePinnedFolder(folderKey: String) {
        val updated = VideoFolderFilterPolicy.togglePinned(_pinnedFolderKeys.value, folderKey)
        _pinnedFolderKeys.value = updated
        appPrefs.pinnedFolderKeys = updated
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
                        val list = outcome.videos
                        val signature = MediaScanSignature.fromVideos(list)
                        val hiddenFolders = privacyManager.getHiddenFolders()
                        val hiddenFoldersChanged = hiddenFolders != lastHiddenFolders
                        if (!MediaLibraryPolicy.shouldPublishScan(lastScanSignature, signature) && !hiddenFoldersChanged) {
                            _scanProgress.value = null
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
                        val availableFolderKeys = VideoFolderGrouper.groupPaths(visibleVideos.map { it.path })
                            .map { it.key }
                            .toSet()
                        val prunedPinned = VideoFolderFilterPolicy.prunePinnedKeys(
                            pinnedKeys = _pinnedFolderKeys.value,
                            availableKeys = availableFolderKeys
                        )
                        if (prunedPinned != _pinnedFolderKeys.value) {
                            _pinnedFolderKeys.value = prunedPinned
                            appPrefs.pinnedFolderKeys = prunedPinned
                        }
                        val validFolderKey = MediaLibraryPolicy.validFolderKey(
                            selectedFolderKey = _selectedFolderKey.value,
                            folderKeys = availableFolderKeys.toList()
                        )
                        if (validFolderKey != _selectedFolderKey.value) {
                            _selectedFolderKey.value = validFolderKey
                        }
                        _videos.value = visibleVideos
                        repository.pruneStaleHistory(list)
                        _isLoading.value = false
                    }
                }
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
        hiddenFolders: List<String>,
        permissionDenied: Boolean
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
                            permissionDenied = permissionDenied,
                            localFileExists = { candidatePath -> File(candidatePath).exists() }
                        )
                    }
            }
    }

    private fun videosFromFavorites(
        scanned: List<VideoItem>,
        favorites: List<FavoriteEntity>,
        hiddenFolders: List<String>,
        permissionDenied: Boolean
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
                            permissionDenied = permissionDenied,
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
