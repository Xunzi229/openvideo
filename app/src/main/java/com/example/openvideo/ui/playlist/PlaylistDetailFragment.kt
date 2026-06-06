package com.example.openvideo.ui.playlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.PlayerEpisodeOrderingPolicy
import com.example.openvideo.ui.player.putSessionQueue
import com.example.openvideo.ui.player.toSessionVideoItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var playlistVideosSnapshot: List<PlaylistVideoEntity> = emptyList()

    private val exportPlaylistLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(PlaylistTransferFormat.JSON_MIME_TYPE)
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            when (viewModel.writePlaylistExportTo(requireContext(), uri, playlistId, playlistName)) {
                is PlaylistViewModel.TransferResult.Success ->
                    Toast.makeText(requireContext(), R.string.playlist_export_success, Toast.LENGTH_SHORT).show()
                is PlaylistViewModel.TransferResult.ReadWriteFailure,
                is PlaylistViewModel.TransferResult.ParseFailure ->
                    Toast.makeText(requireContext(), R.string.playlist_export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importPlaylistLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            when (viewModel.readAndImportPlaylist(requireContext(), uri, playlistId)) {
                is PlaylistViewModel.TransferResult.Success ->
                    Toast.makeText(requireContext(), R.string.playlist_import_success, Toast.LENGTH_SHORT).show()
                is PlaylistViewModel.TransferResult.ParseFailure ->
                    Toast.makeText(requireContext(), R.string.playlist_import_parse_error, Toast.LENGTH_LONG).show()
                is PlaylistViewModel.TransferResult.ReadWriteFailure ->
                    Toast.makeText(requireContext(), R.string.playlist_import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        view.findViewById<ImageButton>(R.id.btn_import_playlist).setOnClickListener {
            importPlaylistLauncher.launch(PlaylistTransferFormat.SUPPORTED_IMPORT_MIME_TYPES)
        }
        view.findViewById<ImageButton>(R.id.btn_export_playlist).setOnClickListener {
            exportPlaylistLauncher.launch(PlaylistTransferFormat.suggestedJsonFileName(playlistName))
        }
        view.findViewById<ImageButton>(R.id.btn_cleanup_playlist).setOnClickListener {
            confirmCleanup()
        }

        recyclerView = view.findViewById(R.id.recycler_videos)
        emptyView = view.findViewById(R.id.tv_empty)

        adapter = PlaylistVideoAdapter(
            onClick = click@{ video ->
                if (!PlaylistVideoAvailabilityPolicy.isAvailable(video.videoPath)) {
                    Toast.makeText(requireContext(), R.string.playlist_video_missing, Toast.LENGTH_SHORT).show()
                    viewModel.removeStalePlaylistVideos(playlistId, listOf(video.videoId))
                    return@click
                }
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
        setupReorderTouchHelper()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getPlaylistVideos(playlistId).collect { list ->
                    val sorted = list.sortedBy { it.position }
                    val staleIds = PlaylistVideoAvailabilityPolicy.staleVideoIds(sorted)
                    if (staleIds.isNotEmpty()) {
                        viewModel.removeStalePlaylistVideos(playlistId, staleIds)
                    }
                    val visible = PlaylistVideoAvailabilityPolicy.filterPlayable(sorted)
                    playlistVideosSnapshot = visible
                    adapter.submitList(visible)
                    emptyView.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (visible.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun setupReorderTouchHelper() {
        itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    source: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = source.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                        return false
                    }
                    val reordered = PlaylistReorderPolicy.move(
                        videos = playlistVideosSnapshot,
                        fromIndex = from,
                        toIndex = to
                    )
                    playlistVideosSnapshot = reordered
                    adapter.submitList(reordered)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    viewModel.reorderPlaylistVideos(playlistId, playlistVideosSnapshot)
                }
            }
        )
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onResume() {
        super.onResume()
        viewModel.pruneMissingFilesFromPlaylist(playlistId)
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_clear_title)
            .setMessage(getString(R.string.playlist_clear_message, playlistName))
            .setPositiveButton(R.string.action_clear) { _, _ -> viewModel.clearPlaylist(playlistId) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmCleanup() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_cleanup_title)
            .setMessage(getString(R.string.playlist_cleanup_message, playlistName))
            .setPositiveButton(R.string.playlist_cleanup) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val removedVideos = viewModel.cleanupPlaylistVideosForUndo(playlistId)
                    showCleanupUndo(removedVideos)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showCleanupUndo(removedVideos: List<PlaylistVideoEntity>) {
        if (removedVideos.isEmpty()) return
        Snackbar.make(requireView(), R.string.playlist_cleanup_complete, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) {
                viewModel.restorePlaylistVideos(removedVideos)
            }
            .show()
    }
}
