package com.example.openvideo.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bumptech.glide.Glide
import androidx.lifecycle.viewModelScope
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ThemeMode
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val appPrefs: AppPrefs,
    private val repository: VideoRepository
) : AndroidViewModel(application) {

    val themeMode: ThemeMode get() = appPrefs.themeMode
    val language: String get() = appPrefs.language
    val defaultSpeed: Float get() = appPrefs.defaultSpeed
    val defaultRatio: AspectRatio get() = appPrefs.defaultAspectRatio

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
    }

    fun setLanguage(lang: String) {
        appPrefs.language = lang
    }

    fun setDefaultSpeed(speed: Float) {
        appPrefs.defaultSpeed = speed
    }

    fun setDefaultRatio(ratio: AspectRatio) {
        appPrefs.defaultAspectRatio = ratio
    }

    fun clearCache() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            ctx.cacheDir?.let { deleteRecursively(it) }
            Glide.get(ctx).clearMemory()
            computeCacheSize()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun computeCacheSize() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val size = calculateDirSize(ctx.cacheDir)
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

    private fun calculateDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        var size = 0L
        dir.listFiles()?.forEach {
            size += if (it.isDirectory) calculateDirSize(it) else it.length()
        }
        return size
    }

    private fun deleteRecursively(dir: File) {
        dir.listFiles()?.forEach {
            if (it.isDirectory) deleteRecursively(it) else it.delete()
        }
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }
}
