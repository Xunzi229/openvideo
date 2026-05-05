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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
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
            onMoreOptions = { video, anchor -> showVideoOptions(video) }
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
                        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), spanCount)
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
            onDelete = { confirmDelete(video) }
        ).show()
    }

    private fun updateViewModeButtons(mode: ViewMode) {
        val activeBg = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.ov_accent_blue)
        val inactiveBg = android.graphics.Color.TRANSPARENT
        val activeTint = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.ov_text_primary)
        val inactiveTint = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.ov_text_secondary)

        if (mode == ViewMode.LIST) {
            btnList.setBackgroundColor(activeBg)
            btnGrid.setBackgroundColor(inactiveBg)
            btnList.setColorFilter(activeTint)
            btnGrid.setColorFilter(inactiveTint)
        } else {
            btnList.setBackgroundColor(inactiveBg)
            btnGrid.setBackgroundColor(activeBg)
            btnList.setColorFilter(inactiveTint)
            btnGrid.setColorFilter(activeTint)
        }
    }

    private fun confirmDelete(video: VideoItem) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("删除视频")
            .setMessage("确定删除「${video.title}」？删除后不可恢复。")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteVideo(video) }
            .setNegativeButton("取消", null)
            .show()
    }
}
