package com.example.openvideo.ui.tv

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.openvideo.R
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.ui.home.HomeCategory
import com.example.openvideo.ui.home.HomeFragment
import com.example.openvideo.ui.home.HomeViewModel
import com.example.openvideo.ui.home.MediaLibraryPermissionPolicy
import com.example.openvideo.ui.local.LocalFolderFragment
import com.example.openvideo.ui.player.PlayerAspectRatioOptions
import com.example.openvideo.ui.series.SeriesListFragment
import com.example.openvideo.ui.series.SeriesListViewModel
import com.example.openvideo.ui.settings.SettingsFragment
import com.example.openvideo.ui.settings.SettingsViewModel
import com.example.openvideo.ui.sources.SourcesFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val KEY_LAST_FOCUSED_CARD_ID = "tv_home_last_focused_card_id"

@AndroidEntryPoint
class TvHomeFragment : Fragment() {

    @Inject lateinit var repository: VideoRepository

    private val homeViewModel: HomeViewModel by viewModels()
    private val seriesViewModel: SeriesListViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var lastFocusedCardId: Int = R.id.tv_card_continue
    private var skipNextResumeFocusRequest: Boolean = false
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        view?.let(::bindPermissionPanel)
        if (grants.any { it.value }) {
            lastFocusedCardId = R.id.tv_card_folders
            navigateTo(LocalFolderFragment())
        } else {
            view?.let(::requestInitialFocus)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tv_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lastFocusedCardId = savedInstanceState?.getInt(KEY_LAST_FOCUSED_CARD_ID, R.id.tv_card_continue)
            ?: R.id.tv_card_continue

        bindPermissionPanel(view)
        bindCard(view, R.id.tv_card_continue) { HomeFragment.newInstance(HomeCategory.RECENT) }
        bindCard(view, R.id.tv_card_folders) { LocalFolderFragment() }
        bindCard(view, R.id.tv_card_series) { SeriesListFragment() }
        bindCard(view, R.id.tv_card_sources) { SourcesFragment() }
        bindCard(view, R.id.tv_card_settings) { SettingsFragment() }
        bindContinueCover(view)
        bindFoldersSummary(view)
        bindSourcesSummary(view)
        bindSettingsSummary(view)
        bindSeriesCover(view)

        if (savedInstanceState == null) {
            requestInitialFocus(view)
        } else {
            requestResumeFocus(view)
        }
        skipNextResumeFocusRequest = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_LAST_FOCUSED_CARD_ID, lastFocusedCardId)
    }

    override fun onResume() {
        super.onResume()
        view?.let { root ->
            bindPermissionPanel(root)
            if (skipNextResumeFocusRequest) {
                skipNextResumeFocusRequest = false
            } else {
                requestResumeFocus(root)
            }
        }
    }

    private fun bindCard(root: View, cardId: Int, fragmentFactory: () -> Fragment) {
        root.findViewById<View>(cardId).apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    lastFocusedCardId = cardId
                }
            }
            setOnClickListener {
                lastFocusedCardId = cardId
                navigateTo(fragmentFactory())
            }
        }
    }

    private fun bindPermissionPanel(root: View) {
        val permissionPanel = root.findViewById<View>(R.id.tv_permission_panel)
        val continueCard = root.findViewById<View>(R.id.tv_card_continue)
        permissionPanel.apply {
            isVisible = !hasMediaReadAccess()
            permissionPanel.contentDescription = getString(
                R.string.tv_permission_title
            ) + " " + getString(R.string.tv_permission_desc) + " " + getString(R.string.tv_permission_action)
            continueCard.nextFocusUpId = if (permissionPanel.isVisible) permissionPanel.id else continueCard.id
            setOnClickListener {
                permissionLauncher.launch(MediaLibraryPermissionPolicy.requiredPermissions())
            }
        }
    }

    private fun requestInitialFocus(root: View) {
        val permissionPanel = root.findViewById<View>(R.id.tv_permission_panel)
        if (permissionPanel.isVisible) {
            requestFocusAfterLayout(permissionPanel)
        } else {
            requestFocusAfterLayout(root.findViewById(R.id.tv_card_continue))
        }
    }

    private fun requestResumeFocus(root: View) {
        val permissionPanel = root.findViewById<View>(R.id.tv_permission_panel)
        if (permissionPanel.isVisible) {
            requestFocusAfterLayout(permissionPanel)
        } else {
            val resumeTarget = root.findViewById<View>(lastFocusedCardId)
                ?: root.findViewById(R.id.tv_card_continue)
            requestFocusAfterLayout(resumeTarget)
        }
    }

    private fun requestFocusAfterLayout(target: View) {
        target.post { target.requestFocus() }
    }

    private fun hasMediaReadAccess(): Boolean =
        MediaLibraryPermissionPolicy.hasReadAccess(
            isPermissionGranted = { permission ->
                ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
            }
        )

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("tv_home")
            .commit()
    }

    private fun bindSeriesCover(root: View) {
        val seriesCard = root.findViewById<View>(R.id.tv_card_series)
        val seriesCover = root.findViewById<ImageView>(R.id.tv_home_series_cover)
        val seriesDetail = root.findViewById<TextView>(R.id.tv_home_series_detail)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                seriesViewModel.series.collect { series ->
                    val firstPosterSeries = series.firstOrNull { it.posterPath?.isNotBlank() == true }
                    seriesCover.contentDescription = firstPosterSeries?.title
                        ?: getString(R.string.tv_home_series)
                    val seriesSummary = if (firstPosterSeries == null) {
                        getString(R.string.tv_home_series_desc)
                    } else {
                        getString(R.string.tv_home_series_summary, firstPosterSeries.title, series.size)
                    }
                    seriesDetail.text = seriesSummary
                    seriesCard.contentDescription = seriesSummary
                    Glide.with(seriesCover)
                        .load(posterModel(firstPosterSeries?.posterPath))
                        .centerCrop()
                        .placeholder(R.drawable.ic_movie)
                        .fallback(R.drawable.ic_movie)
                        .error(R.drawable.ic_movie)
                        .into(seriesCover)
                }
            }
        }
    }

    private fun bindContinueCover(root: View) {
        val continueCard = root.findViewById<View>(R.id.tv_card_continue)
        val continueCover = root.findViewById<ImageView>(R.id.tv_home_continue_cover)
        val continueDetail = root.findViewById<TextView>(R.id.tv_home_continue_detail)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    homeViewModel.recentVideos,
                    homeViewModel.recentContinueWatchingBadges
                ) { recent, badges -> recent to badges }.collect { (recent, badges) ->
                    val firstRecent = recent.firstOrNull()
                    val continueCoverModel = recent.firstNotNullOfOrNull { it.thumbnailUri }
                    val firstRecentBadge = firstRecent?.let { badges[it.id] }
                    continueCover.contentDescription = firstRecent?.title
                        ?: getString(R.string.tv_home_continue)
                    val continueSummary = when {
                        firstRecent == null -> getString(R.string.tv_home_continue_desc)
                        firstRecentBadge != null -> getString(
                            R.string.tv_home_continue_progress_summary,
                            firstRecent.title,
                            firstRecentBadge.progressLabel,
                            recent.size
                        )
                        else -> getString(R.string.tv_home_continue_recent_summary, firstRecent.title, recent.size)
                    }
                    continueDetail.text = continueSummary
                    continueCard.contentDescription = continueSummary
                    Glide.with(continueCover)
                        .load(continueCoverModel)
                        .centerCrop()
                        .placeholder(R.drawable.ic_play)
                        .fallback(R.drawable.ic_play)
                        .error(R.drawable.ic_play)
                        .into(continueCover)
                }
            }
        }
    }

    private fun bindFoldersSummary(root: View) {
        val foldersCard = root.findViewById<View>(R.id.tv_card_folders)
        val foldersIcon = root.findViewById<ImageView>(R.id.tv_home_folders_icon)
        val foldersDetail = root.findViewById<TextView>(R.id.tv_home_folders_detail)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.folders.collect { folders ->
                    val videoCount = folders.sumOf { it.videoCount }
                    val foldersSummary = if (folders.isEmpty()) {
                        getString(R.string.tv_home_folders_desc)
                    } else {
                        getString(R.string.tv_home_folders_summary, folders.size, videoCount)
                    }
                    foldersDetail.text = foldersSummary
                    foldersIcon.contentDescription = foldersSummary
                    foldersCard.contentDescription = foldersSummary
                }
            }
        }
    }

    private fun bindSourcesSummary(root: View) {
        val sourcesCard = root.findViewById<View>(R.id.tv_card_sources)
        val sourcesIcon = root.findViewById<ImageView>(R.id.tv_home_sources_icon)
        val sourcesDetail = root.findViewById<TextView>(R.id.tv_home_sources_detail)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getMediaSources().collect { sources ->
                    val sourcesSummary = if (sources.isEmpty()) {
                        getString(R.string.tv_home_sources_desc)
                    } else {
                        getString(R.string.tv_home_sources_summary, sources.size)
                    }
                    sourcesDetail.text = sourcesSummary
                    sourcesIcon.contentDescription = sourcesSummary
                    sourcesCard.contentDescription = sourcesSummary
                }
            }
        }
    }

    private fun bindSettingsSummary(root: View) {
        val settingsCard = root.findViewById<View>(R.id.tv_card_settings)
        val settingsIcon = root.findViewById<ImageView>(R.id.tv_home_settings_icon)
        val settingsDetail = root.findViewById<TextView>(R.id.tv_home_settings_detail)
        val ratioLabel = PlayerAspectRatioOptions.entries.firstOrNull { it.ratio == settingsViewModel.defaultRatio }
        val settingsSummary = getString(
            R.string.tv_home_settings_summary,
            getString(ratioLabel?.labelRes ?: R.string.settings_ratio_default),
            "${settingsViewModel.defaultSpeed}x"
        )
        settingsDetail.text = settingsSummary
        settingsIcon.contentDescription = settingsSummary
        settingsCard.contentDescription = settingsSummary
    }

    private fun posterModel(path: String?): Any? {
        val trimmed = path?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> null
            trimmed.startsWith("content://", ignoreCase = true) -> Uri.parse(trimmed)
            trimmed.startsWith("file://", ignoreCase = true) -> Uri.parse(trimmed)
            else -> File(trimmed)
        }
    }
}
