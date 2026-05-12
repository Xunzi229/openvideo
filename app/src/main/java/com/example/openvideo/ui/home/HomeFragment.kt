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
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.scanner.VideoDeleteResult
import com.example.openvideo.ui.local.VideoFolderSummary
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.putSessionQueue
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: VideoGridAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var searchView: EditText
    private lateinit var sortLabel: TextView
    private lateinit var btnSortOrder: ImageButton
    private lateinit var btnList: ImageButton
    private lateinit var btnGrid: ImageButton
    private lateinit var chipAll: Chip
    private lateinit var chipRecent: Chip
    private lateinit var chipFavorite: Chip
    private lateinit var filterScroll: View
    private lateinit var folderGroup: ChipGroup
    private var actionMode: ActionMode? = null
    private var pendingDeleteVideos: List<VideoItem> = emptyList()
    private var pendingJumpToTop = false
    private var currentFolders: List<VideoFolderSummary> = emptyList()
    private var currentSelectedFolderKey: String? = null

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

        recyclerView = view.findViewById(R.id.recycler_videos)
        emptyView = view.findViewById(R.id.tv_empty)
        searchView = view.findViewById(R.id.search_view)
        searchView.visibility = View.VISIBLE

        sortLabel = view.findViewById(R.id.tv_sort_label)
        sortLabel.setOnClickListener { changeSortFieldAndScrollToTop() }
        btnSortOrder = view.findViewById(R.id.btn_sort_order)
        btnSortOrder.setOnClickListener { toggleSortOrderAndScrollToTop() }

        btnList = view.findViewById(R.id.btn_list_view)
        btnGrid = view.findViewById(R.id.btn_grid_view)
        btnList.setOnClickListener { viewModel.setViewMode(ViewMode.LIST) }
        btnGrid.setOnClickListener { viewModel.setViewMode(ViewMode.GRID) }
        chipAll = view.findViewById(R.id.chip_all)
        chipRecent = view.findViewById(R.id.chip_recent)
        chipFavorite = view.findViewById(R.id.chip_favorite)
        filterScroll = view.findViewById(R.id.filter_scroll)
        folderGroup = view.findViewById(R.id.folder_group)
        chipAll.setOnClickListener { switchCategory(HomeCategory.ALL) }
        chipRecent.setOnClickListener { switchCategory(HomeCategory.RECENT) }
        chipFavorite.setOnClickListener { switchCategory(HomeCategory.FAVORITES) }

        adapter = VideoGridAdapter(
            onClick = { video -> openPlayer(video) },
            onMoreOptions = { video, anchor -> showVideoOptions(video) },
            onSelectionChanged = { selected ->
                if (adapter.isMultiSelectMode) {
                    actionMode?.title = getString(R.string.multi_select_count, selected.size)
                }
            },
            onLongClick = { video -> startMultiSelectMode() }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<ImageButton>(R.id.btn_refresh).setOnClickListener {
            checkPermissionAndLoad()
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

    private fun observeVideos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.videos.collect { list ->
                        adapter.submitList(list) {
                            jumpVideoListToTopIfNeeded()
                        }
                        emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
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
                    viewModel.viewMode.collect { mode ->
                        adapter.viewMode = mode
                        val spanCount = if (mode == ViewMode.GRID) 2 else 1
                        val lm = recyclerView.layoutManager
                        if (lm is GridLayoutManager) {
                            lm.spanCount = spanCount
                        } else {
                            recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
                        }
                        updateViewModeButtons(mode)
                    }
                }
                launch {
                    viewModel.category.collect { category ->
                        updateCategoryChips(category)
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
                    viewModel.emptyState.collect { state ->
                        emptyView.text = when (state) {
                            MediaLibraryEmptyState.LOADING -> getString(R.string.media_library_loading)
                            MediaLibraryEmptyState.NO_MEDIA -> getString(R.string.no_videos)
                            MediaLibraryEmptyState.FILTERED_BY_PRIVACY -> getString(R.string.media_library_empty_privacy)
                            MediaLibraryEmptyState.FILTERED_BY_QUERY_OR_FOLDER -> getString(R.string.media_library_empty_filtered)
                            MediaLibraryEmptyState.NONE -> getString(R.string.no_videos)
                        }
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

    private fun switchCategory(category: HomeCategory) {
        viewModel.setCategory(category)
        recyclerView.scrollToPosition(0)
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
        pendingJumpToTop = true
    }

    private fun jumpVideoListToTopIfNeeded() {
        if (!pendingJumpToTop) return
        pendingJumpToTop = false
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is LinearLayoutManager) {
            layoutManager.scrollToPositionWithOffset(0, 0)
        } else {
            recyclerView.scrollToPosition(0)
        }
    }

    private fun hasVideoReadPermission(): Boolean {
        return videoReadPermissions().any { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) ==
                PermissionChecker.PERMISSION_GRANTED
        }
    }

    private fun videoReadPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun openPlayer(video: VideoItem) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putSessionQueue(viewModel.videos.value)
            putExtra("video_uri", video.uri.toString())
            putExtra("video_title", video.title)
            putExtra("video_id", video.id)
            putExtra("video_path", video.path)
            putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, video.width)
            putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, video.height)
        }
        startActivity(intent)
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
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_add_to_title)
            .setItems(names) { _, which ->
                if (which == playlists.size) {
                    showCreatePlaylistForVideoDialog(video)
                } else {
                    viewModel.addToPlaylist(playlists[which].id, video)
                }
            }
            .show()
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
        bindCategoryChip(chipAll, category == HomeCategory.ALL)
        bindCategoryChip(chipRecent, category == HomeCategory.RECENT)
        bindCategoryChip(chipFavorite, category == HomeCategory.FAVORITES)
    }

    private fun bindCategoryChip(chip: Chip, selected: Boolean) {
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
    }

    private fun bindFolderChips() {
        filterScroll.visibility = if (currentFolders.size > 1) View.VISIBLE else View.GONE
        folderGroup.removeAllViews()
        folderGroup.addView(createFolderChip(
            text = getString(R.string.home_filter_all_folders),
            checked = currentSelectedFolderKey == null,
            onClick = { viewModel.setFolderFilter(null) }
        ))
        currentFolders.forEach { folder ->
            folderGroup.addView(createFolderChip(
                text = getString(R.string.home_filter_folder_with_count, folder.name, folder.videoCount),
                checked = currentSelectedFolderKey == folder.key,
                onClick = { viewModel.setFolderFilter(folder.key) }
            ))
        }
    }

    private fun createFolderChip(text: String, checked: Boolean, onClick: () -> Unit): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            isChecked = checked
            isSingleLine = true
            checkedIcon = null
            isCheckedIconVisible = false
            setOnClickListener { onClick() }
            chipStrokeWidth = resources.displayMetrics.density
            bindFolderChipStyle(this, checked)
        }
    }

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
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, video.title))
            .setPositiveButton(R.string.action_delete) { _, _ -> deleteVideosWithSystemRequest(listOf(video)) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun startMultiSelectMode() {
        if (actionMode != null) return
        actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: android.view.Menu): Boolean {
                mode.menuInflater.inflate(R.menu.menu_multi_select, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: android.view.Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: android.view.MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_select_all -> {
                        adapter.selectAll()
                        true
                    }
                    R.id.action_delete_selected -> {
                        confirmDeleteSelected()
                        true
                    }
                    R.id.action_favorite_selected -> {
                        adapter.getSelectedItems().forEach { viewModel.toggleFavorite(it) }
                        adapter.exitMultiSelectMode()
                        mode.finish()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                adapter.exitMultiSelectMode()
                actionMode = null
            }
        })
    }

    private fun confirmDeleteSelected() {
        val selected = adapter.getSelectedItems()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_batch_delete_title)
            .setMessage(getString(R.string.dialog_batch_delete_message, selected.size))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deleteVideosWithSystemRequest(selected)
                adapter.exitMultiSelectMode()
                actionMode?.finish()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
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
