package com.example.openvideo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.openvideo.R
import com.example.openvideo.core.ui.ScreenBreakpoint
import com.example.openvideo.core.ui.WindowSizeHelper
import com.example.openvideo.ui.favorite.FavoriteFragment
import com.example.openvideo.ui.history.HistoryFragment
import com.example.openvideo.ui.home.HomeFragment
import com.example.openvideo.ui.playlist.PlaylistFragment
import com.example.openvideo.ui.settings.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    var breakpoint: ScreenBreakpoint = ScreenBreakpoint.COMPACT
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        breakpoint = WindowSizeHelper.computeBreakpoint(this)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_video
        }

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_video -> HomeFragment()
                R.id.nav_playlist -> PlaylistFragment()
                R.id.nav_mine -> SettingsFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        breakpoint = WindowSizeHelper.computeBreakpoint(this)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
