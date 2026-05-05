package com.example.openvideo.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistFragment : Fragment() {

    private val viewModel: PlaylistViewModel by viewModels()
    private lateinit var adapter: PlaylistAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_playlists)
        emptyView = view.findViewById(R.id.tv_empty)

        adapter = PlaylistAdapter(
            onClick = { playlist ->
                // Navigate to playlist detail
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, PlaylistDetailFragment.newInstance(playlist.id, playlist.name))
                    .addToBackStack(null)
                    .commit()
            },
            onMoreOptions = { playlist, anchor ->
                showPlaylistOptions(playlist)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            showCreateDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlists.collect { list ->
                    adapter.submitList(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showCreateDialog() {
        val input = EditText(requireContext()).apply {
            hint = "播放列表名称"
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("创建播放列表")
            .setView(input)
            .setPositiveButton("创建") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.createPlaylist(name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPlaylistOptions(playlist: com.example.openvideo.data.local.PlaylistEntity) {
        val options = arrayOf("重命名", "删除")
        AlertDialog.Builder(requireContext())
            .setTitle(playlist.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(playlist)
                    1 -> confirmDelete(playlist)
                }
            }
            .show()
    }

    private fun showRenameDialog(playlist: com.example.openvideo.data.local.PlaylistEntity) {
        val input = EditText(requireContext()).apply {
            setText(playlist.name)
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("重命名")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.renamePlaylist(playlist.id, name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDelete(playlist: com.example.openvideo.data.local.PlaylistEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除播放列表")
            .setMessage("确定删除「${playlist.name}」？")
            .setPositiveButton("删除") { _, _ -> viewModel.deletePlaylist(playlist.id) }
            .setNegativeButton("取消", null)
            .show()
    }
}
