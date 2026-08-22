package com.example.openvideo.ui.local

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionSheet
import com.example.openvideo.core.ui.AppleActionStyle
import com.example.openvideo.core.ui.AppleAlertDialog
import com.example.openvideo.core.ui.AppleEmptyState
import com.example.openvideo.core.ui.AppleOverlayChrome
import com.example.openvideo.core.ui.AppleOverlayColors
import com.example.openvideo.core.ui.ScreenBreakpoint
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.data.scanner.VideoDeleteResult
import com.example.openvideo.ui.BrowseAdaptiveLayoutPolicy
import com.example.openvideo.ui.MainActivity
import com.example.openvideo.ui.home.HomeViewModel
import com.example.openvideo.ui.home.VideoGridAdapter
import com.example.openvideo.ui.home.VideoOptionsSheet
import com.example.openvideo.ui.home.ViewMode
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.PlayerEpisodeOrderingPolicy
import com.example.openvideo.ui.player.putSessionQueue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FolderVideosFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: VideoGridAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private var pendingDeleteVideos: List<VideoItem> = emptyList()
    private var lastFocusedVideoId: Long? = null
    private var pendingVideoFocusRestoreId: Long? = null

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && pendingDeleteVideos.isNotEmpty()) {
            completePendingDeleteAfterSystemGrant()
        }
        pendingDeleteVideos = emptyList()
    }

    private val folderKey: String by lazy { requireArguments().getString(ARG_FOLDER_KEY).orEmpty() }
    private val folderName: String by lazy { requireArguments().getString(ARG_FOLDER_NAME).orEmpty() }

    /** 当前文件夹下列表快照（用于播放器「列表」会话队列）。 */
    private var folderVideosSnapshot: List<VideoItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_folder_videos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_videos)
        emptyView = view.findViewById(R.id.tv_empty)
        emptyView.isFocusable = true
        emptyView.nextFocusUpId = R.id.btn_back
        view.findViewById<TextView>(R.id.tv_title).text = folderName
        view.findViewById<TextView>(R.id.tv_back_title).setText(R.string.local_folder_title)
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        updateFolderVideoFocusOrder(view, hasVideos = false)

        adapter = VideoGridAdapter(
            onClick = { video -> openPlayer(video) },
            onMoreOptions = { video, _ -> showVideoOptions(video) },
            onFocusChanged = { video -> lastFocusedVideoId = video.id }
        )
        val spanCount = BrowseAdaptiveLayoutPolicy.contentSpanCount(currentBreakpoint())
        adapter.viewMode = if (spanCount > 1) ViewMode.GRID else ViewMode.LIST
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        recyclerView.adapter = adapter

        observeVideos()
        viewModel.loadVideos()
    }

    private fun observeVideos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videos.collect { videos ->
                    val folderVideos = videos.filter {
                        VideoFolderGrouper.folderKey(it.libraryPath) == folderKey
                    }
                    folderVideosSnapshot = folderVideos
                    adapter.submitList(folderVideos) { restoreVideoFocusIfNeeded(folderVideos) }
                    val hasVideos = folderVideos.isNotEmpty()
                    AppleEmptyState.setVisible(emptyView, !hasVideos)
                    recyclerView.visibility = if (hasVideos) View.VISIBLE else View.GONE
                    updateFolderVideoFocusOrder(requireView(), hasVideos)
                }
            }
        }
    }

    private fun updateFolderVideoFocusOrder(view: View, hasVideos: Boolean) {
        val contentFocusTargetId = if (hasVideos) R.id.recycler_videos else R.id.tv_empty
        view.findViewById<View>(R.id.btn_back).nextFocusDownId = contentFocusTargetId
    }

    private fun currentBreakpoint(): ScreenBreakpoint =
        (activity as? MainActivity)?.breakpoint ?: ScreenBreakpoint.COMPACT

    private fun openPlayer(video: VideoItem) {
        lastFocusedVideoId = video.id
        pendingVideoFocusRestoreId = lastFocusedVideoId
        val orderedQueue = PlayerEpisodeOrderingPolicy.orderSameFolderQueue(folderVideosSnapshot)
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putSessionQueue(orderedQueue)
            putExtra("video_uri", video.uri.toString())
            putExtra("video_title", video.title)
            putExtra("video_id", video.id)
            putExtra("video_path", video.path)
            putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, video.width)
            putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, video.height)
        }
        startActivity(intent)
    }

    private fun restoreVideoFocusIfNeeded(videos: List<VideoItem>) {
        val videoId = pendingVideoFocusRestoreId ?: return
        val position = videos.indexOfFirst { it.id == videoId }
        if (position == -1) return
        pendingVideoFocusRestoreId = null
        recyclerView.post {
            recyclerView.scrollToPosition(position)
            recyclerView.post {
                recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
            }
        }
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
        AppleActionSheet.show(
            context = requireContext(),
            title = getString(R.string.playlist_add_to_title),
            actions = playlists.map { playlist ->
                AppleAction(playlist.name) { viewModel.addToPlaylist(playlist.id, video) }
            } + AppleAction(getString(R.string.playlist_create_and_add)) {
                showCreatePlaylistForVideoDialog(video)
            },
            defaultFocusCancel = false
        )
    }

    private fun showCreatePlaylistForVideoDialog(video: VideoItem) {
        val input = AppleOverlayChrome.inputField(
            context = requireContext(),
            colors = AppleOverlayColors.from(requireContext()),
            hint = getString(R.string.playlist_hint_name),
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        )
        AppleAlertDialog.show(
            context = requireContext(),
            title = getString(R.string.playlist_create_title),
            message = getString(R.string.playlist_add_empty_message, video.title),
            extraContent = input,
            includeIme = true,
            focusView = input,
            actions = listOf(
                AppleAction(getString(R.string.action_cancel), AppleActionStyle.CANCEL),
                AppleAction(getString(R.string.action_create)) {
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) viewModel.createPlaylistWithVideo(name, video)
                }
            )
        )
        input.post {
            input.requestFocus()
        }
    }

    private fun confirmDelete(video: VideoItem) {
        AppleAlertDialog.show(
            context = requireContext(),
            title = getString(R.string.dialog_delete_title),
            message = getString(R.string.dialog_delete_message, video.title),
            actions = listOf(
                AppleAction(getString(R.string.action_cancel), AppleActionStyle.CANCEL),
                AppleAction(
                    title = getString(R.string.action_delete),
                    style = AppleActionStyle.DESTRUCTIVE,
                    onClick = { deleteVideosWithSystemRequest(listOf(video)) }
                )
            )
        )
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

    companion object {
        private const val ARG_FOLDER_KEY = "folder_key"
        private const val ARG_FOLDER_NAME = "folder_name"

        fun newInstance(folderKey: String, folderName: String): FolderVideosFragment {
            return FolderVideosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FOLDER_KEY, folderKey)
                    putString(ARG_FOLDER_NAME, folderName)
                }
            }
        }
    }
}
