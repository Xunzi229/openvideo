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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.core.ui.AppleEmptyState
import com.example.openvideo.core.ui.ScreenBreakpoint
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.BrowseAdaptiveLayoutPolicy
import com.example.openvideo.ui.MainActivity
import com.example.openvideo.ui.home.MediaLibraryEmptyState
import com.example.openvideo.ui.home.MediaLibraryPermissionPolicy
import com.example.openvideo.ui.home.MediaLibraryScanLoadingUi
import com.example.openvideo.ui.home.MediaLibraryScanProgress
import com.example.openvideo.core.ui.LibraryNavigator
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.PlayerEpisodeOrderingPolicy
import com.example.openvideo.ui.player.putSessionQueue
import com.example.openvideo.ui.series.SeriesListFragment
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
    private lateinit var continuePlaybackRow: View
    private lateinit var continuePlaybackTitle: TextView
    private var localVideosSnapshot: List<VideoItem> = emptyList()
    private var continuePlaybackVideo: VideoItem? = null
    private var continuePlaybackPositionMs: Long = 0L
    private var latestEmptyState: MediaLibraryEmptyState = MediaLibraryEmptyState.LOADING
    private var latestScanProgress: MediaLibraryScanProgress? = null
    private var lastFocusedFolderKey: String? = null
    private var pendingFolderFocusRestoreKey: String? = null

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
        emptyView.isFocusable = true
        emptyView.nextFocusUpId = R.id.btn_refresh
        scanLoadingContainer = view.findViewById(R.id.scan_loading_container)
        scanProgressBar = view.findViewById(R.id.scan_progress_bar)
        scanProgressLabel = view.findViewById(R.id.tv_scan_progress)
        continuePlaybackRow = view.findViewById(R.id.row_continue_playback)
        continuePlaybackTitle = view.findViewById(R.id.tv_continue_title)

        adapter = VideoFolderAdapter(
            onClick = { folder -> openFolder(folder) },
            onLongClick = { folder -> viewModel.togglePinnedFolder(folder.key) },
            onFocusChanged = { folder -> lastFocusedFolderKey = folder.key }
        )
        recyclerView.layoutManager = GridLayoutManager(
            requireContext(),
            BrowseAdaptiveLayoutPolicy.contentSpanCount(currentBreakpoint())
        )
        recyclerView.adapter = adapter

        continuePlaybackRow.setOnClickListener {
            continuePlaybackVideo?.let { video -> openPlayer(video) }
        }

        updateFolderFocusOrder(view, hasFolders = false)
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
                        adapter.submitList(folders) { restoreFolderFocusIfNeeded(folders) }
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
                        continuePlaybackRow.visibility = if (video == null) View.GONE else View.VISIBLE
                        continuePlaybackTitle.text = video?.title.orEmpty()
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
        view?.let { updateFolderFocusOrder(it, hasFolders) }
        if (hasFolders) {
            scanLoadingContainer.visibility = View.GONE
            AppleEmptyState.setVisible(emptyView, false)
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

    private fun updateFolderFocusOrder(view: View, hasFolders: Boolean) {
        val contentFocusTargetId = if (hasFolders) R.id.recycler_folders else R.id.tv_empty
        view.findViewById<View>(R.id.btn_series).nextFocusDownId = contentFocusTargetId
        view.findViewById<View>(R.id.btn_refresh).nextFocusDownId = contentFocusTargetId
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

    private fun currentBreakpoint(): ScreenBreakpoint =
        (activity as? MainActivity)?.breakpoint ?: ScreenBreakpoint.COMPACT

    override fun onResume() {
        super.onResume()
        checkPermissionAndLoad()
    }

    private fun openFolder(folder: VideoFolder) {
        lastFocusedFolderKey = folder.key
        pendingFolderFocusRestoreKey = lastFocusedFolderKey
        LibraryNavigator.push(
            this,
            FolderVideosFragment.newInstance(folder.key, folder.name),
            "folder:${folder.key}"
        )
    }

    private fun restoreFolderFocusIfNeeded(folders: List<VideoFolder>) {
        val key = pendingFolderFocusRestoreKey ?: return
        val position = folders.indexOfFirst { it.key == key }
        if (position == -1) return
        pendingFolderFocusRestoreKey = null
        recyclerView.post {
            recyclerView.scrollToPosition(position)
            recyclerView.post {
                recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
            }
        }
    }

    private fun openSeriesList() {
        LibraryNavigator.push(this, SeriesListFragment(), "series:list")
    }

    private fun openPlayer(video: VideoItem) {
        val sameFolderQueue = localVideosSnapshot.filter {
            VideoFolderGrouper.folderKey(it.libraryPath) ==
                VideoFolderGrouper.folderKey(video.libraryPath)
        }
        val orderedQueue = PlayerEpisodeOrderingPolicy.orderSameFolderQueue(sameFolderQueue)
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putSessionQueue(requireContext(), orderedQueue.ifEmpty { listOf(video) })
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
