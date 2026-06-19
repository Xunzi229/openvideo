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
import com.example.openvideo.ui.home.HomeCategory
import com.example.openvideo.ui.home.HomeFragment
import com.example.openvideo.ui.home.HomeViewModel
import com.example.openvideo.ui.home.MediaLibraryPermissionPolicy
import com.example.openvideo.ui.local.LocalFolderFragment
import com.example.openvideo.ui.series.SeriesListFragment
import com.example.openvideo.ui.series.SeriesListViewModel
import com.example.openvideo.ui.settings.SettingsFragment
import com.example.openvideo.ui.sources.SourcesFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class TvHomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val seriesViewModel: SeriesListViewModel by viewModels()
    private var lastFocusedCardId: Int = R.id.tv_card_continue
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        view?.let(::bindPermissionPanel)
        if (grants.any { it.value }) {
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

        bindPermissionPanel(view)
        bindCard(view, R.id.tv_card_continue) { HomeFragment.newInstance(HomeCategory.RECENT) }
        bindCard(view, R.id.tv_card_folders) { LocalFolderFragment() }
        bindCard(view, R.id.tv_card_series) { SeriesListFragment() }
        bindCard(view, R.id.tv_card_sources) { SourcesFragment() }
        bindCard(view, R.id.tv_card_settings) { SettingsFragment() }
        bindContinueCover(view)
        bindSeriesCover(view)

        requestInitialFocus(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { root ->
            bindPermissionPanel(root)
            requestResumeFocus(root)
        }
    }

    private fun bindCard(root: View, cardId: Int, fragmentFactory: () -> Fragment) {
        root.findViewById<View>(cardId).setOnClickListener {
            lastFocusedCardId = cardId
            navigateTo(fragmentFactory())
        }
    }

    private fun bindPermissionPanel(root: View) {
        val permissionPanel = root.findViewById<View>(R.id.tv_permission_panel)
        val continueCard = root.findViewById<View>(R.id.tv_card_continue)
        permissionPanel.apply {
            isVisible = !hasMediaReadAccess()
            continueCard.nextFocusUpId = if (permissionPanel.isVisible) permissionPanel.id else continueCard.id
            setOnClickListener {
                permissionLauncher.launch(MediaLibraryPermissionPolicy.requiredPermissions())
            }
        }
    }

    private fun requestInitialFocus(root: View) {
        val permissionPanel = root.findViewById<View>(R.id.tv_permission_panel)
        if (permissionPanel.isVisible) {
            permissionPanel.requestFocus()
        } else {
            root.findViewById<View>(R.id.tv_card_continue).requestFocus()
        }
    }

    private fun requestResumeFocus(root: View) {
        val permissionPanel = root.findViewById<View>(R.id.tv_permission_panel)
        if (permissionPanel.isVisible) {
            permissionPanel.requestFocus()
        } else {
            root.findViewById<View>(lastFocusedCardId).requestFocus()
        }
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
        val seriesCover = root.findViewById<ImageView>(R.id.tv_home_series_cover)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                seriesViewModel.series.collect { series ->
                    Glide.with(seriesCover)
                        .load(posterModel(series.firstOrNull { it.posterPath?.isNotBlank() == true }?.posterPath))
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
        val continueCover = root.findViewById<ImageView>(R.id.tv_home_continue_cover)
        val continueDetail = root.findViewById<TextView>(R.id.tv_home_continue_detail)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.recentVideos.collect { recent ->
                    val firstRecent = recent.firstOrNull()
                    continueDetail.text = if (firstRecent == null) {
                        getString(R.string.tv_home_continue_desc)
                    } else {
                        getString(R.string.tv_home_continue_recent_summary, firstRecent.title, recent.size)
                    }
                    Glide.with(continueCover)
                        .load(firstRecent?.thumbnailUri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_play)
                        .fallback(R.drawable.ic_play)
                        .error(R.drawable.ic_play)
                        .into(continueCover)
                }
            }
        }
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
