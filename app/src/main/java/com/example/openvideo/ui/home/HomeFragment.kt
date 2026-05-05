package com.example.openvideo.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
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

        adapter = VideoGridAdapter { video ->
            openPlayer(video)
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        view.findViewById<ImageButton>(R.id.btn_refresh).setOnClickListener {
            checkPermissionAndLoad()
        }

        observeVideos()
        checkPermissionAndLoad()
    }

    private fun observeVideos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videos.collect { list ->
                    adapter.submitList(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
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
}
