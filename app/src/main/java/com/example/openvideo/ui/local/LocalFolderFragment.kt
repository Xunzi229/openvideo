package com.example.openvideo.ui.local

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.openvideo.ui.player.PlayerEpisodeOrderingPolicy
import com.example.openvideo.ui.player.putSessionQueue
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocalFolderFragment : Fragment() {

    private val viewModel: LocalFolderViewModel by viewModels()
    private lateinit var adapter: VideoFolderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var continuePlaybackFab: FloatingActionButton
    private var localVideosSnapshot: List<VideoItem> = emptyList()
    private var continuePlaybackVideo: VideoItem? = null
    private var continuePlaybackPositionMs: Long = 0L

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadVideos()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_local_folders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_folders)
        emptyView = view.findViewById(R.id.tv_empty)
        continuePlaybackFab = view.findViewById(R.id.fab_continue_playback)

        adapter = VideoFolderAdapter { folder -> openFolder(folder) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        continuePlaybackFab.setOnClickListener {
            continuePlaybackVideo?.let { video -> openPlayer(video) }
        }

        view.findViewById<View>(R.id.btn_refresh).setOnClickListener {
            checkPermissionAndLoad()
        }

        observeFolders()
        checkPermissionAndLoad()
    }

    private fun observeFolders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.folders.collect { folders ->
                        adapter.submitList(folders)
                        emptyView.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE
                        recyclerView.visibility = if (folders.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.continuePlaybackVideo.collect { video ->
                        continuePlaybackVideo = video
                        continuePlaybackFab.visibility = if (video == null) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.continuePlaybackPositionMs.collect { positionMs ->
                        continuePlaybackPositionMs = positionMs
                    }
                }
                launch {
                    viewModel.videos.collect { videos ->
                        localVideosSnapshot = videos
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

    private fun openFolder(folder: VideoFolder) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                FolderVideosFragment.newInstance(folder.key, folder.name)
            )
            .addToBackStack("folder:${folder.key}")
            .commit()
    }

    private fun openPlayer(video: VideoItem) {
        val sameFolderQueue = localVideosSnapshot.filter {
            VideoFolderGrouper.folderKey(it.path) == VideoFolderGrouper.folderKey(video.path)
        }
        val orderedQueue = PlayerEpisodeOrderingPolicy.orderSameFolderQueue(sameFolderQueue)
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putSessionQueue(orderedQueue.ifEmpty { listOf(video) })
            putExtra("video_uri", video.uri.toString())
            putExtra("video_title", video.title)
            putExtra("video_id", video.id)
            putExtra("video_path", video.path)
            putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, video.width)
            putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, video.height)
            putExtra(PlayerActivity.EXTRA_START_POSITION_MS, continuePlaybackPositionMs)
        }
        startActivity(intent)
    }
}
