package com.example.openvideo.ui.playlist

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.ui.settings.SettingsConfirmationActionSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistFragment : Fragment() {

    private val viewModel: PlaylistViewModel by viewModels()
    private lateinit var adapter: PlaylistAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private var activePlaylistDialog: Dialog? = null

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
            hint = getString(R.string.playlist_hint_name)
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_create_title)
            .setView(input)
            .setPositiveButton(R.string.action_create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.createPlaylist(name)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showPlaylistOptions(playlist: com.example.openvideo.data.local.PlaylistEntity) {
        showExclusivePlaylistDialog { onDismiss ->
            PlaylistOptionsActionSheet.show(
                context = requireContext(),
                playlistName = playlist.name,
                onDismiss = onDismiss,
                onRename = { showRenameDialog(playlist) },
                onDelete = { confirmDelete(playlist) }
            )
        }
    }

    private fun showRenameDialog(playlist: com.example.openvideo.data.local.PlaylistEntity) {
        showExclusivePlaylistDialog { onDismiss ->
            PlaylistRenameActionSheet.show(
                context = requireContext(),
                initialName = playlist.name,
                onDismiss = onDismiss,
                onConfirm = { name ->
                    viewModel.renamePlaylist(playlist.id, name)
                }
            )
        }
    }

    private fun confirmDelete(playlist: com.example.openvideo.data.local.PlaylistEntity) {
        showExclusivePlaylistDialog { onDismiss ->
            SettingsConfirmationActionSheet.show(
                context = requireContext(),
                titleRes = R.string.playlist_delete_title,
                message = getString(R.string.playlist_delete_message, playlist.name),
                confirmRes = R.string.action_delete,
                cancelRes = R.string.action_cancel,
                onDismiss = onDismiss,
                onConfirm = { viewModel.deletePlaylist(playlist.id) }
            )
        }
    }

    private fun showExclusivePlaylistDialog(
        showDialog: (onDismiss: () -> Unit) -> Dialog
    ) {
        val current = activePlaylistDialog
        if (current?.isShowing == true) return
        var dialog: Dialog? = null
        val clearActiveDialog = {
            if (activePlaylistDialog === dialog) {
                activePlaylistDialog = null
            }
        }
        dialog = showDialog(clearActiveDialog)
        activePlaylistDialog = dialog
    }

    override fun onDestroyView() {
        activePlaylistDialog?.dismiss()
        activePlaylistDialog = null
        super.onDestroyView()
    }
}
