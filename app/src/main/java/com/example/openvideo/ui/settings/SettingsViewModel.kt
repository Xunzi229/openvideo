package com.example.openvideo.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.ThemeMode
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val appPrefs: AppPrefs,
    private val playerPrefs: PlayerPrefs,
    private val repository: VideoRepository
) : AndroidViewModel(application) {

    val themeMode: ThemeMode get() = appPrefs.themeMode
    val language: String get() = appPrefs.language
    val defaultSpeed: Float get() = DefaultPlayerSettings.supportedSpeedOrDefault(playerPrefs.speed)
    val defaultRatio: AspectRatio get() = DefaultPlayerSettings.aspectRatioOrDefault(playerPrefs.aspectRatio)

    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize

    private val _historyCount = MutableStateFlow(0)
    val historyCount: StateFlow<Int> = _historyCount

    init {
        computeCacheSize()
        observeHistoryCount()
    }

    fun setThemeMode(mode: ThemeMode) {
        appPrefs.themeMode = mode
        AppSettingsApplier.apply(appPrefs)
    }

    fun setLanguage(lang: String) {
        appPrefs.language = lang
        AppSettingsApplier.apply(appPrefs)
    }

    fun setDefaultSpeed(speed: Float) {
        playerPrefs.speed = DefaultPlayerSettings.supportedSpeedOrDefault(speed)
    }

    fun setDefaultRatio(ratio: AspectRatio) {
        playerPrefs.aspectRatio = DefaultPlayerSettings.aspectRatioOrDefault(ratio)
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            ctx.cacheDir?.deleteRecursively()
            withContext(Dispatchers.Main) {
                Glide.get(ctx).clearMemory()
            }
            computeCacheSize()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun computeCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val size = ctx.cacheDir?.walkTopDown()?.sumOf { it.length() } ?: 0L
            _cacheSize.value = formatSize(size)
        }
    }

    private fun observeHistoryCount() {
        viewModelScope.launch {
            repository.getHistory().collect { list ->
                _historyCount.value = list.size
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }
}
