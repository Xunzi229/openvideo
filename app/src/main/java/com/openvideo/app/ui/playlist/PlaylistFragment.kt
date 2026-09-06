package com.openvideo.app.ui.playlist

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.openvideo.app.R
import com.openvideo.app.core.ui.AppleAction
import com.openvideo.app.core.ui.AppleActionStyle
import com.openvideo.app.core.ui.AppleAlertDialog
import com.openvideo.app.core.ui.AppleEmptyState
import com.openvideo.app.core.ui.LibraryNavigator
import com.openvideo.app.core.ui.AppleOverlayChrome
import com.openvideo.app.core.ui.AppleOverlayColors
import com.openvideo.app.ui.settings.SettingsConfirmationActionSheet
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
                LibraryNavigator.push(
                    this,
                    PlaylistDetailFragment.newInstance(playlist.id, playlist.name)
                )
            },
            onMoreOptions = { playlist, anchor ->
                showPlaylistOptions(playlist)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.btn_add).setOnClickListener {
            showCreateDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlists.collect { list ->
                    adapter.submitList(list)
                    AppleEmptyState.setVisible(emptyView, list.isEmpty())
                    recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showCreateDialog() {
        val input = AppleOverlayChrome.inputField(
            context = requireContext(),
            colors = AppleOverlayColors.from(requireContext()),
            hint = getString(R.string.playlist_hint_name),
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        )
        AppleAlertDialog.show(
            context = requireContext(),
            title = getString(R.string.playlist_create_title),
            extraContent = input,
            includeIme = true,
            focusView = input,
            actions = listOf(
                AppleAction(getString(R.string.action_cancel), AppleActionStyle.CANCEL),
                AppleAction(getString(R.string.action_create)) {
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) viewModel.createPlaylist(name)
                }
            )
        )
        input.post {
            input.requestFocus()
        }
    }

    private fun showPlaylistOptions(playlist: com.openvideo.app.data.local.PlaylistEntity) {
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

    private fun showRenameDialog(playlist: com.openvideo.app.data.local.PlaylistEntity) {
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

    private fun confirmDelete(playlist: com.openvideo.app.data.local.PlaylistEntity) {
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
