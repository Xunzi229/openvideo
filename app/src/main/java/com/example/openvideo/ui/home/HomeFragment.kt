package com.example.openvideo.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.player.PlayerActivity
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
    private lateinit var btnList: ImageButton
    private lateinit var btnGrid: ImageButton
    private var actionMode: ActionMode? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadVideos()
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
        sortLabel.setOnClickListener { viewModel.cycleSortField() }

        btnList = view.findViewById(R.id.btn_list_view)
        btnGrid = view.findViewById(R.id.btn_grid_view)
        btnList.setOnClickListener { viewModel.setViewMode(ViewMode.LIST) }
        btnGrid.setOnClickListener { viewModel.setViewMode(ViewMode.GRID) }

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
                        adapter.submitList(list)
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
                        sortLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            0, 0, if (asc) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down, 0
                        )
                    }
                }
                launch {
                    viewModel.viewMode.collect { mode ->
                        adapter.viewMode = mode
                        val spanCount = if (mode == ViewMode.GRID) 2 else 1
                        val lm = recyclerView.layoutManager
                        if (lm is androidx.recyclerview.widget.GridLayoutManager) {
                            lm.spanCount = spanCount
                        } else {
                            recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), spanCount)
                        }
                        updateViewModeButtons(mode)
                    }
                }
            }
        }
    }

    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadVideos()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun openPlayer(video: VideoItem) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("video_uri", video.uri.toString())
            putExtra("video_title", video.title)
            putExtra("video_id", video.id)
        }
        startActivity(intent)
    }

    private fun showVideoOptions(video: VideoItem) {
        VideoOptionsSheet(
            context = requireContext(),
            video = video,
            onFavorite = { viewModel.toggleFavorite(video) },
            onAddToPlaylist = { showAddToPlaylistDialog(video) },
            onDelete = { confirmDelete(video) }
        ).show()
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

    private fun confirmDelete(video: VideoItem) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, video.title))
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteVideo(video) }
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
                viewModel.deleteVideos(selected)
                adapter.exitMultiSelectMode()
                actionMode?.finish()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
