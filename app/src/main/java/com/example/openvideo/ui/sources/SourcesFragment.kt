package com.example.openvideo.ui.sources

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.core.network.NetworkPlaybackHeaderPolicy
import com.example.openvideo.core.network.WebDavConnectionClient
import com.example.openvideo.core.network.WebDavConnectionPolicy
import com.example.openvideo.core.ui.DeferredFeaturePolicy
import com.example.openvideo.data.local.MediaSourceEntity
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.core.ui.AppleHud
import com.example.openvideo.core.ui.LibraryNavigator
import com.example.openvideo.core.ui.GroupedListChrome
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.PlayerActivityIntents
import com.example.openvideo.ui.player.NetworkOpenUrlDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class SourcesFragment : Fragment() {

    @Inject lateinit var repository: VideoRepository
    @Inject lateinit var webDavConnectionClient: WebDavConnectionClient
    private var hasSavedSources = false
    private var hasRecentPlayback = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sources, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recentAdapter = SourceRecentPlaybackAdapter { item -> openRecentPlayback(item) }
        val recentRecycler = view.findViewById<RecyclerView>(R.id.recycler_source_recent)
        val recentEmpty = view.findViewById<TextView>(R.id.tv_source_recent_empty)
        val privacyNotice = SourceCredentialPrivacyPolicy.buildNotice(
            SourceCredentialPrivacyLabels.from(requireContext())
        )
        view.findViewById<TextView>(R.id.tv_source_privacy_notice).text =
            SourceCredentialPrivacyPolicy.formatNotice(privacyNotice)
        recentRecycler.layoutManager = LinearLayoutManager(requireContext())
        recentRecycler.adapter = recentAdapter
        recentRecycler.isNestedScrollingEnabled = false
        recentRecycler.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        val savedSourcesAdapter = SavedMediaSourceAdapter { source -> openSourceDetail(source) }
        val savedSourcesRecycler = view.findViewById<RecyclerView>(R.id.recycler_saved_sources)
        val savedSourcesEmpty = view.findViewById<TextView>(R.id.tv_saved_sources_empty)
        savedSourcesRecycler.layoutManager = LinearLayoutManager(requireContext())
        savedSourcesRecycler.adapter = savedSourcesAdapter
        savedSourcesRecycler.isNestedScrollingEnabled = false
        savedSourcesRecycler.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        val localSourceRow = view.findViewById<View>(R.id.row_source_local)
        localSourceRow.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.nav_home
        }
        localSourceRow.post { localSourceRow.requestFocus() }
        updateSourcesContentFocusTargets(view, hasSavedSources, hasRecentPlayback)
        view.findViewById<View>(R.id.row_source_open_url).setOnClickListener {
            NetworkOpenUrlDialog.show(requireContext()) { normalizedUrl, title ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.recordNetworkRecentUrl(normalizedUrl, title)
                }
            }
        }
        view.findViewById<View>(R.id.row_source_webdav).setOnClickListener {
            WebDavSourceDialog.show(requireContext()) { input ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = webDavConnectionClient.testConnection(
                        baseUrl = input.normalizedBaseUrl,
                        username = input.username,
                        password = input.password,
                        userAgent = NetworkPlaybackHeaderPolicy.userAgent(requireContext())
                    )
                    when (result) {
                        WebDavConnectionPolicy.ConnectionResult.Success -> {
                            repository.addWebDavSource(
                                baseUrl = input.normalizedBaseUrl,
                                name = input.name,
                                username = input.username,
                                password = input.password
                            )
                            AppleHud.show(requireContext(), R.string.webdav_test_success)
                        }
                        is WebDavConnectionPolicy.ConnectionResult.Failure -> {
                            AppleHud.show(requireContext(), webDavFailureMessage(result.error))
                        }
                    }
                }
            }
        }
        view.findViewById<View>(R.id.row_source_future).apply {
            visibility =
                if (DeferredFeaturePolicy.SOURCE_FUTURE_ADAPTERS_VISIBLE) View.VISIBLE else View.GONE
            setOnClickListener {
                AppleHud.show(requireContext(), R.string.sources_future_status_planned)
            }
        }
        view.findViewById<View>(R.id.sources_planned_section).visibility =
            if (DeferredFeaturePolicy.SOURCE_FUTURE_ADAPTERS_VISIBLE) View.VISIBLE else View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(repository.getHistory(), repository.getNetworkRecentUrls()) { history, networkRecent ->
                    SourceRecentPlaybackPolicy.buildItems(
                        history = history,
                        networkRecent = networkRecent,
                        labels = SourceRecentPlaybackLabels.from(requireContext()),
                        maxItems = MAX_RECENT_PLAYBACK_ITEMS,
                        localFileExists = { path -> File(path).exists() }
                    )
                }.collect { items ->
                    hasRecentPlayback = items.isNotEmpty()
                    recentAdapter.submitList(items)
                    recentEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    recentRecycler.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                    updateSourcesContentFocusTargets(view, hasSavedSources, hasRecentPlayback)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getMediaSources().collect { sources ->
                    hasSavedSources = sources.isNotEmpty()
                    savedSourcesAdapter.submitList(sources)
                    savedSourcesEmpty.visibility = if (sources.isEmpty()) View.VISIBLE else View.GONE
                    savedSourcesRecycler.visibility = if (sources.isEmpty()) View.GONE else View.VISIBLE
                    updateSourcesContentFocusTargets(view, hasSavedSources, hasRecentPlayback)
                }
            }
        }
    }

    private fun updateSourcesContentFocusTargets(
        view: View,
        hasSavedSources: Boolean,
        hasRecentPlayback: Boolean
    ) {
        val futureVisible = DeferredFeaturePolicy.SOURCE_FUTURE_ADAPTERS_VISIBLE
        val afterWebDavId = when {
            hasSavedSources -> R.id.recycler_saved_sources
            hasRecentPlayback -> R.id.recycler_source_recent
            futureVisible -> R.id.row_source_future
            else -> R.id.row_source_webdav
        }
        val afterSavedId = when {
            hasRecentPlayback -> R.id.recycler_source_recent
            futureVisible -> R.id.row_source_future
            else -> R.id.recycler_saved_sources
        }
        val recentUpId = if (hasSavedSources) R.id.recycler_saved_sources else R.id.row_source_webdav
        val afterRecentId =
            if (futureVisible) R.id.row_source_future else R.id.recycler_source_recent
        val futureUpId = when {
            hasRecentPlayback -> R.id.recycler_source_recent
            hasSavedSources -> R.id.recycler_saved_sources
            else -> R.id.row_source_webdav
        }

        view.findViewById<View>(R.id.row_source_local).nextFocusDownId = R.id.row_source_open_url
        view.findViewById<View>(R.id.row_source_open_url).nextFocusUpId = R.id.row_source_local
        view.findViewById<View>(R.id.row_source_open_url).nextFocusDownId = R.id.row_source_webdav
        view.findViewById<View>(R.id.recycler_saved_sources).nextFocusUpId = R.id.row_source_webdav
        view.findViewById<View>(R.id.recycler_saved_sources).nextFocusDownId = afterSavedId
        view.findViewById<View>(R.id.recycler_source_recent).nextFocusUpId = recentUpId
        view.findViewById<View>(R.id.recycler_source_recent).nextFocusDownId = afterRecentId
        view.findViewById<View>(R.id.row_source_webdav).nextFocusUpId = R.id.row_source_open_url
        view.findViewById<View>(R.id.row_source_webdav).nextFocusDownId = afterWebDavId
        view.findViewById<View>(R.id.row_source_future).nextFocusUpId = futureUpId
    }

    private fun openSourceDetail(source: MediaSourceEntity) {
        LibraryNavigator.push(
            this,
            SourceDetailFragment.newInstance(source.sourceId),
            "source:${source.sourceId}"
        )
    }

    private fun openRecentPlayback(item: SourceRecentPlaybackItem) {
        if (!item.isPlayable) {
            AppleHud.show(requireContext(), R.string.history_continue_missing_file)
            return
        }
        when (item.type) {
            SourceRecentPlaybackType.LOCAL -> {
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("video_uri", item.playbackUri)
                    putExtra("video_title", item.title)
                    putExtra("video_id", item.videoId)
                    putExtra("video_path", item.playbackUri)
                }
                startActivity(intent)
            }
            SourceRecentPlaybackType.NETWORK_URL -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.recordNetworkRecentUrl(item.playbackUri, item.title)
                }
                startActivity(PlayerActivityIntents.networkPlayback(requireContext(), item.playbackUri))
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

    private companion object {
        const val MAX_RECENT_PLAYBACK_ITEMS = 12
    }
}

private class SavedMediaSourceAdapter(
    private val onClick: (MediaSourceEntity) -> Unit
) : ListAdapter<MediaSourceEntity, SavedMediaSourceAdapter.VH>(
    object : DiffUtil.ItemCallback<MediaSourceEntity>() {
        override fun areItemsTheSame(a: MediaSourceEntity, b: MediaSourceEntity): Boolean =
            a.sourceId == b.sourceId

        override fun areContentsTheSame(a: MediaSourceEntity, b: MediaSourceEntity): Boolean = a == b
    }
) {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.iv_media_source_icon)
        private val name: TextView = view.findViewById(R.id.tv_media_source_name)
        private val address: TextView = view.findViewById(R.id.tv_media_source_address)
        private val type: TextView = view.findViewById(R.id.tv_media_source_type)
        private val hairline: View = view.findViewById(R.id.row_hairline)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onClick(getItem(position))
            }
        }

        fun bind(source: MediaSourceEntity, position: Int, itemCount: Int) {
            icon.setImageResource(
                when (source.type.lowercase()) {
                    "url" -> R.drawable.ic_stream
                    "webdav" -> R.drawable.ic_file_upload
                    else -> R.drawable.ic_info
                }
            )
            name.text = source.name
            address.text = source.displayUrl.ifBlank { source.url }
            type.text = source.type.uppercase()
            GroupedListChrome.bindContained(hairline, position, itemCount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_source, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position), position, itemCount)
}

private class SourceRecentPlaybackAdapter(
    private val onClick: (SourceRecentPlaybackItem) -> Unit
) : ListAdapter<SourceRecentPlaybackItem, SourceRecentPlaybackAdapter.VH>(
    object : DiffUtil.ItemCallback<SourceRecentPlaybackItem>() {
        override fun areItemsTheSame(a: SourceRecentPlaybackItem, b: SourceRecentPlaybackItem): Boolean =
            a.stableId == b.stableId

        override fun areContentsTheSame(a: SourceRecentPlaybackItem, b: SourceRecentPlaybackItem): Boolean = a == b
    }
) {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.iv_source_icon)
        private val sourceLabel: TextView = view.findViewById(R.id.tv_source_label)
        private val title: TextView = view.findViewById(R.id.tv_source_recent_title)
        private val detail: TextView = view.findViewById(R.id.tv_source_recent_detail)
        private val duration: TextView = view.findViewById(R.id.tv_source_recent_duration)
        private val hairline: View = view.findViewById(R.id.row_hairline)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onClick(getItem(position))
            }
        }

        fun bind(item: SourceRecentPlaybackItem, position: Int, itemCount: Int) {
            icon.setImageResource(
                when (item.type) {
                    SourceRecentPlaybackType.LOCAL -> R.drawable.ic_folder
                    SourceRecentPlaybackType.NETWORK_URL -> R.drawable.ic_stream
                }
            )
            sourceLabel.text = item.sourceLabel
            title.text = item.title
            detail.text = item.detailLabel
            duration.text = formatDuration(item.durationMs)
            itemView.alpha = if (item.isPlayable) 1f else 0.6f
            GroupedListChrome.bindContained(hairline, position, itemCount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_source_recent_playback, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position), position, itemCount)

    private fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "--:--"
        val totalSec = ms / 1000L
        val hours = totalSec / 3600L
        val minutes = (totalSec % 3600L) / 60L
        val seconds = totalSec % 60L
        return if (hours > 0L) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
