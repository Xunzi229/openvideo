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

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
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
            missing.visibility = View.VISIBLE
            return
        }
        missing.visibility = View.GONE
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
        requireView().findViewById<Button>(R.id.btn_browse_source).visibility =
            if (source.type.equals("webdav", ignoreCase = true)) View.VISIBLE else View.GONE
        requireView().findViewById<Button>(R.id.btn_delete_source).isEnabled = state.canDelete
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
