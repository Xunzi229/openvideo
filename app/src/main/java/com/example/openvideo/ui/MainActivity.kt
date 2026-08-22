package com.example.openvideo.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
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
import com.example.openvideo.core.ui.ScreenBreakpoint
import com.example.openvideo.core.ui.SystemBarInsetsPolicy
import com.example.openvideo.core.ui.WindowSizeHelper
import com.example.openvideo.data.repository.VideoRepository
import com.example.openvideo.ui.home.HomeFragment
import com.example.openvideo.ui.local.LocalFolderFragment
import com.example.openvideo.ui.player.PlayerActivityIntents
import com.example.openvideo.ui.playlist.PlaylistFragment
import com.example.openvideo.ui.settings.SettingsFragment
import com.example.openvideo.ui.settings.SettingsViewModel
import com.example.openvideo.ui.sources.SourcesFragment
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        settingsViewModel.checkForAppUpdateSilently()

        breakpoint = WindowSizeHelper.computeBreakpoint(this)
        isTvMode = computeTvMode()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.menu.findItem(R.id.nav_sources).isVisible = BuildConfig.SOURCES_NAV_ENABLED
        bottomNav.isVisible = !isTvMode
        bindSystemBarInsets()
        if (savedInstanceState == null) {
            if (!isTvMode) bottomNav.selectedItemId = R.id.nav_home
            loadFragment(if (isTvMode) TvHomeFragment() else LocalFolderFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> LocalFolderFragment()
                R.id.nav_video -> HomeFragment()
                R.id.nav_sources -> SourcesFragment()
                R.id.nav_playlist -> PlaylistFragment()
                R.id.nav_mine -> SettingsFragment()
                else -> LocalFolderFragment()
            }
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            loadFragment(fragment)
            true
        }

        handleSharedPlaybackIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedPlaybackIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        pauseHiddenPlayerIfNeeded()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        breakpoint = WindowSizeHelper.computeBreakpoint(this)
        isTvMode = computeTvMode()
        findViewById<View>(R.id.main_root).requestApplyInsets()
    }

    private fun bindSystemBarInsets() {
        val root = findViewById<View>(R.id.main_root)
        val fragmentContainer = findViewById<View>(R.id.fragment_container)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val edges = SystemBarInsetsPolicy.union(
                SystemBarInsetsPolicy.Edges(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom),
                SystemBarInsetsPolicy.Edges(cutout.left, cutout.top, cutout.right, cutout.bottom)
            )
            root.updatePadding(left = edges.left, right = edges.right)
            if (bottomNav.isVisible) {
                fragmentContainer.updatePadding(top = edges.top, bottom = 0)
                bottomNav.updatePadding(bottom = edges.bottom)
            } else {
                fragmentContainer.updatePadding(top = edges.top, bottom = edges.bottom)
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
}
