package com.example.openvideo.ui.privacy

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionStyle
import com.example.openvideo.core.ui.AppleAlertDialog
import com.example.openvideo.core.ui.AppleEmptyState
import com.example.openvideo.core.ui.AppleOverlayChrome
import com.example.openvideo.core.ui.AppleOverlayColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrivacyFragment : Fragment() {

    @Inject lateinit var privacyManager: PrivacyManager
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

        recyclerView = view.findViewById(R.id.recycler_privacy)
        emptyView = view.findViewById(R.id.tv_empty)

        adapter = PrivacyFolderAdapter(
            onRemove = { path -> confirmRemove(path) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.btn_add).setOnClickListener {
            showAddDialog()
        }

        loadFolders()
    }

    private fun loadFolders() {
        val folders = privacyManager.getHiddenFolders()
        adapter.submitList(folders)
        AppleEmptyState.setVisible(emptyView, folders.isEmpty())
        recyclerView.visibility = if (folders.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddDialog() {
        val input = AppleOverlayChrome.inputField(
            context = requireContext(),
            colors = AppleOverlayColors.from(requireContext()),
            hint = getString(R.string.privacy_hint_path),
            inputType = InputType.TYPE_CLASS_TEXT
        )
        AppleAlertDialog.show(
            context = requireContext(),
            title = getString(R.string.privacy_add_title),
            extraContent = input,
            includeIme = true,
            focusView = input,
            actions = listOf(
                AppleAction(getString(R.string.action_cancel), AppleActionStyle.CANCEL),
                AppleAction(getString(R.string.action_add)) {
                    val path = input.text.toString().trim()
                    if (path.isNotEmpty()) {
                        privacyManager.addHiddenFolder(path)
                        loadFolders()
                    }
                }
            )
        )
        input.post {
            input.requestFocus()
        }
    }

    private fun confirmRemove(path: String) {
        AppleAlertDialog.show(
            context = requireContext(),
            title = getString(R.string.privacy_remove_title),
            message = getString(R.string.privacy_remove_message, path),
            actions = listOf(
                AppleAction(getString(R.string.action_cancel), AppleActionStyle.CANCEL),
                AppleAction(
                    title = getString(R.string.action_remove),
                    style = AppleActionStyle.DESTRUCTIVE,
                    onClick = {
                        privacyManager.removeHiddenFolder(path)
                        loadFolders()
                    }
                )
            )
        )
    }
}
