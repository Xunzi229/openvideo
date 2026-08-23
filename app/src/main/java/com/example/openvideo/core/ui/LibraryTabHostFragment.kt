package com.example.openvideo.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.openvideo.R
import com.example.openvideo.ui.home.HomeFragment
import com.example.openvideo.ui.local.LocalFolderFragment
import com.example.openvideo.ui.playlist.PlaylistFragment
import com.example.openvideo.ui.settings.SettingsFragment
import com.example.openvideo.ui.sources.SourcesFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LibraryTabHostFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_library_tab_host, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.addOnBackStackChangedListener {
            (activity as? com.example.openvideo.ui.MainActivity)?.onLibraryBackStackChanged()
        }
        if (savedInstanceState != null) return
        if (childFragmentManager.findFragmentById(R.id.tab_child_container) != null) return
        childFragmentManager.beginTransaction()
            .replace(R.id.tab_child_container, createRootFragment())
            .commit()
    }

    fun canPop(): Boolean = childFragmentManager.backStackEntryCount > 0

    fun pop(): Boolean {
        if (!canPop()) return false
        childFragmentManager.popBackStack()
        return true
    }

    private fun createRootFragment(): Fragment = when (requireArguments().getInt(ARG_TAB_ID)) {
        R.id.nav_home -> LocalFolderFragment()
        R.id.nav_video -> HomeFragment()
        R.id.nav_sources -> SourcesFragment()
        R.id.nav_playlist -> PlaylistFragment()
        else -> SettingsFragment()
    }

    companion object {
        private const val ARG_TAB_ID = "tab_id"

        fun newInstance(tabId: Int): LibraryTabHostFragment =
            LibraryTabHostFragment().apply {
                arguments = Bundle().apply { putInt(ARG_TAB_ID, tabId) }
            }
    }
}
