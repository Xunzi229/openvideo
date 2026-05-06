package com.example.openvideo.ui.local

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
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
import com.example.openvideo.ui.home.HomeViewModel
import com.example.openvideo.ui.home.VideoGridAdapter
import com.example.openvideo.ui.home.VideoOptionsSheet
import com.example.openvideo.ui.player.PlayerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FolderVideosFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: VideoGridAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    private val folderKey: String by lazy { requireArguments().getString(ARG_FOLDER_KEY).orEmpty() }
    private val folderName: String by lazy { requireArguments().getString(ARG_FOLDER_NAME).orEmpty() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_folder_videos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_videos)
        emptyView = view.findViewById(R.id.tv_empty)
        view.findViewById<TextView>(R.id.tv_title).text = folderName
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = VideoGridAdapter(
            onClick = { video -> openPlayer(video) },
            onMoreOptions = { video, _ -> showVideoOptions(video) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        observeVideos()
        viewModel.loadVideos()
    }

    private fun observeVideos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videos.collect { videos ->
                    val folderVideos = videos.filter {
                        VideoFolderGrouper.folderKey(it.path) == folderKey
                    }
                    adapter.submitList(folderVideos)
                    emptyView.visibility = if (folderVideos.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (folderVideos.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun openPlayer(video: VideoItem) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
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

    private fun confirmDelete(video: VideoItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, video.title))
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteVideo(video) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
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
