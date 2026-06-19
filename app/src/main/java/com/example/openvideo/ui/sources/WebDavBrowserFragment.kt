package com.example.openvideo.ui.sources

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.core.network.NetworkPlaybackHeaderPolicy
import com.example.openvideo.core.network.WebDavConnectionClient
import com.example.openvideo.core.network.WebDavConnectionPolicy
import com.example.openvideo.core.network.WebDavDirectoryParser
import com.example.openvideo.core.network.WebDavSubtitleMatcher
import com.example.openvideo.core.prefs.WebDavCredentialStore
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.ui.player.PlayerActivityIntents
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.Credentials
import javax.inject.Inject

@AndroidEntryPoint
class WebDavBrowserFragment : Fragment() {

    @Inject lateinit var repository: VideoRepository
    @Inject lateinit var webDavConnectionClient: WebDavConnectionClient

    private var sourceId: Long = 0L
    private lateinit var directoryUrl: String
    private lateinit var adapter: WebDavEntryAdapter
    private var credentials: WebDavCredentialStore.Credentials? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceId = arguments?.getLong(ARG_SOURCE_ID) ?: 0L
        directoryUrl = arguments?.getString(ARG_DIRECTORY_URL).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_webdav_browser, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backButton = view.findViewById<View>(R.id.btn_webdav_back)
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        backButton.post { backButton.requestFocus() }
        view.findViewById<TextView>(R.id.tv_webdav_path).text = directoryUrl
        adapter = WebDavEntryAdapter { entry -> openEntry(entry) }
        view.findViewById<RecyclerView>(R.id.recycler_webdav_entries).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WebDavBrowserFragment.adapter
        }
        loadDirectory()
    }

    private fun loadDirectory() {
        val progress = requireView().findViewById<ProgressBar>(R.id.progress_webdav)
        val empty = requireView().findViewById<TextView>(R.id.tv_webdav_empty)
        progress.visibility = View.VISIBLE
        empty.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val credentials = repository.getWebDavCredentials(sourceId)
            if (credentials == null) {
                progress.visibility = View.GONE
                empty.visibility = View.VISIBLE
                empty.setText(R.string.webdav_credentials_missing)
                updateContentFocusTarget(hasEntries = false)
                return@launch
            }
            this@WebDavBrowserFragment.credentials = credentials
            when (val result = webDavConnectionClient.listDirectory(
                directoryUrl = directoryUrl,
                username = credentials.username,
                password = credentials.password,
                userAgent = NetworkPlaybackHeaderPolicy.userAgent(requireContext())
            )) {
                is WebDavConnectionClient.DirectoryResult.Success -> {
                    adapter.submitList(result.entries)
                    progress.visibility = View.GONE
                    empty.visibility = if (result.entries.isEmpty()) View.VISIBLE else View.GONE
                    empty.setText(R.string.webdav_directory_empty)
                    updateContentFocusTarget(result.entries.isNotEmpty())
                }
                is WebDavConnectionClient.DirectoryResult.Failure -> {
                    progress.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                    empty.setText(webDavFailureMessage(result.error))
                    updateContentFocusTarget(hasEntries = false)
                }
            }
        }
    }

    private fun updateContentFocusTarget(hasEntries: Boolean) {
        val contentFocusTargetId = if (hasEntries) R.id.recycler_webdav_entries else R.id.tv_webdav_empty
        val backButton = requireView().findViewById<View>(R.id.btn_webdav_back)
        backButton.nextFocusDownId = contentFocusTargetId
    }

    private fun openEntry(entry: WebDavDirectoryParser.Entry) {
        when {
            entry.isDirectory -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, newInstance(sourceId, entry.url))
                    .addToBackStack("webdav:$sourceId:${entry.url}")
                    .commit()
            }
            entry.isPlayableVideo -> {
                val credentials = credentials ?: run {
                    Toast.makeText(requireContext(), R.string.webdav_credentials_missing, Toast.LENGTH_SHORT).show()
                    return
                }
                startActivity(
                    PlayerActivityIntents.networkPlayback(
                        requireContext(),
                        entry.url,
                        requestHeaders = mapOf(
                            "Authorization" to Credentials.basic(credentials.username, credentials.password)
                        ),
                        externalSubtitleUri = WebDavSubtitleMatcher.matchForVideo(
                            video = entry,
                            entries = adapter.currentList
                        )?.url
                    )
                )
            }
            else -> {
                Toast.makeText(requireContext(), R.string.webdav_entry_not_playable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun webDavFailureMessage(error: WebDavConnectionPolicy.Error): Int =
        when (error) {
            WebDavConnectionPolicy.Error.UNAUTHORIZED -> R.string.webdav_test_unauthorized
            WebDavConnectionPolicy.Error.FORBIDDEN -> R.string.webdav_test_forbidden
            WebDavConnectionPolicy.Error.NOT_FOUND -> R.string.webdav_test_not_found
            WebDavConnectionPolicy.Error.TIMEOUT -> R.string.webdav_test_timeout
            WebDavConnectionPolicy.Error.CERTIFICATE_ERROR -> R.string.webdav_test_certificate_error
            WebDavConnectionPolicy.Error.NETWORK_ERROR -> R.string.webdav_test_network_error
            else -> R.string.webdav_test_failed
        }

    companion object {
        private const val ARG_SOURCE_ID = "source_id"
        private const val ARG_DIRECTORY_URL = "directory_url"

        fun newInstance(sourceId: Long, directoryUrl: String): WebDavBrowserFragment =
            WebDavBrowserFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SOURCE_ID, sourceId)
                    putString(ARG_DIRECTORY_URL, directoryUrl)
                }
            }
    }
}

private class WebDavEntryAdapter(
    private val onClick: (WebDavDirectoryParser.Entry) -> Unit
) : ListAdapter<WebDavDirectoryParser.Entry, WebDavEntryAdapter.VH>(
    object : DiffUtil.ItemCallback<WebDavDirectoryParser.Entry>() {
        override fun areItemsTheSame(
            oldItem: WebDavDirectoryParser.Entry,
            newItem: WebDavDirectoryParser.Entry
        ): Boolean = oldItem.url == newItem.url

        override fun areContentsTheSame(
            oldItem: WebDavDirectoryParser.Entry,
            newItem: WebDavDirectoryParser.Entry
        ): Boolean = oldItem == newItem
    }
) {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.iv_webdav_entry_icon)
        private val name: TextView = view.findViewById(R.id.tv_webdav_entry_name)
        private val meta: TextView = view.findViewById(R.id.tv_webdav_entry_meta)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onClick(getItem(position))
            }
        }

        fun bind(entry: WebDavDirectoryParser.Entry) {
            icon.setImageResource(if (entry.isDirectory) R.drawable.ic_folder else R.drawable.ic_movie)
            name.text = entry.name
            meta.text = when {
                entry.isDirectory -> itemView.context.getString(R.string.webdav_entry_folder)
                entry.isPlayableVideo -> entry.sizeBytes?.let { formatSize(it) }
                    ?: itemView.context.getString(R.string.webdav_entry_video)
                else -> itemView.context.getString(R.string.webdav_entry_file)
            }
        }

        private fun formatSize(sizeBytes: Long): String {
            val mb = sizeBytes / (1024f * 1024f)
            return itemView.context.getString(R.string.size_format, mb)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_webdav_entry, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
