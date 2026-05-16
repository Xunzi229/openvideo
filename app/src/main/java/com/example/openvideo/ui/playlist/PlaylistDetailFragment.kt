package com.example.openvideo.ui.playlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.PlayerEpisodeOrderingPolicy
import com.example.openvideo.ui.player.putSessionQueue
import com.example.openvideo.ui.player.toSessionVideoItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistDetailFragment : Fragment() {

    private val viewModel: PlaylistViewModel by viewModels()
    private var playlistId: Long = 0
    private var playlistName: String = ""
    private lateinit var adapter: PlaylistVideoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    private var playlistVideosSnapshot: List<PlaylistVideoEntity> = emptyList()

    companion object {
        fun newInstance(playlistId: Long, playlistName: String): PlaylistDetailFragment {
            return PlaylistDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong("playlist_id", playlistId)
                    putString("playlist_name", playlistName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = arguments?.getLong("playlist_id") ?: 0
        playlistName = arguments?.getString("playlist_name") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_playlist_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tv_title).text = playlistName
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<ImageButton>(R.id.btn_clear).setOnClickListener {
            confirmClear()
        }

        recyclerView = view.findViewById(R.id.recycler_videos)
        emptyView = view.findViewById(R.id.tv_empty)

        adapter = PlaylistVideoAdapter(
            onClick = { video ->
                val queue = playlistVideosSnapshot.map { it.toSessionVideoItem() }
                val orderedQueue = PlayerEpisodeOrderingPolicy.orderQueueIfEligible(queue)
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putSessionQueue(orderedQueue)
                    putExtra("video_uri", video.videoPath)
                    putExtra("video_title", video.videoTitle)
                    putExtra("video_id", video.videoId)
                    putExtra("video_path", video.videoPath)
                }
                startActivity(intent)
            },
            onRemove = { video ->
                viewModel.removeFromPlaylist(playlistId, video.videoId)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getPlaylistVideos(playlistId).collect { list ->
                    playlistVideosSnapshot = list.sortedBy { it.position }
                    adapter.submitList(playlistVideosSnapshot)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_clear_title)
            .setMessage(getString(R.string.playlist_clear_message, playlistName))
            .setPositiveButton(R.string.action_clear) { _, _ -> viewModel.clearPlaylist(playlistId) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
