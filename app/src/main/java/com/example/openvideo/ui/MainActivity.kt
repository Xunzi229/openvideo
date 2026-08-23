package com.example.openvideo.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.openvideo.BuildConfig
import com.example.openvideo.R
import com.example.openvideo.core.network.NetworkRecentUrlPolicy
import com.example.openvideo.core.network.NetworkSharedUrlPolicy
import com.example.openvideo.core.player.PlaybackServiceIntents
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.ui.AppleHaptics
import com.example.openvideo.core.ui.LibrarySwipeFrameLayout
import com.example.openvideo.core.ui.LibraryTabHostFragment
import com.example.openvideo.core.ui.ScreenBreakpoint
import com.example.openvideo.core.ui.SystemBarInsetsPolicy
import com.example.openvideo.core.ui.TabBarBlur
import com.example.openvideo.core.ui.WindowSizeHelper
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.ui.player.PlayerActivityIntents
import com.example.openvideo.ui.settings.SettingsViewModel
import com.example.openvideo.ui.tv.TvHomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var playerManager: PlayerManager
    @Inject lateinit var playerPrefs: PlayerPrefs
    @Inject lateinit var repository: VideoRepository

    var breakpoint: ScreenBreakpoint = ScreenBreakpoint.COMPACT
        private set

    var isTvMode: Boolean = false
        private set

    private val settingsViewModel: SettingsViewModel by viewModels()
    private var currentTabId: Int = R.id.nav_home

    private val tabBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            currentTabHost()?.pop()
            refreshTabBackCallback()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        settingsViewModel.checkForAppUpdateSilently()

        breakpoint = WindowSizeHelper.computeBreakpoint(this)
        isTvMode = computeTvMode()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        TabBarBlur.bind(bottomNav)
        bottomNav.menu.findItem(R.id.nav_sources).isVisible = BuildConfig.SOURCES_NAV_ENABLED
        bindPhoneChrome()
        bindSystemBarInsets()
        bindSideNav()
        onBackPressedDispatcher.addCallback(this, tabBackCallback)

        findViewById<LibrarySwipeFrameLayout>(R.id.phone_tab_host).onSwipeBack = {
            val popped = currentTabHost()?.pop() == true
            refreshTabBackCallback()
            popped
        }

        if (savedInstanceState == null) {
            if (isTvMode) {
                loadFragment(TvHomeFragment())
            } else {
                bottomNav.selectedItemId = R.id.nav_home
                showTab(R.id.nav_home)
            }
        } else {
            currentTabId = savedInstanceState.getInt(KEY_CURRENT_TAB, R.id.nav_home)
            if (!isTvMode) {
                showTab(currentTabId)
                bottomNav.selectedItemId = currentTabId
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (isTvMode) return@setOnItemSelectedListener false
            AppleHaptics.light(bottomNav)
            showTab(item.itemId)
            true
        }
        bottomNav.setOnItemReselectedListener {
            if (isTvMode) return@setOnItemReselectedListener
            val host = currentTabHost() ?: return@setOnItemReselectedListener
            while (host.canPop()) {
                host.pop()
            }
            refreshTabBackCallback()
        }

        handleSharedPlaybackIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_TAB, currentTabId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedPlaybackIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        pauseHiddenPlayerIfNeeded()
        refreshTabBackCallback()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        breakpoint = WindowSizeHelper.computeBreakpoint(this)
        isTvMode = computeTvMode()
        bindPhoneChrome()
        bindSideNav()
        findViewById<View>(R.id.main_root).requestApplyInsets()
    }

    private fun bindPhoneChrome() {
        val tablet = !isTvMode && breakpoint.isAtLeastMedium
        findViewById<View>(R.id.phone_tab_host).isVisible = !isTvMode
        findViewById<View>(R.id.fragment_container).isVisible = isTvMode
        findViewById<View>(R.id.bottom_nav).isVisible = !isTvMode && !tablet
        findViewById<View>(R.id.side_nav).isVisible = tablet
    }

    private fun bindSideNav() {
        val sideNav = findViewById<LinearLayout>(R.id.side_nav)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        sideNav.removeAllViews()
        val menu = bottomNav.menu
        for (index in 0 until menu.size()) {
            val item = menu.getItem(index)
            if (!item.isVisible) continue
            val row = LayoutInflater.from(this).inflate(R.layout.item_side_nav, sideNav, false)
            row.findViewById<ImageView>(R.id.iv_side_nav_icon).setImageDrawable(item.icon)
            row.findViewById<TextView>(R.id.tv_side_nav_label).text = item.title
            row.isSelected = item.itemId == currentTabId
            row.setOnClickListener {
                AppleHaptics.light(row)
                bottomNav.selectedItemId = item.itemId
            }
            sideNav.addView(row)
        }
    }

    private fun showTab(tabId: Int, createIfMissing: Boolean = true) {
        currentTabId = tabId
        TAB_PANES.forEach { (id, paneId) ->
            findViewById<View>(paneId).isVisible = id == tabId
        }
        if (createIfMissing) ensureTabHost(tabId)
        bindSideNav()
        refreshTabBackCallback()
    }

    private fun ensureTabHost(tabId: Int) {
        val tag = tabTag(tabId)
        if (supportFragmentManager.findFragmentByTag(tag) != null) return
        supportFragmentManager.beginTransaction()
            .replace(tabPaneId(tabId), LibraryTabHostFragment.newInstance(tabId), tag)
            .commitNow()
    }

    private fun currentTabHost(): LibraryTabHostFragment? =
        supportFragmentManager.findFragmentByTag(tabTag(currentTabId)) as? LibraryTabHostFragment

    fun onLibraryBackStackChanged() {
        refreshTabBackCallback()
    }

    private fun refreshTabBackCallback() {
        tabBackCallback.isEnabled = !isTvMode && currentTabHost()?.canPop() == true
    }

    private fun tabPaneId(tabId: Int): Int = TAB_PANES.getValue(tabId)

    private fun tabTag(tabId: Int): String = "library_tab_$tabId"

    private fun bindSystemBarInsets() {
        val root = findViewById<View>(R.id.main_root)
        val fragmentContainer = findViewById<View>(R.id.fragment_container)
        val phoneTabHost = findViewById<View>(R.id.phone_tab_host)
        val sideNav = findViewById<View>(R.id.side_nav)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val edges = SystemBarInsetsPolicy.union(
                SystemBarInsetsPolicy.Edges(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom),
                SystemBarInsetsPolicy.Edges(cutout.left, cutout.top, cutout.right, cutout.bottom)
            )
            root.updatePadding(left = edges.left, right = edges.right)
            val contentBottom = if (bottomNav.isVisible) 0 else edges.bottom
            fragmentContainer.updatePadding(top = edges.top, bottom = contentBottom)
            phoneTabHost.updatePadding(top = edges.top, bottom = contentBottom)
            sideNav.updatePadding(top = edges.top, bottom = edges.bottom)
            if (bottomNav.isVisible) {
                bottomNav.updatePadding(bottom = edges.bottom)
            } else {
                bottomNav.updatePadding(bottom = 0)
            }
            insets
        }
    }

    private fun computeTvMode(): Boolean =
        MainActivityTvModePolicy.isTvMode(
            uiMode = resources.configuration.uiMode,
            hasLeanbackFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        )

    private fun pauseHiddenPlayerIfNeeded() {
        val player = playerManager.player
        val decision = MainActivityPlaybackGuardPolicy.onResume(
            backgroundAudio = playerPrefs.bgAudio,
            playerExists = player != null,
            isPlayingOrRequested = player?.isPlaying == true || player?.playWhenReady == true
        )
        if (decision.pausePlayer) {
            player?.pause()
        }
        if (decision.stopPlaybackService) {
            runCatching { stopService(PlaybackServiceIntents.stop(this)) }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun handleSharedPlaybackIntent(intent: Intent) {
        val playbackUrl = NetworkSharedUrlPolicy.extractPlaybackUrl(
            action = intent.action,
            mimeType = intent.type,
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT),
            dataString = intent.dataString
        ) ?: return
        val title = NetworkRecentUrlPolicy.titleFor(playbackUrl)
        lifecycleScope.launch {
            repository.recordNetworkRecentUrl(playbackUrl, title)
        }
        val playerIntent = PlayerActivityIntents.networkPlayback(this, playbackUrl)
        startActivity(playerIntent)
    }

    companion object {
        private const val KEY_CURRENT_TAB = "current_tab"
        private val TAB_PANES = mapOf(
            R.id.nav_home to R.id.tab_pane_home,
            R.id.nav_video to R.id.tab_pane_video,
            R.id.nav_sources to R.id.tab_pane_sources,
            R.id.nav_playlist to R.id.tab_pane_playlist,
            R.id.nav_mine to R.id.tab_pane_mine
        )
    }
}
