package com.example.openvideo.ui.local

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
import com.example.openvideo.ui.home.MediaLibraryEmptyState
import com.example.openvideo.ui.home.MediaLibraryPermissionPolicy
import com.example.openvideo.ui.home.MediaLibraryScanLoadingUi
import com.example.openvideo.ui.home.MediaLibraryScanProgress
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.PlayerEpisodeOrderingPolicy
import com.example.openvideo.ui.player.putSessionQueue
import com.example.openvideo.ui.series.SeriesListFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocalFolderFragment : Fragment() {

    private val viewModel: LocalFolderViewModel by viewModels()
    private lateinit var adapter: VideoFolderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var scanLoadingContainer: View
    private lateinit var scanProgressBar: ProgressBar
    private lateinit var scanProgressLabel: TextView
    private lateinit var continuePlaybackFab: FloatingActionButton
    private var localVideosSnapshot: List<VideoItem> = emptyList()
    private var continuePlaybackVideo: VideoItem? = null
    private var continuePlaybackPositionMs: Long = 0L
    private var latestEmptyState: MediaLibraryEmptyState = MediaLibraryEmptyState.LOADING
    private var latestScanProgress: MediaLibraryScanProgress? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.any { it.value }) viewModel.loadVideos()
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
        scanLoadingContainer = view.findViewById(R.id.scan_loading_container)
        scanProgressBar = view.findViewById(R.id.scan_progress_bar)
        scanProgressLabel = view.findViewById(R.id.tv_scan_progress)
        continuePlaybackFab = view.findViewById(R.id.fab_continue_playback)

        adapter = VideoFolderAdapter(
            onClick = { folder -> openFolder(folder) },
            onLongClick = { folder -> viewModel.togglePinnedFolder(folder.key) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        continuePlaybackFab.setOnClickListener {
            continuePlaybackVideo?.let { video -> openPlayer(video) }
        }

        view.findViewById<View>(R.id.btn_refresh).setOnClickListener {
            checkPermissionAndLoad()
        }
        view.findViewById<View>(R.id.btn_series).setOnClickListener {
            openSeriesList()
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
                    }
                }
                launch {
                    combine(viewModel.emptyState, viewModel.scanProgress, viewModel.folders) { state, progress, folders ->
                        Triple(state, progress, folders)
                    }.collect { (state, progress, folders) ->
                        latestEmptyState = state
                        latestScanProgress = progress
                        emptyView.text = when (state) {
                            MediaLibraryEmptyState.PERMISSION_DENIED -> getString(R.string.media_library_permission_denied)
                            MediaLibraryEmptyState.SCAN_ERROR -> getString(R.string.media_library_scan_error)
                            MediaLibraryEmptyState.NO_MEDIA -> getString(R.string.no_videos)
                            else -> getString(R.string.no_videos)
                        }
                        val showBlockingEmpty = state == MediaLibraryEmptyState.PERMISSION_DENIED ||
                            state == MediaLibraryEmptyState.SCAN_ERROR ||
                            state == MediaLibraryEmptyState.LOADING ||
                            state == MediaLibraryEmptyState.NO_MEDIA
                        if (showBlockingEmpty) {
                            updateFolderListVisibility(false)
                        } else if (folders.isNotEmpty()) {
                            updateFolderListVisibility(true)
                        }
                        bindEmptyUi(state, progress, folders.isEmpty())
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
        if (hasVideoReadPermission()) {
            viewModel.loadVideos()
        } else {
            permissionLauncher.launch(MediaLibraryPermissionPolicy.requiredPermissions())
        }
    }

    private fun hasVideoReadPermission(): Boolean =
        MediaLibraryPermissionPolicy.requiredPermissions().any { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }

    private fun updateFolderListVisibility(hasFolders: Boolean) {
        if (hasFolders) {
            scanLoadingContainer.visibility = View.GONE
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            return
        }
        bindEmptyUi(
            state = latestEmptyState,
            progress = latestScanProgress,
            isContentEmpty = true
        )
        recyclerView.visibility = View.GONE
    }

    private fun bindEmptyUi(
        state: MediaLibraryEmptyState,
        progress: MediaLibraryScanProgress?,
        isContentEmpty: Boolean
    ) {
        MediaLibraryScanLoadingUi.bind(
            context = requireContext(),
            loadingContainer = scanLoadingContainer,
            progressBar = scanProgressBar,
            progressLabel = scanProgressLabel,
            emptyLabel = emptyView,
            emptyState = state,
            scanProgress = progress,
            isContentEmpty = isContentEmpty
        )
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndLoad()
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

    private fun openSeriesList() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SeriesListFragment())
            .addToBackStack("series:list")
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
