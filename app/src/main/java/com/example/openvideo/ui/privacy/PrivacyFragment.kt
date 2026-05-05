package com.example.openvideo.ui.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PrivacyFragment : Fragment() {

    private lateinit var privacyManager: PrivacyManager
    private lateinit var adapter: PrivacyFolderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_privacy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        privacyManager = PrivacyManager(requireContext())

        recyclerView = view.findViewById(R.id.recycler_privacy)
        emptyView = view.findViewById(R.id.tv_empty)

        adapter = PrivacyFolderAdapter(
            onRemove = { path -> confirmRemove(path) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.fab_add).setOnClickListener {
            showAddDialog()
        }

        loadFolders()
    }

    private fun loadFolders() {
        val folders = privacyManager.getHiddenFolders()
        adapter.submitList(folders)
        emptyView.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (folders.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.privacy_hint_path)
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.privacy_add_title)
            .setView(input)
            .setPositiveButton(R.string.action_add) { _, _ ->
                val path = input.text.toString().trim()
                if (path.isNotEmpty()) {
                    privacyManager.addHiddenFolder(path)
                    loadFolders()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmRemove(path: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.privacy_remove_title)
            .setMessage(getString(R.string.privacy_remove_message, path))
            .setPositiveButton(R.string.action_remove) { _, _ ->
                privacyManager.removeHiddenFolder(path)
                loadFolders()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
