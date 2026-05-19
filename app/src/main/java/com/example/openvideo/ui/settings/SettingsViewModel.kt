package com.example.openvideo.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.example.openvideo.R
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SettingsBackupExporter
import com.example.openvideo.core.prefs.SettingsBackupFileWriter
import com.example.openvideo.core.prefs.ThemeMode
import com.example.openvideo.core.update.GitHubReleaseChecker
import com.example.openvideo.core.update.UpdateApkInstaller
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

    private val _updateBadgeVisible = MutableStateFlow(appPrefs.githubUpdateBadgeVisible)
    val updateBadgeVisible: StateFlow<Boolean> = _updateBadgeVisible

    init {
        computeCacheSize()
        observeHistoryCount()
    }

    /**
     * Starts silently on app launch (from MainActivity). No toast/dialog.
     * Throttled to once per 24h unless [force].
     */
    fun checkForAppUpdateSilently(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!force && !GitHubReleaseChecker.shouldRunPeriodicCheck(appPrefs.lastGitHubReleaseCheckMs)) {
                return@launch
            }
            val release = fetchLatestReleaseOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                applyGitHubReleaseCheckResult(release)
            }
        }
    }

    /**
     * Checks GitHub latest release; if newer, selects APK for this device's ABI, verifies SHA-256 when a
     * checksum asset exists, then installs or falls back to browser.
     */
    fun onCheckUpdateClick(activityContext: Context) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val release = fetchLatestReleaseOrNull()
            if (release == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activityContext, R.string.settings_update_check_failed, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                applyGitHubReleaseCheckResult(release)
            }

            if (!GitHubReleaseChecker.isRemoteNewer(release.tagName, installedVersionName())) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activityContext, R.string.settings_already_latest, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val apk = GitHubReleaseChecker.selectApkForAbi(release.assets, Build.SUPPORTED_ABIS)
            if (apk == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activityContext, R.string.settings_update_no_apk_asset, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val ua = "OpenVideo/${installedVersionName()} (Android)"
            val expectedHex = GitHubReleaseChecker.resolveExpectedSha256Hex(release.assets, apk) { url ->
                GitHubReleaseChecker.fetchUrlText(url, ua)
            }

            if (expectedHex == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activityContext, R.string.settings_update_no_checksum_browser, Toast.LENGTH_LONG).show()
                    activityContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apk.browserDownloadUrl)))
                }
                return@launch
            }

            val dest = UpdateApkInstaller.cacheApkFile(app)
            if (!UpdateApkInstaller.downloadApk(apk.browserDownloadUrl, dest, ua)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activityContext, R.string.settings_update_download_failed, Toast.LENGTH_SHORT).show()
                    activityContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apk.browserDownloadUrl)))
                }
                return@launch
            }

            if (!UpdateApkInstaller.shaMatches(dest, expectedHex)) {
                dest.delete()
                withContext(Dispatchers.Main) {
                    Toast.makeText(activityContext, R.string.settings_update_sha_mismatch, Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !activityContext.packageManager.canRequestPackageInstalls()
                ) {
                    Toast.makeText(activityContext, R.string.settings_update_allow_install_or_browser, Toast.LENGTH_LONG)
                        .show()
                    activityContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apk.browserDownloadUrl)))
                    return@withContext
                }
                try {
                    activityContext.startActivity(UpdateApkInstaller.buildInstallIntent(activityContext, dest))
                } catch (_: Exception) {
                    Toast.makeText(activityContext, R.string.settings_update_install_failed_browser, Toast.LENGTH_SHORT)
                        .show()
                    activityContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apk.browserDownloadUrl)))
                }
            }
        }
    }

    private fun fetchLatestReleaseOrNull(): GitHubReleaseChecker.LatestRelease? {
        val ua = "OpenVideo/${installedVersionName()} (Android)"
        return GitHubReleaseChecker.fetchLatestRelease(ua)
    }

    private fun applyGitHubReleaseCheckResult(release: GitHubReleaseChecker.LatestRelease) {
        val newer = GitHubReleaseChecker.isRemoteNewer(release.tagName, installedVersionName())
        val url = if (newer) GitHubReleaseChecker.preferredDownloadUrl(release, Build.SUPPORTED_ABIS) else ""
        appPrefs.lastGitHubReleaseCheckMs = System.currentTimeMillis()
        appPrefs.githubUpdateBadgeVisible = newer
        appPrefs.githubPendingDownloadUrl = url
        _updateBadgeVisible.value = newer
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

    fun buildSettingsExportJson(): String =
        SettingsBackupExporter.exportJson(playerPrefs, appPrefs)

    suspend fun writeSettingsExportTo(context: Context, uri: Uri): SettingsBackupFileWriter.Result =
        withContext(Dispatchers.IO) {
            val json = buildSettingsExportJson()
            SettingsBackupFileWriter.writeJson(context.contentResolver, uri, json)
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

    fun installedVersionName(): String {
        val ctx = getApplication<Application>()
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            }
            info.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
}
