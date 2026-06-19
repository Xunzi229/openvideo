package com.example.openvideo.ui.sources

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.openvideo.R
import com.example.openvideo.core.network.NetworkUrlPolicy
import com.example.openvideo.data.local.MediaSourceEntity
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.ui.settings.SettingsConfirmationActionSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import javax.inject.Inject

@AndroidEntryPoint
class SourceDetailFragment : Fragment() {

    @Inject lateinit var repository: VideoRepository

    private var sourceId: Long = 0L
    private var currentSource: MediaSourceEntity? = null
    private var activeSourceDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceId = arguments?.getLong(ARG_SOURCE_ID) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_source_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<View>(R.id.btn_back)
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        backButton.post { backButton.requestFocus() }
        view.findViewById<Button>(R.id.btn_test_source).setOnClickListener {
            testCurrentSource()
        }
        view.findViewById<Button>(R.id.btn_browse_source).setOnClickListener {
            browseCurrentSource()
        }
        view.findViewById<Button>(R.id.btn_delete_source).setOnClickListener {
            confirmDeleteCurrentSource()
        }
        val privacyNotice = SourceCredentialPrivacyPolicy.buildNotice(
            SourceCredentialPrivacyLabels.from(requireContext())
        )
        view.findViewById<TextView>(R.id.tv_source_detail_privacy_notice).text =
            SourceCredentialPrivacyPolicy.formatNotice(privacyNotice)

        viewLifecycleOwner.lifecycleScope.launch {
            bindSource(repository.getMediaSource(sourceId))
        }
    }

    private fun bindSource(source: MediaSourceEntity?) {
        currentSource = source
        val missing = view?.findViewById<TextView>(R.id.tv_source_detail_missing) ?: return
        if (source == null) {
            showMissingSource(missing)
            return
        }
        missing.visibility = View.GONE
        setSourceDetailContentVisible(isVisible = true)
        val state = SourceDetailPresentationPolicy.buildUiState(
            source = source,
            labels = SourceDetailLabels.from(requireContext()),
            formatTimestamp = { timestamp -> DateFormat.getDateTimeInstance().format(timestamp) }
        )
        requireView().findViewById<TextView>(R.id.tv_source_detail_type).text = state.typeLabel
        requireView().findViewById<TextView>(R.id.tv_source_detail_name).text = state.name
        requireView().findViewById<TextView>(R.id.tv_source_detail_address).text = state.address
        requireView().findViewById<TextView>(R.id.tv_source_detail_last_used).text =
            getString(R.string.source_detail_last_used, state.lastUsedLabel)
        requireView().findViewById<Button>(R.id.btn_test_source).isEnabled = state.canTestConnection
        val showBrowse = source.type.equals("webdav", ignoreCase = true)
        requireView().findViewById<Button>(R.id.btn_browse_source).visibility =
            if (showBrowse) View.VISIBLE else View.GONE
        applyActionFocusOrder(showBrowse)
        requireView().findViewById<Button>(R.id.btn_delete_source).isEnabled = state.canDelete
    }

    private fun showMissingSource(missing: TextView) {
        missing.visibility = View.VISIBLE
        setSourceDetailContentVisible(isVisible = false)
        val backButton = requireView().findViewById<View>(R.id.btn_back)
        backButton.nextFocusDownId = R.id.tv_source_detail_missing
        missing.nextFocusUpId = R.id.btn_back
    }

    private fun setSourceDetailContentVisible(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        listOf(
            R.id.tv_source_detail_type,
            R.id.tv_source_detail_name,
            R.id.tv_source_detail_address,
            R.id.tv_source_detail_last_used,
            R.id.btn_test_source,
            R.id.btn_browse_source,
            R.id.btn_delete_source,
            R.id.tv_source_detail_privacy_notice
        ).forEach { id ->
            requireView().findViewById<View>(id).visibility = visibility
        }
    }

    private fun applyActionFocusOrder(showBrowse: Boolean) {
        val root = requireView()
        val backButton = root.findViewById<View>(R.id.btn_back)
        val testButton = root.findViewById<Button>(R.id.btn_test_source)
        val browseButton = root.findViewById<Button>(R.id.btn_browse_source)
        val deleteButton = root.findViewById<Button>(R.id.btn_delete_source)

        backButton.nextFocusDownId = R.id.btn_test_source
        testButton.nextFocusUpId = R.id.btn_back
        testButton.nextFocusDownId = if (showBrowse) R.id.btn_browse_source else R.id.btn_delete_source
        browseButton.nextFocusUpId = R.id.btn_test_source
        browseButton.nextFocusDownId = R.id.btn_delete_source
        deleteButton.nextFocusUpId = if (showBrowse) R.id.btn_browse_source else R.id.btn_test_source
    }

    private fun testCurrentSource() {
        val source = currentSource ?: return
        val result = NetworkUrlPolicy.validatePlaybackUrl(source.normalizedUrl)
        val message = if (result is NetworkUrlPolicy.Validation.Valid) {
            R.string.source_detail_test_ok
        } else {
            R.string.source_detail_test_invalid
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun browseCurrentSource() {
        val source = currentSource ?: return
        if (!source.type.equals("webdav", ignoreCase = true)) return
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                WebDavBrowserFragment.newInstance(source.sourceId, source.normalizedUrl)
            )
            .addToBackStack("webdav:${source.sourceId}")
            .commit()
    }

    private fun confirmDeleteCurrentSource() {
        val source = currentSource ?: return
        showExclusiveSourceDialog { onDismiss ->
            SettingsConfirmationActionSheet.show(
                context = requireContext(),
                titleRes = R.string.source_detail_delete_title,
                message = getString(R.string.source_detail_delete_message, source.name),
                confirmRes = R.string.action_delete,
                cancelRes = R.string.action_cancel,
                onDismiss = onDismiss,
                onConfirm = {
                    viewLifecycleOwner.lifecycleScope.launch {
                        repository.deleteMediaSource(sourceId)
                        parentFragmentManager.popBackStack()
                    }
                }
            )
        }
    }

    private fun showExclusiveSourceDialog(
        showDialog: (onDismiss: () -> Unit) -> Dialog
    ) {
        val current = activeSourceDialog
        if (current?.isShowing == true) return
        var dialog: Dialog? = null
        val clearActiveDialog = {
            if (activeSourceDialog === dialog) {
                activeSourceDialog = null
            }
        }
        dialog = showDialog(clearActiveDialog)
        activeSourceDialog = dialog
    }

    override fun onDestroyView() {
        activeSourceDialog?.dismiss()
        activeSourceDialog = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_SOURCE_ID = "source_id"

        fun newInstance(sourceId: Long): SourceDetailFragment {
            return SourceDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SOURCE_ID, sourceId)
                }
            }
        }
    }
}
