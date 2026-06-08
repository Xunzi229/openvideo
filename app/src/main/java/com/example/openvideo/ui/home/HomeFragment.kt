package com.example.openvideo.ui.home

import android.Manifest
import android.content.res.ColorStateList
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.core.metadata.MediaSmartListType
import com.example.openvideo.core.ui.ScreenBreakpoint
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.scanner.VideoDeleteResult
import com.example.openvideo.ui.MainActivity
import com.example.openvideo.ui.local.VideoFolderSummary
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.PlayerActivityIntents
import com.example.openvideo.ui.player.NetworkOpenUrlDialog
import com.example.openvideo.ui.player.putSessionQueue
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private val adapters = mutableMapOf<HomeCategory, VideoGridAdapter>()
    private val recyclerViews = mutableMapOf<HomeCategory, RecyclerView>()
    private lateinit var emptyView: TextView
    private lateinit var scanLoadingContainer: View
    private lateinit var scanProgressBar: ProgressBar
    private lateinit var scanProgressLabel: TextView
    private lateinit var searchView: EditText
    private lateinit var sortRow: View
    private lateinit var sortLabel: TextView
    private lateinit var btnSortOrder: ImageButton
    private lateinit var btnList: ImageButton
    private lateinit var btnGrid: ImageButton
    private lateinit var btnLibraryFilter: ImageButton
    private lateinit var chipAll: Chip
    private lateinit var chipRecent: Chip
    private lateinit var chipFavorite: Chip
    private lateinit var smartFilterScroll: View
    private lateinit var smartFilterGroup: ChipGroup
    private lateinit var filterScroll: View
    private lateinit var folderGroup: ChipGroup
    private lateinit var folderPinHint: TextView
    private var actionMode: ActionMode? = null
    private var pendingDeleteVideos: List<VideoItem> = emptyList()
    private var pendingJumpToTopCategory: HomeCategory? = null
    private var latestEmptyState: MediaLibraryEmptyState = MediaLibraryEmptyState.LOADING
    private var latestScanProgress: MediaLibraryScanProgress? = null
    private var activeCategory = HomeCategory.ALL
    private val categoryLists = mutableMapOf<HomeCategory, List<VideoItem>>()
    private val lastFocusedHomeVideoIds = mutableMapOf<HomeCategory, Long>()
    private val pendingHomeVideoFocusRestoreIds = mutableMapOf<HomeCategory, Long>()
    private var currentSmartListSections: List<HomeSmartListSection> = emptyList()
    private var currentSelectedSmartListType: MediaSmartListType? = null
    private var currentFolders: List<VideoFolderSummary> = emptyList()
    private var currentSelectedFolderKey: String? = null
    private var filterPopover: VideoLibraryFilterPopover? = null

    private val activeAdapter: VideoGridAdapter
        get() = adapters.getValue(activeCategory)

    private val activeRecyclerView: RecyclerView
        get() = recyclerViews.getValue(activeCategory)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.any { it.value }) viewModel.loadVideos()
    }

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && pendingDeleteVideos.isNotEmpty()) {
            completePendingDeleteAfterSystemGrant()
        }
        pendingDeleteVideos = emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyView = view.findViewById(R.id.tv_empty)
        scanLoadingContainer = view.findViewById(R.id.scan_loading_container)
        scanProgressBar = view.findViewById(R.id.scan_progress_bar)
        scanProgressLabel = view.findViewById(R.id.tv_scan_progress)
        searchView = view.findViewById(R.id.search_view)
        searchView.visibility = View.VISIBLE

        sortRow = view.findViewById(R.id.sort_row)
        sortLabel = view.findViewById(R.id.tv_sort_label)
        sortLabel.setOnClickListener { changeSortFieldAndScrollToTop() }
        btnSortOrder = view.findViewById(R.id.btn_sort_order)
        btnSortOrder.setOnClickListener { toggleSortOrderAndScrollToTop() }

        btnList = view.findViewById(R.id.btn_list_view)
        btnGrid = view.findViewById(R.id.btn_grid_view)
        btnLibraryFilter = view.findViewById(R.id.btn_library_filter)
        btnList.setOnClickListener { viewModel.setViewMode(ViewMode.LIST) }
        btnGrid.setOnClickListener { viewModel.setViewMode(ViewMode.GRID) }
        btnLibraryFilter.setOnClickListener { toggleAdvancedFilterPopover() }
        chipAll = view.findViewById(R.id.chip_all)
        chipRecent = view.findViewById(R.id.chip_recent)
        chipFavorite = view.findViewById(R.id.chip_favorite)
        smartFilterScroll = view.findViewById(R.id.smart_filter_scroll)
        smartFilterGroup = view.findViewById(R.id.smart_filter_group)
        filterScroll = view.findViewById(R.id.filter_scroll)
        folderGroup = view.findViewById(R.id.folder_group)
        folderPinHint = view.findViewById(R.id.folder_pin_hint)
        chipAll.setOnClickListener { switchCategory(HomeCategory.ALL) }
        chipRecent.setOnClickListener { switchCategory(HomeCategory.RECENT) }
        chipFavorite.setOnClickListener { switchCategory(HomeCategory.FAVORITES) }
        configureHomeFocusOrder(view)

        initCategoryList(HomeCategory.ALL, view.findViewById(R.id.recycler_all_videos))
        initCategoryList(HomeCategory.RECENT, view.findViewById(R.id.recycler_recent_videos))
        initCategoryList(HomeCategory.FAVORITES, view.findViewById(R.id.recycler_favorite_videos))
        showCategoryPage(HomeCategory.ALL)

        view.findViewById<ImageButton>(R.id.btn_refresh).setOnClickListener {
            checkPermissionAndLoad()
        }
        view.findViewById<ImageButton>(R.id.btn_open_url).setOnClickListener {
            showOpenUrlDialog()
        }

        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateSearchQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        observeVideos()
        checkPermissionAndLoad()
    }

    private fun initCategoryList(category: HomeCategory, recyclerView: RecyclerView) {
        val adapter = VideoGridAdapter(
            onClick = { video -> openPlayer(video) },
            onMoreOptions = { video, _ -> showVideoOptions(video) },
            onSelectionChanged = { selected ->
                if (activeAdapter.isMultiSelectMode) {
                    actionMode?.title = getString(R.string.multi_select_count, selected.size)
                }
            },
            onLongClick = { video -> startMultiSelectMode(category) },
            onFocusChanged = { video -> lastFocusedHomeVideoIds[category] = video.id }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        adapters[category] = adapter
        recyclerViews[category] = recyclerView
    }

    private fun observeVideos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allVideos.collect { list ->
                        submitCategoryList(HomeCategory.ALL, list)
                    }
                }
                launch {
                    viewModel.recentVideos.collect { list ->
                        submitCategoryList(HomeCategory.RECENT, list)
                    }
                }
                launch {
                    viewModel.recentContinueWatchingBadges.collect { badges ->
                        adapters.getValue(HomeCategory.RECENT).continueWatchingBadges = badges
                    }
                }
                launch {
                    viewModel.favoriteVideos.collect { list ->
                        submitCategoryList(HomeCategory.FAVORITES, list)
                    }
                }
                launch {
                    viewModel.smartLists.collect { sections ->
                        currentSmartListSections = sections
                        bindSmartListChips()
                    }
                }
                launch {
                    viewModel.selectedSmartListType.collect { type ->
                        currentSelectedSmartListType = type
                        bindSmartListChips()
                    }
                }
                launch {
                    viewModel.sortField.collect { field ->
                        sortLabel.text = getString(field.labelRes)
                    }
                }
                launch {
                    viewModel.sortAsc.collect { asc ->
                        btnSortOrder.setImageResource(if (asc) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
                    }
                }
                launch {
                    viewModel.categoryViewModes.collect { modes ->
                        applyCategoryViewModes(modes)
                        updateViewModeButtons(modes[activeCategory] ?: ViewMode.LIST)
                    }
                }
                launch {
                    viewModel.category.collect { category ->
                        activeCategory = category
                        updateCategoryChips(category)
                        updateSortControlsVisibility(category)
                        updateViewModeButtons(viewModel.categoryViewModes.value[category] ?: ViewMode.LIST)
                        showCategoryPage(category)
                        bindSmartListChips()
                        updateActiveEmptyState()
                    }
                }
                launch {
                    viewModel.folders.collect { folders ->
                        currentFolders = folders
                        bindFolderChips()
                    }
                }
                launch {
                    viewModel.selectedFolderKey.collect { folderKey ->
                        currentSelectedFolderKey = folderKey
                        bindFolderChips()
                    }
                }
                launch {
                    viewModel.hasActiveAdvancedFilters.collect { active ->
                        bindFilterButtonTint(active || filterPopover?.isShowing() == true)
                    }
                }
                launch {
                    combine(viewModel.emptyState, viewModel.scanProgress) { state, progress ->
                        state to progress
                    }.collect { (state, progress) ->
                        latestEmptyState = state
                        latestScanProgress = progress
                        emptyView.text = when (state) {
                            MediaLibraryEmptyState.PERMISSION_DENIED -> getString(R.string.media_library_permission_denied)
                            MediaLibraryEmptyState.SCAN_ERROR -> getString(R.string.media_library_scan_error)
                            MediaLibraryEmptyState.NO_MEDIA -> getString(R.string.no_videos)
                            MediaLibraryEmptyState.NO_FAVORITES -> getString(R.string.media_library_empty_favorites)
                            MediaLibraryEmptyState.FILTERED_BY_PRIVACY -> getString(R.string.media_library_empty_privacy)
                            MediaLibraryEmptyState.FILTERED_BY_QUERY_OR_FOLDER -> getString(R.string.media_library_empty_filtered)
                            MediaLibraryEmptyState.LOADING,
                            MediaLibraryEmptyState.NONE -> getString(R.string.no_videos)
                        }
                        bindEmptyUi(state, progress)
                    }
                }
            }
        }
    }

    private fun checkPermissionAndLoad() {
        if (hasVideoReadPermission()) {
            viewModel.loadVideos()
        } else {
            permissionLauncher.launch(videoReadPermissions())
        }
    }

    private fun showOpenUrlDialog() {
        NetworkOpenUrlDialog.show(requireContext()) { normalizedUrl, title ->
            viewModel.recordNetworkRecentUrl(normalizedUrl, title)
        }
    }

    private fun switchCategory(category: HomeCategory) {
        actionMode?.finish()
        viewModel.setCategory(category)
    }

    private fun changeSortFieldAndScrollToTop() {
        viewModel.cycleSortField()
        requestVideoListJumpToTop()
    }

    private fun toggleSortOrderAndScrollToTop() {
        viewModel.toggleSortOrder()
        requestVideoListJumpToTop()
    }

    private fun requestVideoListJumpToTop() {
        pendingJumpToTopCategory = activeCategory
    }

    private fun jumpVideoListToTopIfNeeded(category: HomeCategory) {
        if (pendingJumpToTopCategory != category) return
        pendingJumpToTopCategory = null
        val recyclerView = recyclerViews[category] ?: return
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is LinearLayoutManager) {
            layoutManager.scrollToPositionWithOffset(0, 0)
        } else {
            recyclerView.scrollToPosition(0)
        }
    }

    private fun submitCategoryList(category: HomeCategory, list: List<VideoItem>) {
        categoryLists[category] = list
        adapters.getValue(category).submitList(list) {
            jumpVideoListToTopIfNeeded(category)
            restoreHomeVideoFocusIfNeeded(category, list)
        }
        updateCategoryChips(activeCategory)
        if (category == activeCategory) updateActiveEmptyState()
    }

    private fun restoreHomeVideoFocusIfNeeded(category: HomeCategory, list: List<VideoItem>) {
        val videoId = pendingHomeVideoFocusRestoreIds[category] ?: return
        val position = list.indexOfFirst { it.id == videoId }
        if (position == -1) return
        pendingHomeVideoFocusRestoreIds.remove(category)
        val recyclerView = recyclerViews[category] ?: return
        recyclerView.post {
            recyclerView.scrollToPosition(position)
            recyclerView.post {
                recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
            }
        }
    }

    private fun showCategoryPage(category: HomeCategory) {
        recyclerViews.forEach { (pageCategory, recyclerView) ->
            recyclerView.visibility = if (pageCategory == category) View.VISIBLE else View.GONE
        }
        refreshHomeContentFocusOrder(category)
    }

    private fun configureHomeFocusOrder(view: View) {
        view.findViewById<View>(R.id.btn_open_url).nextFocusRightId = R.id.btn_refresh
        view.findViewById<View>(R.id.btn_open_url).nextFocusDownId = R.id.search_view
        view.findViewById<View>(R.id.btn_refresh).nextFocusLeftId = R.id.btn_open_url
        view.findViewById<View>(R.id.btn_refresh).nextFocusDownId = R.id.search_view

        searchView.nextFocusUpId = R.id.btn_open_url
        searchView.nextFocusDownId = R.id.chip_all

        chipAll.nextFocusUpId = R.id.search_view
        chipAll.nextFocusRightId = R.id.chip_recent
        chipAll.nextFocusDownId = R.id.tv_sort_label
        chipRecent.nextFocusUpId = R.id.search_view
        chipRecent.nextFocusLeftId = R.id.chip_all
        chipRecent.nextFocusRightId = R.id.chip_favorite
        chipRecent.nextFocusDownId = R.id.tv_sort_label
        chipFavorite.nextFocusUpId = R.id.search_view
        chipFavorite.nextFocusLeftId = R.id.chip_recent
        chipFavorite.nextFocusDownId = R.id.btn_library_filter

        sortLabel.isClickable = true
        sortLabel.isFocusable = true
        sortLabel.nextFocusUpId = R.id.chip_all
        btnSortOrder.nextFocusUpId = R.id.chip_all
        btnSortOrder.nextFocusLeftId = R.id.tv_sort_label
        btnSortOrder.nextFocusRightId = R.id.btn_library_filter
        btnLibraryFilter.nextFocusUpId = R.id.chip_favorite
        btnLibraryFilter.nextFocusLeftId = R.id.btn_sort_order
        btnLibraryFilter.nextFocusRightId = R.id.btn_list_view
        btnList.nextFocusUpId = R.id.chip_favorite
        btnList.nextFocusLeftId = R.id.btn_library_filter
        btnList.nextFocusRightId = R.id.btn_grid_view
        btnGrid.nextFocusUpId = R.id.chip_favorite
        btnGrid.nextFocusLeftId = R.id.btn_list_view
        emptyView.isFocusable = true
        emptyView.nextFocusUpId = R.id.btn_refresh
    }

    private fun refreshHomeContentFocusOrder(category: HomeCategory = activeCategory) {
        val recyclerId = activeRecyclerViewId(category)
        val contentFocusTargetId = if (categoryLists[category].orEmpty().isEmpty()) R.id.btn_refresh else recyclerId
        listOf(sortLabel, btnSortOrder, btnLibraryFilter, btnList, btnGrid).forEach { control ->
            control.nextFocusDownId = contentFocusTargetId
        }
        recyclerViews.values.forEach { recyclerView ->
            recyclerView.isFocusable = true
            recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            recyclerView.nextFocusUpId = R.id.btn_list_view
        }
        refreshDynamicFilterChipFocusOrder()
    }

    private fun activeRecyclerViewId(category: HomeCategory): Int = when (category) {
        HomeCategory.ALL -> R.id.recycler_all_videos
        HomeCategory.RECENT -> R.id.recycler_recent_videos
        HomeCategory.FAVORITES -> R.id.recycler_favorite_videos
    }

    private fun updateSortControlsVisibility(category: HomeCategory) {
        val hideSortControls = category == HomeCategory.RECENT
        sortRow.visibility = View.VISIBLE
        sortLabel.visibility = if (hideSortControls) View.GONE else View.VISIBLE
        btnSortOrder.visibility = if (hideSortControls) View.GONE else View.VISIBLE
    }

    private fun applyCategoryViewModes(modes: Map<HomeCategory, ViewMode>) {
        HomeCategory.entries.forEach { category ->
            applyViewMode(category, modes[category] ?: ViewMode.LIST)
        }
    }

    private fun applyViewMode(category: HomeCategory, mode: ViewMode) {
        adapters[category]?.viewMode = mode
        val recyclerView = recyclerViews[category] ?: return
        val spanCount = HomeAdaptiveLayoutPolicy.spanCount(
            viewMode = mode,
            breakpoint = currentBreakpoint()
        )
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {
            layoutManager.spanCount = spanCount
        } else {
            recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        }
    }

    private fun currentBreakpoint(): ScreenBreakpoint =
        (activity as? MainActivity)?.breakpoint ?: ScreenBreakpoint.COMPACT

    private fun updateActiveEmptyState() {
        val list = categoryLists[activeCategory].orEmpty()
        val isEmpty = list.isEmpty()
        refreshHomeContentFocusOrder(activeCategory)
        activeRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (!isEmpty) {
            scanLoadingContainer.visibility = View.GONE
            emptyView.visibility = View.GONE
            return
        }
        bindEmptyUi(latestEmptyState, latestScanProgress)
    }

    private fun bindEmptyUi(
        state: MediaLibraryEmptyState,
        progress: MediaLibraryScanProgress?
    ) {
        val isEmpty = categoryLists[activeCategory].orEmpty().isEmpty()
        MediaLibraryScanLoadingUi.bind(
            context = requireContext(),
            loadingContainer = scanLoadingContainer,
            progressBar = scanProgressBar,
            progressLabel = scanProgressLabel,
            emptyLabel = emptyView,
            emptyState = state,
            scanProgress = progress,
            isContentEmpty = isEmpty
        )
    }

    private fun hasVideoReadPermission(): Boolean {
        return videoReadPermissions().any { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) ==
                PermissionChecker.PERMISSION_GRANTED
        }
    }

    private fun videoReadPermissions(): Array<String> =
        MediaLibraryPermissionPolicy.requiredPermissions()

    override fun onResume() {
        super.onResume()
        checkPermissionAndLoad()
    }

    private fun openPlayer(video: VideoItem) {
        val category = activeCategory
        lastFocusedHomeVideoIds[category] = video.id
        lastFocusedHomeVideoIds[category]?.let { pendingHomeVideoFocusRestoreIds[category] = it }
        val queue = categoryLists[activeCategory].orEmpty().ifEmpty { listOf(video) }
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putSessionQueue(queue)
            putExtra("video_uri", video.uri.toString())
            putExtra("video_title", video.title)
            putExtra("video_id", video.id)
            putExtra("video_path", video.path)
            putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, video.width)
            putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, video.height)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        actionMode?.finish()
        recyclerViews.values.forEach { recyclerView ->
            recyclerView.adapter = null
        }
        adapters.clear()
        recyclerViews.clear()
        categoryLists.clear()
        super.onDestroyView()
    }

    private fun showVideoOptions(video: VideoItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            VideoOptionsSheet(
                context = requireContext(),
                video = video,
                isFavorite = viewModel.isFavorite(video.id),
                onPlay = { openPlayer(video) },
                onFavorite = { viewModel.toggleFavorite(video) },
                onAddToPlaylist = { showAddToPlaylistDialog(video) },
                onDelete = { confirmDelete(video) }
            ).show()
        }
    }

    private fun showAddToPlaylistDialog(video: VideoItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val playlists = viewModel.playlists.first()
            if (playlists.isEmpty()) {
                showCreatePlaylistForVideoDialog(video)
            } else {
                showPlaylistPicker(video, playlists)
            }
        }
    }

    private fun showPlaylistPicker(video: VideoItem, playlists: List<PlaylistEntity>) {
        val names = playlists.map { it.name }
            .plus(getString(R.string.playlist_create_and_add))
            .toTypedArray()
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_add_to_title)
            .setItems(names) { _, which ->
                if (which == playlists.size) {
                    showCreatePlaylistForVideoDialog(video)
                } else {
                    viewModel.addToPlaylist(playlists[which].id, video)
                }
            }
            .show()
        dialog.listView?.post {
            dialog.listView?.requestFocus()
        }
    }

    private fun showCreatePlaylistForVideoDialog(video: VideoItem) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.playlist_hint_name)
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_create_title)
            .setMessage(getString(R.string.playlist_add_empty_message, video.title))
            .setView(input)
            .setPositiveButton(R.string.action_create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.createPlaylistWithVideo(name, video)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
        input.post {
            input.requestFocus()
        }
    }

    private fun updateViewModeButtons(mode: ViewMode) {
        val activeTint = ContextCompat.getColor(requireContext(), R.color.ov_text_primary)
        val inactiveTint = ContextCompat.getColor(requireContext(), R.color.ov_text_secondary)

        if (mode == ViewMode.LIST) {
            btnList.setBackgroundResource(R.drawable.bg_icon_button_selected)
            btnGrid.setBackgroundResource(R.drawable.bg_icon_button_unselected)
            btnList.setColorFilter(activeTint)
            btnGrid.setColorFilter(inactiveTint)
        } else {
            btnList.setBackgroundResource(R.drawable.bg_icon_button_unselected)
            btnGrid.setBackgroundResource(R.drawable.bg_icon_button_selected)
            btnList.setColorFilter(inactiveTint)
            btnGrid.setColorFilter(activeTint)
        }
    }

    private fun updateCategoryChips(category: HomeCategory) {
        bindCategoryChip(
            chip = chipAll,
            selected = category == HomeCategory.ALL,
            labelRes = R.string.home_filter_all,
            count = categoryLists[HomeCategory.ALL].orEmpty().size
        )
        bindCategoryChip(
            chip = chipRecent,
            selected = category == HomeCategory.RECENT,
            labelRes = R.string.home_filter_recent,
            count = categoryLists[HomeCategory.RECENT].orEmpty().size
        )
        bindCategoryChip(
            chip = chipFavorite,
            selected = category == HomeCategory.FAVORITES,
            labelRes = R.string.home_filter_favorite,
            count = categoryLists[HomeCategory.FAVORITES].orEmpty().size
        )
    }

    private fun bindCategoryChip(
        chip: Chip,
        selected: Boolean,
        labelRes: Int,
        count: Int
    ) {
        val background = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.ov_accent_blue else R.color.ov_bg_elevated
        )
        val stroke = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.ov_accent_blue else R.color.ov_divider
        )
        val text = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.ov_text_primary else R.color.ov_text_secondary
        )
        chip.isChecked = selected
        chip.chipBackgroundColor = ColorStateList.valueOf(background)
        chip.chipStrokeColor = ColorStateList.valueOf(stroke)
        chip.setTextColor(text)
        chip.text = getString(R.string.home_filter_count, getString(labelRes), count)
    }

    private fun bindFolderChips() {
        filterScroll.visibility = if (currentFolders.size > 1) View.VISIBLE else View.GONE
        folderPinHint.visibility = if (currentFolders.size > 1) View.VISIBLE else View.GONE
        folderGroup.removeAllViews()
        folderGroup.addView(createFolderChip(
            text = getString(R.string.home_filter_all_folders),
            checked = currentSelectedFolderKey == null,
            onClick = { viewModel.setFolderFilter(null) }
        ))
        currentFolders.forEach { folder ->
            folderGroup.addView(createFolderChip(
                text = folderChipLabel(folder),
                checked = currentSelectedFolderKey == folder.key,
                onClick = { viewModel.setFolderFilter(folder.key) },
                onLongClick = { viewModel.togglePinnedFolder(folder.key) }
            ))
        }
        refreshDynamicFilterChipFocusOrder()
    }

    private fun bindSmartListChips() {
        val shouldShow = activeCategory == HomeCategory.ALL && currentSmartListSections.isNotEmpty()
        smartFilterScroll.visibility = if (shouldShow) View.VISIBLE else View.GONE
        smartFilterGroup.removeAllViews()
        if (!shouldShow) {
            refreshDynamicFilterChipFocusOrder()
            return
        }

        smartFilterGroup.addView(createSmartListChip(
            text = getString(R.string.home_smart_all),
            checked = currentSelectedSmartListType == null,
            onClick = { viewModel.setSmartListFilter(null) }
        ))
        currentSmartListSections.forEach { section ->
            smartFilterGroup.addView(createSmartListChip(
                text = getString(
                    R.string.home_smart_count,
                    getString(HomeSmartListLabels.labelRes(section.type)),
                    section.videos.size
                ),
                checked = currentSelectedSmartListType == section.type,
                onClick = { viewModel.setSmartListFilter(section.type) }
            ))
        }
        refreshDynamicFilterChipFocusOrder()
    }

    private fun refreshDynamicFilterChipFocusOrder() {
        val smartChips = dynamicChipChildren(smartFilterGroup)
        val folderChips = dynamicChipChildren(folderGroup)
        val smartVisible = smartFilterScroll.visibility == View.VISIBLE && smartChips.isNotEmpty()
        val folderVisible = filterScroll.visibility == View.VISIBLE && folderChips.isNotEmpty()
        val firstDynamicChipId = when {
            smartVisible -> smartChips.first().id
            folderVisible -> folderChips.first().id
            else -> R.id.tv_sort_label
        }

        chipAll.nextFocusDownId = firstDynamicChipId
        chipRecent.nextFocusDownId = firstDynamicChipId
        chipFavorite.nextFocusDownId = firstDynamicChipId

        if (smartVisible) {
            val nextDownId = if (folderVisible) folderChips.first().id else R.id.tv_sort_label
            wireDynamicChipRow(smartChips, nextUpId = R.id.chip_all, nextDownId = nextDownId)
        }
        if (folderVisible) {
            val nextUpId = if (smartVisible) smartChips.first().id else R.id.chip_all
            wireDynamicChipRow(folderChips, nextUpId = nextUpId, nextDownId = R.id.tv_sort_label)
        }
    }

    private fun dynamicChipChildren(group: ChipGroup): List<Chip> {
        return (0 until group.childCount).mapNotNull { index ->
            group.getChildAt(index) as? Chip
        }.onEach { chip ->
            if (chip.id == View.NO_ID) chip.id = View.generateViewId()
        }
    }

    private fun wireDynamicChipRow(chips: List<Chip>, nextUpId: Int, nextDownId: Int) {
        chips.forEachIndexed { index, chip ->
            chip.nextFocusLeftId = chips.getOrNull(index - 1)?.id ?: chip.id
            chip.nextFocusRightId = chips.getOrNull(index + 1)?.id ?: chip.id
            chip.nextFocusUpId = nextUpId
            chip.nextFocusDownId = nextDownId
        }
    }

    private fun toggleAdvancedFilterPopover() {
        val popover = filterPopover ?: VideoLibraryFilterPopover(
            anchor = btnLibraryFilter,
            initial = viewModel.advancedFilters.value,
            onApply = { filters ->
                viewModel.setAdvancedFilters(filters)
            },
            onDismiss = { bindFilterButtonTint(viewModel.hasActiveAdvancedFilters.value) }
        ).also { filterPopover = it }
        popover.toggle(viewModel.advancedFilters.value)
        bindFilterButtonTint(viewModel.hasActiveAdvancedFilters.value || popover.isShowing())
    }

    private fun bindFilterButtonTint(active: Boolean) {
        val colorRes = if (active) R.color.ov_accent_blue else R.color.ov_text_secondary
        btnLibraryFilter.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), colorRes)
        )
    }

    private fun folderChipLabel(folder: VideoFolderSummary): String {
        val label = getString(R.string.home_filter_folder_with_count, folder.name, folder.videoCount)
        return if (folder.isPinned) {
            getString(R.string.home_filter_folder_pinned_prefix, label)
        } else {
            label
        }
    }

    private fun createFolderChip(
        text: String,
        checked: Boolean,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            isChecked = checked
            isSingleLine = true
            checkedIcon = null
            isCheckedIconVisible = false
            setOnClickListener { onClick() }
            onLongClick?.let { handler ->
                setOnLongClickListener {
                    handler()
                    true
                }
            }
            chipStrokeWidth = resources.displayMetrics.density
            bindFolderChipStyle(this, checked)
        }
    }

    private fun createSmartListChip(
        text: String,
        checked: Boolean,
        onClick: () -> Unit
    ): Chip = createFolderChip(
        text = text,
        checked = checked,
        onClick = onClick
    )

    private fun bindFolderChipStyle(chip: Chip, selected: Boolean) {
        val background = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.ov_accent_blue else R.color.ov_bg_elevated
        )
        val stroke = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.ov_accent_blue else R.color.ov_divider
        )
        val text = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.ov_text_primary else R.color.ov_text_secondary
        )
        chip.chipBackgroundColor = ColorStateList.valueOf(background)
        chip.chipStrokeColor = ColorStateList.valueOf(stroke)
        chip.setTextColor(text)
    }

    private fun confirmDelete(video: VideoItem) {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, video.title))
            .setPositiveButton(R.string.action_delete) { _, _ -> deleteVideosWithSystemRequest(listOf(video)) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
        val cancelButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
        cancelButton.post {
            cancelButton.requestFocus()
        }
    }

    private fun startMultiSelectMode(category: HomeCategory) {
        if (actionMode != null) return
        activeCategory = category
        actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: android.view.Menu): Boolean {
                mode.menuInflater.inflate(R.menu.menu_multi_select, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: android.view.Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: android.view.MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_select_all -> {
                        activeAdapter.selectAll()
                        true
                    }
                    R.id.action_delete_selected -> {
                        confirmDeleteSelected()
                        true
                    }
                    R.id.action_favorite_selected -> {
                        activeAdapter.getSelectedItems().forEach { viewModel.toggleFavorite(it) }
                        activeAdapter.exitMultiSelectMode()
                        mode.finish()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                activeAdapter.exitMultiSelectMode()
                actionMode = null
            }
        })
    }

    private fun confirmDeleteSelected() {
        val selected = activeAdapter.getSelectedItems()
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_batch_delete_title)
            .setMessage(getString(R.string.dialog_batch_delete_message, selected.size))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deleteVideosWithSystemRequest(selected)
                activeAdapter.exitMultiSelectMode()
                actionMode?.finish()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
        val cancelButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
        cancelButton.post {
            cancelButton.requestFocus()
        }
    }

    private fun deleteVideosWithSystemRequest(videos: List<VideoItem>) {
        viewModel.deleteVideosWithResult(videos) { result ->
            if (result is VideoDeleteResult.RequiresUserAction) {
                pendingDeleteVideos = videos
                deleteRequestLauncher.launch(
                    IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                )
            }
        }
    }

    private fun completePendingDeleteAfterSystemGrant() {
        viewModel.deleteVideos(pendingDeleteVideos)
        viewModel.loadVideos()
    }
}
